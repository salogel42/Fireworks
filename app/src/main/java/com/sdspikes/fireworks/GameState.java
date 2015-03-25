package com.sdspikes.fireworks;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by sdspikes on 3/23/2015.
 */
public class GameState {

    enum CardColor { blue, red, green, yellow, white };

    public static class Card {
        private int rank;
        private CardColor color;

        public Card(int rank, CardColor color) {
            this.rank = rank;
            this.color = color;
        }
    }

    public final int numPlayers;
    public Queue<Card> deck;
    public List<List<Card>> hands;
    public List<Card>[] played = new ArrayList[CardColor.values().length];
    public List<Card>[] discarded = new ArrayList[CardColor.values().length];

    public GameState(int numPlayers) {
        this.numPlayers = numPlayers;
        deck = createNewDeck();
        for (int i = 0; i < CardColor.values().length; i++) {
            played[i] = new ArrayList<Card>();
            discarded[i] = new ArrayList<Card>();
        }
        int numCardsInHand = cardsPerHand(numPlayers);
        hands = new ArrayList<List<Card>>();
        for (int i = 0; i < numPlayers; i++) {
            hands.add(new ArrayList<Card>());
            for (int j = 0; j < numCardsInHand; j++) {
                hands.get(i).add(deck.remove());
            }
        }
    }

    public GameState(JSONObject jsonState) {
        int players = -1;
        try {
            players = jsonState.getInt("numPlayers");

            JSONArray deckArray = jsonState.getJSONArray("deck");
            deck = new LinkedList<Card>();
            for (int i = 0; i < deckArray.length(); i++) {
                deck.add(decodeCard(deckArray.getJSONObject(i)));
            }

            hands = decodeListOfLists(jsonState.getJSONArray("hands"));
            played =  decodeArrayOfLists(jsonState.getJSONArray("played"));
            discarded =  decodeArrayOfLists(jsonState.getJSONArray("discarded"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // set here so that there's exactly one place where numPlayers is initialized, since it's
        // final.
        numPlayers = players;
    }

    private static List<List<Card>> decodeListOfLists(JSONArray jsonArray) throws JSONException {
        List<List<Card>> list = new ArrayList<List<Card>>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray handArray = jsonArray.getJSONArray(i);
            List<Card> cards = new ArrayList<>();
            for (int j = 0; j < handArray.length(); j++) {
                cards.add(decodeCard(handArray.getJSONObject(j)));
            }
            list.add(cards);
        }
        return list;
    }

    private static List<Card>[] decodeArrayOfLists(JSONArray jsonArray) throws JSONException {
        List<Card>[] arr = new List[CardColor.values().length];
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray handArray = jsonArray.getJSONArray(i);
            List<Card> cards = new ArrayList<>();
            for (int j = 0; j < handArray.length(); j++) {
                cards.add(decodeCard(handArray.getJSONObject(j)));
            }
            arr[i] = cards;
        }
        return arr;
    }

    private static Card decodeCard(JSONObject jsonCard) throws JSONException {
        CardColor color = CardColor.valueOf(jsonCard.getString("color"));
        return new Card(jsonCard.getInt("rank"), color);
    }

    private static JSONObject encodeCard(Card card) throws JSONException {
        JSONObject cardObject = new JSONObject();
        cardObject.put("color", card.color.toString());
        cardObject.put("rank", card.rank);
        return cardObject;
    }

    private static JSONArray encodeArrayOfLists(List<Card>[] arr) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < arr.length; i++) {
            JSONArray handArray = new JSONArray();
            for (int j = 0; j < arr[i].size(); j++) {
                handArray.put(encodeCard(arr[i].get(j)));
            }
            jsonArray.put(handArray);
        }
        return jsonArray;
    }

    /* destructively modifies deck! */
    public JSONObject getJSONObect() {
        JSONObject result = new JSONObject();
        try {
            result.put("numPlayers", numPlayers);

            JSONArray deckArray = new JSONArray();
            while (!deck.isEmpty()) {
                deckArray.put(encodeCard(deck.poll()));
            }
            result.put("deck", deckArray);

            JSONArray handsArray = new JSONArray();
            for (int i = 0; i < hands.size(); i++) {
                JSONArray handArray = new JSONArray();
                for (int j = 0; j < hands.get(i).size(); j++) {
                    handArray.put(encodeCard(hands.get(i).get(j)));
                }
                handsArray.put(handArray);
            }
            result.put("hands", handsArray);

            result.put("played", encodeArrayOfLists(played));
            result.put("discarded", encodeArrayOfLists(discarded));
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

    private static Queue<Card> createNewDeck() {
        LinkedList<Card> deck = new LinkedList<Card>();
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
