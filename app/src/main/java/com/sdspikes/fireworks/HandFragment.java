package com.sdspikes.fireworks;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HandFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HandFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HandFragment extends Fragment {

    public static final String TAG = "HandFragment";

    private static final String ARG_HAND = "hand";
    private static final String ARG_PLAYER_ID = "player_id";
    private static final String ARG_PLAYER_NAME = "player_name";

    // TODO: Rename and change types of parameters
    private List<GameState.Card> mHand;
    private String mPlayerId;
    private String mPlayerName;
    private int mButtonWidth = 0;

    private ArrayList<Button> cardButtons;
    private TextView playerName;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param hand the hand to display in this fragment.
     * @return A new instance of fragment BlankFragment.
     */
    public static HandFragment newInstance(List<GameState.Card> hand, String playerId, String playerName) {
        HandFragment fragment = new HandFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(ARG_HAND, hand.toArray(new GameState.Card[hand.size()]));
        args.putString(ARG_PLAYER_ID, playerId);
        args.putString(ARG_PLAYER_NAME, playerName);
        fragment.setArguments(args);
        return fragment;
    }

    public HandFragment() {
        // Required empty public constructor
    }

    public void updateHand(List<GameState.Card> newHand) {
        mHand = newHand;
        updateDisplay();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mHand = Arrays.asList((GameState.Card[]) getArguments().getParcelableArray(ARG_HAND));
            mPlayerId = getArguments().getString(ARG_PLAYER_ID);
            mPlayerName = getArguments().getString(ARG_PLAYER_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mHand == null) {
            Log.d(TAG, "hand is null in onCreateView :(");
            return null;
        }

        View v;
        // Inflate the layout for this fragment
        if (mHand.size() > 4) {
            v = inflater.inflate(R.layout.fragment_large_hand, container, false);
        } else {
            v = inflater.inflate(R.layout.fragment_small_hand, container, false);
        }
        cardButtons = new ArrayList<>();
        cardButtons.add((Button)v.findViewById(R.id.card1));
        cardButtons.add((Button)v.findViewById(R.id.card2));
        cardButtons.add((Button)v.findViewById(R.id.card3));
        cardButtons.add((Button)v.findViewById(R.id.card4));
        if (mHand.size() > 4) {
            cardButtons.add((Button)v.findViewById(R.id.card5));
        }
        playerName = (TextView)v.findViewById(R.id.player_name);

        // TODO(sdspikes): Is there a better way to know for sure that the measuredWidth is going to be ready?
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "In the layout listener " + mPlayerName  );
                Log.d(TAG, "View: " + getView());
                Log.d(TAG, "mButtonWidth: " + mButtonWidth);
                if (getView() != null && mButtonWidth == 0) {
                    Log.d(TAG, "width: " + getView().getMeasuredWidth());
                    mButtonWidth = getView().getMeasuredWidth();
                    if (mButtonWidth != 0) {
                        updateDisplay();
                    }
                }
            }
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(View v) {
        if (mListener != null) {
            mListener.onFragmentSelected(mPlayerId, idToIndex(v.getId()));
        }
    }

    private int idToIndex(int id) {
        switch (id) {
            case R.id.card1: return 0;
            case R.id.card2: return 1;
            case R.id.card3: return 2;
            case R.id.card4: return 3;
            case R.id.card5: return 4;
            default: return -1;
        }

    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        // Not sure if needed
        playerName = null;
        cardButtons = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentSelected(String playerId, int index);
    }

    private void updateDisplay() {
        mButtonWidth = getView().getMeasuredWidth()/cardButtons.size();
        Log.d(TAG, "" + getView().getMeasuredWidth());
        Log.d(TAG, "" + mButtonWidth);
        for (int i = 0; i < mHand.size() && i < cardButtons.size(); i++) {
            try {
                Log.d(TAG, "" + i + "  card: " + mHand.get(i).encodeCard().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Button button = cardButtons.get(i);

            ViewGroup.LayoutParams params = button.getLayoutParams();
            //Button new width
            params.width = mButtonWidth;

            button.setLayoutParams(params);
            button.setText(Integer.toString(mHand.get(i).rank));
            button.setBackgroundResource(cardColorToBGColor(mHand.get(i).color));
            button.setTextColor(getResources().getColor(cardColorToTextColor(mHand.get(i).color)));
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onFragmentSelected(mPlayerId, idToIndex(v.getId()));
                    }
                }
            });
        }

        playerName.setText(mPlayerName);
        // TODO(sdspikes): update the display based on the hand etc
    }

    private int cardColorToBGColor(GameState.CardColor color) {
        switch (color) {
            case b: return R.color.Blue;
            case g: return R.color.Green;
            case r: return R.color.Red;
            case w: return R.color.White;
            case y: return R.color.Yellow;
            default: return R.color.BlurbColor;
        }
    }

    private int cardColorToTextColor(GameState.CardColor color) {
        switch (color) {
            case b: case g: case r: return R.color.TextDarkBG;
            case w: case y: default: return R.color.TextLightBG;
        }
    }
}
