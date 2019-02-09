/*
   Copyright 2019 tehguy

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.pixima.libmvgoogleplay;

import android.app.Activity;
import android.webkit.JavascriptInterface;

import com.google.android.gms.games.LeaderboardsClient;

class LeaderboardsHandler extends AbstractHandler<LeaderboardsClient> {
    static final String INTERFACE_NAME = "__google_play_leaderboards";

    private static final int RC_LEADERBOARD_UI = 9004;

    LeaderboardsHandler(Activity activity) {
        super(activity);
    }

    @JavascriptInterface
    public void showLeaderboardView(String boardId) {
        if (mClient == null) return;

        mClient.getLeaderboardIntent(boardId)
                .addOnSuccessListener(intent ->
                        mParentActivity.startActivityForResult(intent, RC_LEADERBOARD_UI));
    }

    @JavascriptInterface
    public void addScoreToLeaderboard(String boardId, int score) {
        if (mClient == null) return;

        mClient.submitScore(boardId, score);
    }
}
