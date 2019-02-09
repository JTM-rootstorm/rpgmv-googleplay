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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.JavascriptInterface;

import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class AchievementsHandler extends AbstractHandler<AchievementsClient> {
    static final String INTERFACE_NAME = "__google_play_achievements";

    private static final int RC_ACHIEVEMENT_UI = 9003;

    private Map<String, AchievementShell> mAchievementCache;

    AchievementsHandler(Activity activity) {
        super(activity);

        mAchievementCache = new HashMap<>();
    }

    @JavascriptInterface
    public void showAchievementView() {
        if (mClient != null) {
            mClient.getAchievementsIntent()
                    .addOnSuccessListener(intent ->
                            mParentActivity.startActivityForResult(intent, RC_ACHIEVEMENT_UI));
        }
    }

    @JavascriptInterface
    public void unlockAchievement(String achievementId) {
        if (mClient != null) {
            mClient.unlock(achievementId);
        }
    }

    @JavascriptInterface
    public void incrementAchievementStep(String achievementId, int stepAmount) {
        if (mClient != null) {
            mClient.increment(achievementId, stepAmount);
        }
    }

    @JavascriptInterface
    public String getAllAchievementDataAsJSON() {
        return !mAchievementCache.isEmpty() ?
                gson.toJson(mAchievementCache.values().toArray())
                : null;
    }

    void cacheAchievements(boolean forceReload) {
        mClient.load(forceReload)
                .addOnCompleteListener(task -> {
                    try {
                        AchievementBuffer achievementBuffer = Objects.requireNonNull(task.getResult()).get();

                        int buffSize = Objects.requireNonNull(achievementBuffer).getCount();

                        for (int i = 0; i < buffSize; i++){
                            Achievement achievement = achievementBuffer.get(i).freeze();
                            AchievementShell achievementShell = new AchievementShell(achievement);

                            mAchievementCache.put(achievementShell.id, achievementShell);
                        }

                        achievementBuffer.release();
                    }
                    catch (NullPointerException ignored) {}
                });
    }

    private static class AchievementShell {
        String id;
        String name;
        String desc;
        Uri    revealedImageUri;
        Uri    unlockedImageUri;
        int    state;
        int    type;
        int    currentSteps = 0;
        int    stepsToUnlock = 0;

        AchievementShell(@NonNull Achievement achievement) {
            id = achievement.getAchievementId();
            name = achievement.getName();
            desc = achievement.getDescription();
            revealedImageUri = achievement.getRevealedImageUri();
            unlockedImageUri = achievement.getUnlockedImageUri();
            state = achievement.getState();
            type = achievement.getType();

            if (type == Achievement.TYPE_INCREMENTAL) {
                currentSteps = achievement.getCurrentSteps();
                stepsToUnlock = achievement.getTotalSteps();
            }
        }
    }
}
