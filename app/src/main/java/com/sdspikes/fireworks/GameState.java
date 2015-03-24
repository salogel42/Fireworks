package com.sdspikes.fireworks;

import android.support.annotation.NonNull;

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

    private final int numPlayers;
    private Queue<Card> deck;
    private List<List<Card>> hands;
    private List<Card>[] played = new ArrayList[CardColor.values().length];
    private List<Card>[] discarded = new ArrayList[CardColor.values().length];

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
