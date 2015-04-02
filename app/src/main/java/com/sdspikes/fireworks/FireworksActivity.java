/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sdspikes.fireworks;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.plus.Plus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Button Clicker 2000. A minimalistic game showing the multiplayer features of
 * the Google Play game services API. The objective of this game is clicking a
 * button. Whoever clicks the button the most times within a 20 second interval
 * wins. It's that simple. This game can be played with 2, 3 or 4 players. The
 * code is organized in sections in order to make understanding as clear as
 * possible. We start with the integration section where we show how the game
 * is integrated with the Google Play game services API, then move on to
 * game-specific UI and logic.
 * <p/>
 * INSTRUCTIONS: To run this sample, please set up
 * a project in the Developer Console. Then, place your app ID on
 * res/values/ids.xml. Also, change the package name to the package name you
 * used to create the client ID in Developer Console. Make sure you sign the
 * APK with the certificate whose fingerprint you entered in Developer Console
 * when creating your Client Id.
 *
 * @author Bruno Oliveira (btco), 2013-04-26
 */
public class FireworksActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener, RoomStatusUpdateListener,
        RoomUpdateListener, OnInvitationReceivedListener,
        HandFragment.OnFragmentInteractionListener {

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

    public static final String TAG = "FireworksActivity";

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    public static final int MIN_NUM_PLAYERS = 2;
    public static final int MAX_NUM_PLAYERS = 5;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;
    public static final int MAX_CARD_WIDTH = 40;
    public static final int MIN_CARD_WIDTH = 20;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    private String mRoomId = null;

    private Room mRoom = null;

    // Are we playing in multiplayer mode?
    private boolean mMultiplayer = false;

    // Is the game done?
    private boolean mGameComplete = false;

    // The participants in the currently active game
    private List<Participant> mParticipants = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    private String mIncomingInvitationId = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fireworks);

        // Create the Google Api Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // set up a click listener for everything we care about
        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.button_single_player:
            case R.id.button_single_player_2:
                // play a single-player game
                resetGameVars();
                startGame(false);
                break;
            case R.id.button_sign_in:
                // user wants to sign in
                // Check to see the developer who's running this sample code read the instructions :-)
                // NOTE: this check is here only because this is a sample! Don't include this
                // check in your actual production app.
                if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id)) {
                    Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
                }

                // start the sign-in flow
                Log.d(TAG, "Sign-in button clicked");
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
            case R.id.button_sign_out:
                // user wants to sign out
                // sign out.
                Log.d(TAG, "Sign-out button clicked");
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
                // show list of invitable players
                intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 3);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_SELECT_PLAYERS);
                break;
            case R.id.button_see_invitations:
                // show list of pending invitations
                intent = Games.Invitations.getInvitationInboxIntent(mGoogleApiClient);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_INVITATION_INBOX);
                break;
            case R.id.button_accept_popup_invitation:
                // user wants to accept the invitation shown on the invitation popup
                // (the one we got through the OnInvitationReceivedListener).
                acceptInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            case R.id.button_quick_game:
                // user wants to play against a random opponent right now
                startQuickGame();
                break;
        }
    }

    void startQuickGame() {
        // quick-start a game with 1 randomly selected opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 4;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                MAX_OPPONENTS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                handleSelectPlayersResult(responseCode, intent);
                break;
            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the selected invitation:
                handleInvitationInboxResult(responseCode, intent);
                break;
            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d(TAG, "Starting game (waiting room returned OK).");
                    startGame(true);
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                }
                break;
            case RC_SIGN_IN:
                Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
                        + responseCode + ", intent=" + intent);
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (responseCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    BaseGameUtils.showActivityResultError(this, requestCode, responseCode, R.string.signin_other_error);
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }

    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Log.d(TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
        Log.d(TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
        Log.d(TAG, "Room created, waiting for it to be ready...");
    }

    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }

    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
        leaveRoom();

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            switchToScreen(R.id.screen_sign_in);
        } else {
            switchToScreen(R.id.screen_wait);
        }
        super.onStop();
    }

    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        switchToScreen(R.id.screen_wait);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.w(TAG,
                    "GameHelper: client was already connected on onStart()");
        } else {
            Log.d(TAG, "Connecting client.");
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
            leaveRoom();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        stopKeepingScreenOn();
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
            switchToScreen(R.id.screen_wait);
        } else {
            switchToMainScreen();
        }
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        // We got an invitation to play a game! So, store it in
        // mIncomingInvitationId
        // and show the popup on the screen.
        mIncomingInvitationId = invitation.getInvitationId();
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(
                invitation.getInviter().getDisplayName() + " " +
                        getString(R.string.is_inviting_you));
        switchToScreen(mCurScreen); // This will show the invitation popup
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        if (mIncomingInvitationId.equals(invitationId)) {
            mIncomingInvitationId = null;
            switchToScreen(mCurScreen); // This will hide the invitation popup
        }
    }

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected() called. Sign in successful!");

        Log.d(TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        if (connectionHint != null) {
            Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
            Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(TAG, "onConnected: connection hint has a room invite!");
                acceptInviteToRoom(inv.getInvitationId());
                return;
            }
        }
        switchToMainScreen();

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
                    connectionResult, RC_SIGN_IN, getString(R.string.signin_other_error));
        }

        switchToScreen(R.id.screen_sign_in);
    }

    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        // get room ID, participants and my ID:
        mRoomId = room.getRoomId();
        mRoom = room;
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + mRoomId);
        Log.d(TAG, "My ID " + mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");
    }

    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }

    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        showGameError();
    }

    // Show error message about game being cancelled and return to main screen.
    void showGameError() {
        BaseGameUtils.makeSimpleDialog(this, getString(R.string.game_problem));
        switchToMainScreen();
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {
    }

    @Override
    public void onP2PConnected(String participant) {
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    void updateRoom(Room room) {
        if (room != null && mRoom != room) {
            mRoom = room;
            mRoomId = room.getRoomId();
            Log.d(TAG, "updating room: " + mRoomId);
            mParticipants = room.getParticipants();
            if (mParticipants != null) {
                resetGameVars();
                setUpMap();
            }
        } else {
            Log.d(TAG, "tried to update room");
        }
    }

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */

    // Current state of the game:

    // My participant ID in the currently active game
    private String mMyId = null;

    private SortedMap<String, String> mIdToName;

    private FireworksTurn mTurnData = null;

    private boolean mHandSelectionMode = false;

    private boolean mDiscardMode = false;

    private boolean mPlayMode = false;

    private Map<String, HandFragment> fragments = new HashMap<>();

    // Reset game variables in preparation for a new game.
    void resetGameVars() {
        mMyId = null;
        mTurnData = null;
        mIdToName = null;
        mHandSelectionMode = false;
        mDiscardMode = false;
        mPlayMode = false;
        fragments = new HashMap<>();
        togglePlayOptionsVisible(false);
    }

    // Assumes mRoom is set up
    private void setUpMap() {
        if (mIdToName == null) {
            mParticipants = mRoom.getParticipants();
            mMyId = mRoom.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));
            mIdToName = new TreeMap<>();
            for (Participant p : mParticipants) {
                mIdToName.put(p.getParticipantId(), p.getDisplayName());
            }
        }
    }

    public void startGame(boolean multiplayer) {
        mMultiplayer = multiplayer;

        if (!multiplayer) {
            // TODO(sdspikes): create a game with some AIs, give them names?
//            mTurnData.state = new GameState(3);
        } else {
            setUpMap();
            if (mMyId.equals(mIdToName.keySet().iterator().next())) {
                try {
                    setUpGame();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                Log.d(TAG, mParticipants.get(0).getParticipantId() + " is the first id and will choose who is first player");
//                //TODO(sdspikes): actually send the message
//                int rand = new Random().nextInt(mParticipants.size());
//                String firstPlayerId = mParticipants.get(rand).getParticipantId();
//                JSONObject firstPlayer = new JSONObject();
//                try {
//                    if (firstPlayerId.equals(mMyId)) {
//                        // If it's me, don't bother sending messages, just set up the game and send
//                        // the message about the first state
//                        setUpGame();
//                    } else {
//                        // otherwise, set up a message letting everyone know who the first player is
//                        // so that player can set up the game
//                        firstPlayer.put("firstPlayer", firstPlayerId);
//                        broadcastGameInfo(firstPlayer);
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
                togglePlayOptionsVisible(true);
            } else {
                Log.d(TAG, mMyId + " is not the first id and should get a message w/ game state");
            }
        }
    }

    private void displayInitialState() {
        switchToScreen(R.id.screen_game);
        GameState.HandNode currentNode = mTurnData.state.hands.get(mMyId);
        FragmentManager fm = getFragmentManager();

        if (fm.findFragmentById(R.id.my_hand) == null) {
            if (currentNode != null) {
                addHandFragment(fm, currentNode, mMyId, mIdToName.get(mMyId), R.id.my_hand);
            } else {
                Log.d(TAG, mMyId);
            }
        }

        if (fm.findFragmentById(R.id.other_hands) == null) {
            while (true) {
                String currentId = currentNode.nextPlayerId;
                if (currentId.equals(mMyId)) { break; }
                currentNode = mTurnData.state.hands.get(currentId);
                addHandFragment(fm, currentNode, currentId, mIdToName.get(currentId), R.id.other_hands);
            }
        }

        LinearLayout played = (LinearLayout)findViewById(R.id.played_pile);

        played.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                LinearLayout played = (LinearLayout)findViewById(R.id.played_pile);
                mDiscardWidthR2 = played.getMeasuredWidth();

                if (mDiscardWidthR2 != 0) {
                    int usableWidth = mDiscardWidthR2 - findViewById(R.id.played_label).getMeasuredWidth();
                    mDiscardWidthR1 = mDiscardWidthR2 - findViewById(R.id.discarded_label).getMeasuredWidth();
                    for (int i = 1; i < played.getChildCount(); i++) {
                        ViewGroup.LayoutParams params = played.getChildAt(i).getLayoutParams();
                        params.width = usableWidth/5;
                        played.getChildAt(i).setLayoutParams(params);
                    }
                    played.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // In case all the data is ready already and was just waiting on this.
                    updateDisplay();
                }
            }
        });
    }

    private void addHandFragment(
            FragmentManager fm, GameState.HandNode node, String playerId, String playerName, int id) {
        try {
            Log.d(TAG, node.encodeNode().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        HandFragment handFragment = HandFragment.newInstance(
                node.hand,
                playerId,
                playerName);
        fragments.put(playerId, handFragment);
        // TODO(sdspikes): previously I just had .commit here and got
        // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        fm.beginTransaction().add(id, handFragment).commitAllowingStateLoss();
    }

    private void togglePlayOptionsVisible(boolean visible) {
        findViewById(R.id.my_turn_buttons).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */

    // Called when we receive a real-time message from the network.
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();

        try {
            JSONObject obj = unpersist(buf);
            // First, try seeing if it's a gamestate since that's the most common message
            mTurnData = FireworksTurn.unpersist(obj);
            if (mTurnData != null) {
                Log.d(TAG, "got turn data!");
                if (obj.has("firstTime")) {
                    // Set up the id map if necessary
                    setUpMap();
                    displayInitialState();
                } else {
                    updateDisplay();
                }
                togglePlayOptionsVisible(mMyId.equals(mTurnData.state.currentPlayerId));
            } else if (obj.has("handUpdate")) {
                // TODO(sdspikes): is it possible that the hand could be updated (different cards)
                // before this?
                mTurnData.state.hands.get(sender).setHand(obj.getJSONArray("handUpdate"));
            } else if (obj.has("firstPlayer")) {
                String firstPlayerId = obj.getString("firstPlayer");
                Log.d(TAG, "first player id: " + firstPlayerId + ", mine: " + mMyId);
                if (firstPlayerId == mMyId) {
                    setUpGame();
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setUpGame() throws JSONException {
        // set up initial turn data
        mTurnData = new FireworksTurn();
        mTurnData.state = new GameState(mParticipants);
        // TODO(sdspikes): maybe pass this to GameState constructor?
        mTurnData.state.currentPlayerId = mMyId;
        JSONObject jsonTurn =  mTurnData.getJSONObject();
        jsonTurn.put("firstTime", mMyId);
        broadcastGameInfo(jsonTurn);
        displayInitialState();
    }

    private JSONObject unpersist(byte[] buf) throws JSONException {
        Log.d(TAG, "Message received: " + buf);

        if (buf == null) {
            Log.d(TAG, "Empty message---possible bug.");
            return null;
        }

        String st = null;
        try {
            st = new String(buf, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }
        Log.d(TAG, "====UNPERSIST \n" + st);

        return new JSONObject(st);
    }

    public byte[] persist(JSONObject obj) {
        String st = obj.toString();
        Log.d(TAG, "==== PERSISTING\n" + st);
        return st.getBytes(Charset.forName("UTF-8"));
    }

    // Broadcast my score to everybody else.
    private void broadcastGameInfo(JSONObject obj) {
        byte[] buf = persist(obj);
        if (!mMultiplayer)
            return; // playing single-player mode, no need to inform anyone of anything

        // Send to every other participant.
        for (Participant p : mParticipants) {
            Log.d(TAG, "about to attempt to send to " + p.getParticipantId());
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if (mGameComplete) {
                Log.d(TAG, "sending reliable to " + p.getParticipantId());
                // final score notification must be sent via reliable message
                int result = Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null,
                        buf, mRoomId, p.getParticipantId());
            } else {
                sendUnreliable(buf, p.getParticipantId());
            }
        }
    }

    private void sendUnreliable(byte[] buf, String participantId) {
        Log.d(TAG, "sending unreliable to " + participantId);
        // it's an interim score notification, so we can use unreliable
        int result = Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient,
                buf, mRoomId, participantId);
        if (result == RealTimeMultiplayer.REAL_TIME_MESSAGE_FAILED) {
            Log.d(TAG, "failed to send message.");
            if (buf.length > Multiplayer.MAX_UNRELIABLE_MESSAGE_LEN) {
                Log.d(TAG, "It's too long by " + (buf.length - Multiplayer.MAX_UNRELIABLE_MESSAGE_LEN));
            }
        }
        if (result != RealTimeMultiplayer.REAL_TIME_MESSAGE_FAILED)
            Log.d(TAG, "success!");
    }

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // This array lists everything that's clickable, so we can install click
    // event handlers.
    final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_invite_players,
            R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out, R.id.button_single_player,
            R.id.button_single_player_2
    };

    // This array lists all the individual screens our game has.
    final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait
    };
    int mCurScreen = -1;

    void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        boolean showInvPopup;
        if (mIncomingInvitationId == null) {
            // no invitation, so no popup
            showInvPopup = false;
        } else if (mMultiplayer) {
            // if in multiplayer, only show invitation on main screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        } else {
            // single-player: show on main screen and gameplay screen
            showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);
        }
        findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
    }

    void switchToMainScreen() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            switchToScreen(R.id.screen_main);
        } else {
            switchToScreen(R.id.screen_sign_in);
        }
    }

    // updates the label that shows my score
    void updateAllPlayers(JSONObject obj) {
        updateDisplay();
        Log.d(TAG, "Updating players about something");
        broadcastGameInfo(obj);
    }

    private int mDiscardWidthR1 = 0;
    private int mDiscardWidthR2 = 0;

    private void updateDisplay() {
        // In case it's called too early
        if (mTurnData == null) { return; }
        for (Map.Entry<String, GameState.HandNode> entry : mTurnData.state.hands.entrySet()) {
            fragments.get(entry.getKey()).updateHand(entry.getValue().hand);
        }
        LinearLayout played = (LinearLayout) findViewById(R.id.played_pile);
        for (int i = 1; i < played.getChildCount(); i++) {
            ((TextView)played.getChildAt(i)).setText(String.valueOf(mTurnData.state.played[i - 1]));
        }
        ((TextView)findViewById(R.id.hints)).setText(
                String.valueOf(mTurnData.state.hintsRemaining));
        ((TextView)findViewById(R.id.misplays)).setText(
                String.valueOf(mTurnData.state.explosionsRemaining));
        if (mDiscardWidthR1 != 0) {
            LinearLayout row1 = ((LinearLayout) findViewById(R.id.discard_pile_row_1));
            row1.removeAllViews();

            int totalDiscarded = 0;
            for (int i = 0; i < mTurnData.state.discarded.length; i++) {
                totalDiscarded += mTurnData.state.discarded[i].size();
            }
            for (int i = 0; i < mTurnData.state.discarded.length; i++) {
                for (int j = 0; j < mTurnData.state.discarded[i].size(); j++) {
                    row1.addView(makeNewCardTextView(
                            mDiscardWidthR1/totalDiscarded, mTurnData.state.discarded[i].get(j)));
                }
            }
        }
        // TODO(sdspikes): add log

    }

    private TextView makeNewCardTextView(int width, GameState.Card card) {
        width = Math.min(width, MAX_CARD_WIDTH);
        width = Math.max(width, MIN_CARD_WIDTH);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                width, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView textView = new TextView(this);
        textView.setLayoutParams(params);
        textView.setGravity(Gravity.CENTER);
        textView.setText(String.valueOf(card.rank));
        textView.setBackgroundResource(HandFragment.cardColorToBGColor(card.color));
        textView.setTextColor(getResources().getColor(HandFragment.cardColorToTextColor(card.color)));
        return textView;
    }
    /*
     * MISC SECTION. Miscellaneous methods.
     */


    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    // In-game controls

    // Cancel the game. Should possibly wait until the game is canceled before
    // giving up on the view.
    public void onDiscardClicked(View view) {
        // allow user to click their cards, listener will deal with actually doing the work
        togglePlayOptionsVisible(false);
        mHandSelectionMode = true;
        mDiscardMode = true;
    }

    // Leave the game during your turn. Note that there is a separate
    // Games.TurnBasedMultiplayer.leaveMatch() if you want to leave NOT on your turn.
    public void onPlayClicked(View view) {
        // allow user to click their cards, listener will deal with actually doing the work
        togglePlayOptionsVisible(false);
        mHandSelectionMode = true;
        mPlayMode = true;
    }


    // Upload your new gamestate, then take a turn, and pass it on to the next
    // player.
    public void onGiveHintClicked(View view) {
        mTurnData.turnCounter += 1;

        // TODO(sdspikes): choose character to give info
        // TODO(sdspikes): choose attribute
        // TODO(sdspikes): notify all players
        updateAllPlayers(mTurnData.getJSONObject());
    }

    @Override
    public void onFragmentSelected(String playerId, int index) {
        if (mHandSelectionMode && playerId == mMyId) {
            GameState.HandNode node = mTurnData.state.hands.get(mMyId);
            GameState.Card removedCard = node.hand.remove(index);
            node.hand.add(mTurnData.state.deck.remove());
            if (mPlayMode) {
                if (mTurnData.state.played[removedCard.color.ordinal()] == removedCard.rank - 1) {
                    mTurnData.state.played[removedCard.color.ordinal()]++;
                } else {
                    mTurnData.state.explosionsRemaining--;
                }
            }
            if (mDiscardMode) {
                mTurnData.state.discarded[removedCard.color.ordinal()].add(removedCard);
            }
            mTurnData.state.currentPlayerId =
                    mTurnData.state.hands.get(mTurnData.state.currentPlayerId).nextPlayerId;
            Log.d(TAG, "updated currentId : " + mTurnData.state.currentPlayerId);
            mPlayMode = false;
            mDiscardMode = false;
            mHandSelectionMode = false;
            updateAllPlayers(mTurnData.getJSONObject());
        }
    }
}
