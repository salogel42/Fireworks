package com.sdspikes.fireworks;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    public static final String ARG_HAND_HIDDEN = "hand_hidden";

    // TODO: Rename and change types of parameters
    private List<GameState.Card> mHand;
    private String mPlayerId;
    private String mPlayerName;
    private int mButtonWidth = 0;
    private boolean mHandHidden;

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
    public static HandFragment newInstance(List<GameState.Card> hand, String playerId, String playerName, boolean handHidden) {
        HandFragment fragment = new HandFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(ARG_HAND, hand.toArray(new GameState.Card[hand.size()]));
        args.putString(ARG_PLAYER_ID, playerId);
        args.putString(ARG_PLAYER_NAME, playerName);
        args.putBoolean(ARG_HAND_HIDDEN, handHidden);
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
            mHandHidden = getArguments().getBoolean(ARG_HAND_HIDDEN);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mHand == null) {
            Log.d(TAG, "hand is null in onCreateView :(");
            return null;
        }

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_hand, container, false);
        cardButtons = new ArrayList<>();
        for (int i = 0; i < mHand.size(); i++) {
            Button b = new Button(getActivity());
            b.setId(indexToId(i));
            cardButtons.add(b);
            ((LinearLayout) v.findViewById(R.id.hand_container)).addView(b);
        }
        playerName = (TextView)v.findViewById(R.id.player_name);

        // TODO(sdspikes): Is there a better way to know for sure that the measuredWidth is going to be ready?
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "In the layout listener " + mPlayerName  );
                if (getView() != null && mButtonWidth == 0) {
                    mButtonWidth = (getView().getMeasuredWidth())/cardButtons.size();
                    if (mButtonWidth != 0) {
                        Log.d(TAG, "Things are measurable! Width is " + getView().getMeasuredWidth());
                        setUpView();
                        updateDisplay();
                        // now that we have the width, no need for this listener anymore
                        getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "player on-click triggered for " + mPlayerName);
                if (mListener != null) {
                    mListener.onFragmentSelected(mPlayerId, -1);
                }
            }
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
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

    private int indexToId(int id) {
        switch (id) {
            case 0: return R.id.card1;
            case 1: return R.id.card2;
            case 2: return R.id.card3;
            case 3: return R.id.card4;
            case 4: return R.id.card5;
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
        public void onFragmentSelected(String playerId, int index);
    }

    private void setUpView() {
        int marginSize = mButtonWidth/10;
        LinearLayout container = (LinearLayout) getView();
        container.setPadding(0, 0, 0, marginSize);
        TextView name = (TextView) getView().findViewById(R.id.player_name);
        name.setPadding(marginSize, 0, 0, 0);

        for (int i = 0; i < mHand.size() && i < cardButtons.size(); i++) {
            Button button = cardButtons.get(i);

            int buttonSize = mButtonWidth - marginSize * 2;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(buttonSize, buttonSize);
            params.setMargins(marginSize, 0, marginSize, 0);
            params.gravity = Gravity.CENTER;

            button.setLayoutParams(params);

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
    }
    private void updateDisplay() {
        // In case updateDisplay gets called before widths are calculated (it'll be called again
        // when they are ready)
        if (mButtonWidth == 0) { return; }

        try {
            Log.d(TAG, "cards: " + GameState.HandNode.encodeHand(mHand).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < mHand.size() && i < cardButtons.size(); i++) {
            Button button = cardButtons.get(i);

            if (!mHandHidden) {
                button.setText(Integer.toString(mHand.get(i).rank));
                button.setBackgroundResource(cardColorToBGColor.get(mHand.get(i).color));
                button.setTextColor(getResources().getColor(cardColorToTextColor(mHand.get(i).color)));
            } else {
                button.setText(" ");
                button.setBackgroundResource(cardColorToBGColor.get(null));
            }
        }

        playerName.setText(mPlayerName);
    }

    public static final Map<GameState.CardColor, Integer> cardColorToBGColor = new HashMap<>();
    static {
        cardColorToBGColor.put(GameState.CardColor.b, R.color.Blue);
        cardColorToBGColor.put(GameState.CardColor.g, R.color.Green);
        cardColorToBGColor.put(GameState.CardColor.r, R.color.Red);
        cardColorToBGColor.put(GameState.CardColor.w, R.color.White);
        cardColorToBGColor.put(GameState.CardColor.y, R.color.Yellow);
        cardColorToBGColor.put(null, R.color.TextDarkBG);
    }

    public static int cardColorToTextColor(GameState.CardColor color) {
        if (color == null) return R.color.TextLightBG;
        switch (color) {
            case g: return R.color.TextDarkerBG;
            case b: case r: return R.color.TextDarkBG;
            case w: case y: default: return R.color.TextLightBG;
        }
    }

    public static String rankToString(int rank) {
        switch (rank) {
            case 1: return "one";
            case 2: return "two";
            case 3: return "three";
            case 4: return "four";
            case 5: return "five";
            default: return "";
        }
    }
}
