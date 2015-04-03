package com.sdspikes.fireworks;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.games.multiplayer.Participant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by sdspikes on 3/23/2015.
 */
public class GameState {

    public static final String EXPLOSIONS = "splode";
    public static final String HINTS = "hints";
    public static final String NUM_PLAYERS = "nPlayers";
    public static final String DECK = "deck";
    public static final String HANDS = "hands";
    public static final String ID = "id";
    public static final String HAND_NODE = "node";
    public static final String PLAYED = "play";
    public static final String DISCARDED = "discard";
    public static final String CURRENT_ID = "currId";

    // Short names to reduce the length of messages
    enum CardColor { b, r, g, y, w }

    public static class Card implements Parcelable{
        // Short names for message length
        public static final String COLOR = "c";
        public static final String RANK = "r";

        public final int rank;
        public final CardColor color;

        public Card(int rank, CardColor color) {
            this.rank = rank;
            this.color = color;
        }

        public Card(JSONObject jsonCard) throws JSONException {
            color = CardColor.valueOf(jsonCard.getString(COLOR));
            rank = jsonCard.getInt(RANK);
        }

        public Card(Parcel p) {
            int[] arr = new int[2];
            p.readIntArray(arr);
            this.rank = arr[0];
            this.color = CardColor.values()[arr[1]];
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeIntArray(new int[]{this.rank, this.color.ordinal()});
        }

        public JSONObject encodeCard() throws JSONException {
            JSONObject cardObject = new JSONObject();
            cardObject.put(COLOR, color.toString());
            cardObject.put(RANK, rank);
            return cardObject;
        }

        @Override
        public String toString () {
            // e.g. blue 2
            return cardColorToString(color) + " " + rank;
        }

        public static String cardColorToString(GameState.CardColor color) {
            if (color == null) { return ""; }
            switch (color) {
                case b: return "blue";
                case g: return "green";
                case r: return "red";
                case w: return "white";
                case y: return "yellow";
                default: return "";
            }
        }

    }

    public static class HandNode {
        public static final String NEXT_ID = "nId";
        public static final String HAND = "hand";

        public final String nextPlayerId;
        public List<Card> hand;

        public HandNode(String nextId, List<Card> hand) {
            nextPlayerId = nextId;
            this.hand = hand;
        }

        public HandNode(String nextId) {
            this(nextId, new ArrayList<Card>());
        }

        public HandNode(JSONObject jsonNode) throws JSONException {
            this(jsonNode.getString(NEXT_ID));
            setHand(jsonNode.getJSONArray(HAND));
        }

        public void setHand(JSONArray jsonHand) throws JSONException {
            for (int i = 0; i < jsonHand.length(); i++) {
                hand.add(new Card(jsonHand.getJSONObject(i)));
            }
        }

        public JSONObject encodeNode() throws JSONException {
            JSONObject result = new JSONObject();
            result.put(NEXT_ID, nextPlayerId);
            result.put(HAND, encodeHand());
            return result;
        }

        public JSONArray encodeHand() throws JSONException {
            JSONArray jsonHand = new JSONArray();
            for (Card c : hand) {
                jsonHand.put(c.encodeCard());
            }
            return jsonHand;
        }
    }

    public String currentPlayerId;
    public final int numPlayers;
    public LinkedList<Card> deck;
    public int[] played = new int[CardColor.values().length];
    public List<Card>[] discarded = new ArrayList[CardColor.values().length];
    public HashMap<String, HandNode> hands = new HashMap<>();
    public int hintsRemaining = 8;
    public int explosionsRemaining = 3;

    public GameState(List<Participant> players) {
        numPlayers = players.size();
        deck = createNewDeck();
        for (int i = 0; i < CardColor.values().length; i++) {
            played[i] = 0;
            discarded[i] = new ArrayList<>();
        }
        int numCardsInHand = cardsPerHand(numPlayers);
        for (int i = 0; i < numPlayers; i++) {
            Participant player = players.get(i);
            List<Card> hand = new ArrayList<>();
            for (int j = 0; j < numCardsInHand; j++) {
                hand.add(deck.remove());
            }
            hands.put(player.getParticipantId(), new HandNode(
                    players.get((i + 1)  % numPlayers).getParticipantId(),
                    hand));
        }
        // TODO(sdspikes): apparently this is not always correct?
        currentPlayerId = players.get(0).getParticipantId();
    }

