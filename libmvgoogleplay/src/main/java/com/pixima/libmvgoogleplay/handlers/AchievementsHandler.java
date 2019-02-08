package com.pixima.libmvgoogleplay.handlers;

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

public class AchievementsHandler extends AbstractHandler<AchievementsClient> {
    public static final String INTERFACE_NAME = "__google_play_achievements";

    private static final int RC_ACHIEVEMENT_UI = 9003;

    private Map<String, AchievementShell> mAchievementCache;

    public AchievementsHandler(Activity activity) {
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

    public void cacheAchievements(boolean forceReload) {
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
