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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class AchievementsHandler extends AbstractHandler<AchievementsClient> {
    static final String INTERFACE_NAME = "__google_play_achievements";

    private static final int RC_ACHIEVEMENT_UI = 9003;

    private Map<String, AchievementShell> mAchievementCache;
    private List<String> mUnlockQueue;

    AchievementsHandler(Activity activity) {
        super(activity);

        mAchievementCache = new HashMap<>();
        mUnlockQueue = new ArrayList<>();
    }

    @JavascriptInterface
    public void showAchievementView() {
        if (mClient != null) {
            mClient.getAchievementsIntent()
                    .addOnSuccessListener(intent ->
                            mParentActivity.startActivityForResult(intent, RC_ACHIEVEMENT_UI));
        }
    }

    @SuppressWarnings("WeakerAccess")
    @JavascriptInterface
    public void unlockAchievement(@NonNull String achievementId) {
        if (mClient != null) {
            mClient.unlock(achievementId);
        }

        if (!mAchievementCache.isEmpty()) {
            AchievementShell shell = mAchievementCache.get(achievementId);

            if (shell == null) return;
            if (shell.state == Achievement.STATE_UNLOCKED) return;

            shell.state = Achievement.STATE_UNLOCKED;

            mUnlockQueue.add(shell.id);
            mAchievementCache.put(achievementId, shell);
        }
    }

    @JavascriptInterface
    public void incrementAchievementStep(String achievementId, int stepAmount) {
        if (mClient != null) {
            mClient.increment(achievementId, stepAmount);
        }

        if (!mAchievementCache.isEmpty()) {
            AchievementShell shell = mAchievementCache.get(achievementId);

            if (shell == null) return;
            if (shell.type != Achievement.TYPE_INCREMENTAL
                    && shell.state == Achievement.STATE_UNLOCKED) return;

            shell.currentSteps += stepAmount;

            mAchievementCache.put(achievementId, shell);

            if (shell.currentSteps >= shell.stepsToUnlock) {
                unlockAchievement(achievementId);
            }
        }
    }

    @JavascriptInterface
    public String getAllAchievementDataAsJson() {
        return !mAchievementCache.isEmpty() ?
                gson.toJson(mAchievementCache.values().toArray())
                : null;
    }

    @JavascriptInterface
    public String getAchievementDataAsJson(String achievementId) {
        return !mAchievementCache.isEmpty() ?
                gson.toJson(mAchievementCache.get(achievementId))
                : null;
    }

    void cacheAchievements(boolean forceReload) {
        mClient.load(forceReload)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;

                    try {
                        AchievementBuffer achievementBuffer = Objects.requireNonNull(task.getResult()).get();

                        int buffSize = achievementBuffer != null ? achievementBuffer.getCount() : 0;

                        for (int i = 0; i < buffSize; i++){
                            Achievement achievement = achievementBuffer.get(i).freeze();
                            AchievementShell achievementShell = new AchievementShell(achievement);

                            mAchievementCache.put(achievementShell.id, achievementShell);
                        }

                        if (achievementBuffer != null) {
                            achievementBuffer.release();
                        }
                    }
                    catch (NullPointerException ignored) {}
                });
    }

    void unlockCachedAchievements() {
        if (mUnlockQueue.isEmpty()) return;

        for (String achievementId : mUnlockQueue) {
            unlockAchievement(achievementId);
        }

        mUnlockQueue.clear();
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
