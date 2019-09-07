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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.games.Games;

public class GPlayMain {
    private static final String INTERFACE_NAME = "__google_play_main";

    private static final int RC_SIGN_IN = 9001;

    private Activity mParentActivity;

    private GoogleSignInClient mGoogleSignInClient;

    private AchievementsHandler mAchievementsHandler;
    private EventsHandler mEventsHandler;
    private LeaderboardsHandler mLeaderboardsHandler;

    private boolean enable_achievements;
    private boolean enable_events;
    private boolean enable_leaderboards;
    private boolean enable_auto_signin;

    private boolean manualSignOut = false;
    private boolean isFirstStart = true;

    @SuppressLint("AddJavascriptInterface")
    public GPlayMain(@NonNull Context context, @NonNull WebView webView) {
        mParentActivity = ((Activity) context);

        if (!isGooglePlayServicesAvailable(mParentActivity)) return;

        Resources res = mParentActivity.getResources();

        enable_achievements = res.getBoolean(R.bool.gplay_enable_achievements);
        enable_events = res.getBoolean(R.bool.gplay_enable_events);
        enable_leaderboards = res.getBoolean(R.bool.gplay_enable_leaderboards);
        enable_auto_signin = res.getBoolean(R.bool.gplay_enable_auto_signin);

        String val = mParentActivity.getString(R.string.app_id);

        if (val.contains("YOUR_") || val.isEmpty()) {
            Log.d(INTERFACE_NAME, "The APP_ID in ids.xml for this app has not been set, " +
                    "Google Play Services will not be initialized");
        }

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(mParentActivity, googleSignInOptions);

        webView.addJavascriptInterface(this, INTERFACE_NAME);

        if (enable_achievements) {
            mAchievementsHandler = new AchievementsHandler(mParentActivity);
            webView.addJavascriptInterface(mAchievementsHandler, AchievementsHandler.INTERFACE_NAME);
        }

        if (enable_events) {
            mEventsHandler = new EventsHandler(mParentActivity);
            webView.addJavascriptInterface(mEventsHandler, EventsHandler.INTERFACE_NAME);
        }

        if (enable_leaderboards) {
            mLeaderboardsHandler = new LeaderboardsHandler(mParentActivity);
            webView.addJavascriptInterface(mLeaderboardsHandler, LeaderboardsHandler.INTERFACE_NAME);
        }
    }

    public void OnActivityResult(int requestCode, int resultCode, Intent data) {
        if (!isGooglePlayServicesAvailable(mParentActivity)) return;
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()) {
                GoogleSignInAccount signInAccount = result.getSignInAccount();
                onConnected(signInAccount);

                manualSignOut = false;
            }
            else {
                int statusCode = result.getStatus().getStatusCode();

                onDisconnected();

                handleErrorStatusCodes(statusCode);
            }
        }
    }

    public void onResume() {
        if (!isGooglePlayServicesAvailable(mParentActivity)) return;
        if (!manualSignOut && enable_auto_signin) {
            startSilentSignIn();
        }
    }

    public void onStart() {
        if (!isGooglePlayServicesAvailable(mParentActivity)) return;
        if (!manualSignOut && enable_auto_signin) {
            startInteractiveSignIn();
        }
    }

    @SuppressWarnings("WeakerAccess")
    @JavascriptInterface
    public void startInteractiveSignIn() {
        mParentActivity.startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    @JavascriptInterface
    public void signOut() {
        if (!isSignedIn()) return;

        mGoogleSignInClient.signOut().addOnCompleteListener(mParentActivity,
                task -> onDisconnected());

        manualSignOut = true;
    }

    @SuppressWarnings("WeakerAccess")
    @JavascriptInterface
    public boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(mParentActivity) != null;
    }

    private boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    private void startSilentSignIn() {
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(mParentActivity,
                task -> {
                    if (task.isSuccessful()) {
                        GoogleSignInAccount signInAccount = task.getResult();
                        onConnected(signInAccount);
                    }
                    else {
                        ApiException exception = ((ApiException) task.getException());

                        if (exception != null) {
                            if (exception.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                                startSilentSignIn();
                            }
                            else {
                                handleErrorStatusCodes(exception.getStatusCode());
                            }
                        }
                        else {
                            Log.e(INTERFACE_NAME, "Sign-in is really broken.");
                        }
                    }
                });
    }

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        if (enable_achievements) {
            mAchievementsHandler.setClient(Games.getAchievementsClient(mParentActivity,
                    googleSignInAccount));
            mAchievementsHandler.unlockCachedAchievements();
            mAchievementsHandler.cacheAchievements(isFirstStart);
        }

        if (enable_events) {
            mEventsHandler.setClient(Games.getEventsClient(mParentActivity, googleSignInAccount));
            mEventsHandler.incrementCachedEvents();
            mEventsHandler.cacheEvents(isFirstStart);
        }

        if (enable_leaderboards) {
            mLeaderboardsHandler.setClient(Games.getLeaderboardsClient(mParentActivity,
                    googleSignInAccount));
        }

        if (isFirstStart) {
            isFirstStart = false;
        }
    }

    private void onDisconnected() {
        if (enable_achievements) {
            mAchievementsHandler.setClient(null);
        }

        if (enable_events) {
            mEventsHandler.setClient(null);
        }

        if (enable_leaderboards) {
            mLeaderboardsHandler.setClient(null);
        }
    }

    private void handleErrorStatusCodes(int statusCode) {
        String message;

        switch (statusCode) {
            case CommonStatusCodes.API_NOT_CONNECTED:
                message = mParentActivity.getString(R.string.gplay_api_not_connected);
                break;
            case CommonStatusCodes.CANCELED:
                message = mParentActivity.getString(R.string.gplay_api_cancelled);
                break;
            case CommonStatusCodes.DEVELOPER_ERROR:
                message = mParentActivity.getString(R.string.gplay_api_misconfigured);
                break;
            case CommonStatusCodes.ERROR:
                message = mParentActivity.getString(R.string.gplay_api_error);
                break;
            case CommonStatusCodes.INTERNAL_ERROR: case CommonStatusCodes.NETWORK_ERROR:
                message = mParentActivity.getString(R.string.gplay_api_internal_network_error);
                break;
            case CommonStatusCodes.INVALID_ACCOUNT:
                message = mParentActivity.getString(R.string.gplay_api_invalid_account);
                break;
            case CommonStatusCodes.TIMEOUT:
                message = mParentActivity.getString(R.string.gplay_api_timeout);
                break;
            case GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS:
                // we ignore this one
                return;
            default:
                message = mParentActivity.getString(R.string.gplay_api_unspecified) + statusCode;
                break;
        }

        new AlertDialog.Builder(mParentActivity).setMessage(message)
                .setNeutralButton(android.R.string.ok, null).show();
    }
}