    public GameState(JSONObject jsonState) {
        int players = -1;
        try {
            players = jsonState.getInt(NUM_PLAYERS);

            JSONArray deckArray = jsonState.getJSONArray(DECK);
            deck = new LinkedList<>();
            for (int i = 0; i < deckArray.length(); i++) {
                deck.add(new Card(deckArray.getJSONObject(i)));
            }

            JSONArray mapArray = jsonState.getJSONArray(HANDS);
            hands = new HashMap<>();
            for (int i = 0; i < mapArray.length(); i++) {
                String id = mapArray.getJSONObject(i).getString(ID);
                hands.put(id, new HandNode(mapArray.getJSONObject(i).getJSONObject(HAND_NODE)));
            }

            played =  decodeIntArray(jsonState.getJSONArray(PLAYED));
            discarded =  decodeArrayOfLists(jsonState.getJSONArray(DISCARDED));
            currentPlayerId = jsonState.getString(CURRENT_ID);
            explosionsRemaining = jsonState.getInt(EXPLOSIONS);
            hintsRemaining = jsonState.getInt(HINTS);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // set here so that there's exactly one place where numPlayers is initialized, since it's
        // final.
        numPlayers = players;
    }

    private static List<Card>[] decodeArrayOfLists(JSONArray jsonArray) throws JSONException {
        List<Card>[] arr = new List[CardColor.values().length];
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray handArray = jsonArray.getJSONArray(i);
            List<Card> cards = new ArrayList<>();
            for (int j = 0; j < handArray.length(); j++) {
                cards.add(new Card(handArray.getJSONObject(j)));
            }
            arr[i] = cards;
        }
        return arr;
    }

    public static int[] decodeIntArray(JSONArray jsonArray) throws JSONException {
        int[] arr = new int[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            arr[i] = jsonArray.getInt(i);
        }
        return arr;
    }

    public static JSONArray encodeIntArray(int[] arr) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (int i : arr) {
            jsonArray.put(i);
        }
        return jsonArray;
    }

    private static JSONArray encodeArrayOfLists(List<Card>[] arr) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (List<Card> anArr : arr) {
            JSONArray handArray = new JSONArray();
            for (int j = 0; j < anArr.size(); j++) {
                handArray.put(anArr.get(j).encodeCard());
            }
            jsonArray.put(handArray);
        }
        return jsonArray;
    }

    public JSONObject getJSONObject() {
        JSONObject result = new JSONObject();
        try {
            result.put(NUM_PLAYERS, numPlayers);

            JSONArray deckArray = new JSONArray();
            for (Card c : deck) {
                deckArray.put(c.encodeCard());
            }
            result.put(DECK, deckArray);

            JSONArray handArray = new JSONArray();
            for (Map.Entry<String, HandNode> entry : hands.entrySet()) {
                JSONObject jsonEntry = new JSONObject();
                jsonEntry.put(ID, entry.getKey());
                jsonEntry.put(HAND_NODE, entry.getValue().encodeNode());
                handArray.put(jsonEntry);
            }
            result.put(HANDS, handArray);

            result.put(PLAYED, encodeIntArray(played));
            result.put(DISCARDED, encodeArrayOfLists(discarded));
            result.put(CURRENT_ID, currentPlayerId);
            result.put(EXPLOSIONS, explosionsRemaining);
            result.put(HINTS, hintsRemaining);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static int cardsPerHand(int numPlayers) {
        switch (numPlayers) {
            case 2:case 3: return 5;
            case 4:case 5: return 4;
            default: return -1;
        }
    }

    private static int cardsPerRank(int rank) {
        switch (rank) {
            case 1: return 3;
            case 2:case 3:case 4: return 2;
            case 5: return 1;
            default: return -1;
        }
    }

    private static LinkedList<Card> createNewDeck() {
        LinkedList<Card> deck = new LinkedList<>();
        for (CardColor c : CardColor.values()) {
            for (int i = 1; i <= 5; i++) {
                for (int j = 0; j < cardsPerRank(i); j++) {
                    deck.add(new Card(i, c));
                }
            }
        }
        Collections.shuffle(deck);
        return deck;
    }
}
