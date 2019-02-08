package com.pixima.libmvgoogleplay.handlers;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;

import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.tasks.OnSuccessListener;

public class AchievementsHandler extends AbstractHandler<AchievementsClient> {
    public static final String INTERFACE_NAME = "__google_play_achievements";

    private static final int RC_ACHIEVEMENT_UI = 9003;

    public AchievementsHandler(Activity activity) {
        super(activity);
    }

    @JavascriptInterface
    public void showAchievementView() {
        if (mClient != null) {
            mClient.getAchievementsIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            mParentActivity.startActivityForResult(intent, RC_ACHIEVEMENT_UI);
                        }
                    });
        }
    }

    @JavascriptInterface
    public void unlockAchievement(String achievementId) {
        if (mClient != null) {
            mClient.unlock(achievementId);
        }
    }
}
