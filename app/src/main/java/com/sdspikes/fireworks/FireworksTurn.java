/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sdspikes.fireworks;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Basic turn data. It's just a blank data string and a turn number counter.
 * 
 * @author wolff
 * 
 */
public class FireworksTurn {

    public static final String TAG = "EBTurn";

    public GameState state;
    public int turnCounter = 0;

    public FireworksTurn() {
    }

    // This is the byte array we will write out to the TBMP API.
    public JSONObject getJSONObject() {
        JSONObject retVal = new JSONObject();

        try {
            retVal.put("state", state.getJSONObject());
            retVal.put("turn", turnCounter);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return retVal;
    }

    // Creates a new instance of FireworksTurn.
    static public FireworksTurn unpersist(JSONObject obj) {
        FireworksTurn retVal = new FireworksTurn();

        try {
            if (!obj.has("state")) {
                return null;
            } else {
                retVal.state = new GameState(obj.getJSONObject("state"));
            }
            if (obj.has("turn")) {
                retVal.turnCounter = obj.getInt("turn");
            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return retVal;
    }
}
