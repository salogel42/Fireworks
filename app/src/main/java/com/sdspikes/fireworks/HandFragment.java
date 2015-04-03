package com.sdspikes.fireworks;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
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
                    // TODO(sdspikes): maybe use a per
                    mButtonWidth = (getView().getMeasuredWidth())/cardButtons.size();
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
        public void onFragmentSelected(String playerId, int index);
    }

    private void updateDisplay() {
        // In case updateDisplay gets called before widths are calculated (it'll be called again
        // when they are ready)
        if (mButtonWidth == 0) { return; }

        for (int i = 0; i < mHand.size() && i < cardButtons.size(); i++) {
            try {
                Log.d(TAG, "" + i + "  card: " + mHand.get(i).encodeCard().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            TextView textView = cardButtons.get(i);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textView.getLayoutParams();
            //Button new width
            int marginSize = mButtonWidth/10;
            params.width = mButtonWidth - marginSize * 2;
            params.height = params.width;
            params.setMargins(marginSize, 0, marginSize, 0);

            textView.setLayoutParams(params);
            if (!mHandHidden) {
                textView.setText(Integer.toString(mHand.get(i).rank));
                textView.setBackgroundResource(cardColorToBGColor.get(mHand.get(i).color));
                textView.setTextColor(getResources().getColor(cardColorToTextColor(mHand.get(i).color)));
            } else {
                textView.setText(" ");
                textView.setBackgroundResource(cardColorToBGColor.get(null));
            }

            textView.setVisibility(View.VISIBLE);
            textView.setOnClickListener(new View.OnClickListener() {
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
