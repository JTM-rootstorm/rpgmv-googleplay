package com.pixima.libmvgoogleplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.games.Games;
import com.pixima.libmvgoogleplay.handlers.AchievementsHandler;
import com.pixima.libmvgoogleplay.handlers.EventsHandler;

public class GPlayMain {
    private static final String INTERFACE_NAME = "__google_play_main";

    private static int RC_SIGN_IN = 9001;

    private Activity mParentActivity;

    private GoogleSignInClient mGoogleSignInClient;

    private AchievementsHandler mAchievementsHandler;
    private EventsHandler mEventsHandler;

    private boolean enable_achievements = (R.bool.enable_achievements != 0);
    private boolean enable_events = (R.bool.enable_events != 0);
    private boolean enable_leaderboards = (R.bool.enable_leaderboards != 0);
    private boolean enable_auto_signin = (R.bool.enable_auto_signin != 0);

    private boolean manualSignOut = false;
    private boolean firstStart = true;

    public GPlayMain(@NonNull Context context, @NonNull WebView webView) {
        mParentActivity = ((Activity) context);

        String val = mParentActivity.getString(R.string.app_id);

        if (val.startsWith("YOUR_")) {
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
    }

    public void OnActivityResult(int requestCode, int resultCode, Intent data) {
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
        if (!manualSignOut && enable_auto_signin) {
            startSilentSignIn();
        }
    }

    public void onStart() {
        if (!manualSignOut && enable_auto_signin) {
            startInteractiveSignIn();
        }
    }

    @JavascriptInterface
    public void startInteractiveSignIn() {
        mParentActivity.startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
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
        }

        if (enable_events) {
            mEventsHandler.setClient(Games.getEventsClient(mParentActivity, googleSignInAccount));
        }

        if (firstStart) {
            if (enable_events) {
                mEventsHandler.cacheEvents(true);
            }

            firstStart = false;
        }
        else {
            if (enable_events) {
                mEventsHandler.cacheEvents(false);
            }
        }
    }

    private void onDisconnected() {
        if (enable_achievements) {
            mAchievementsHandler.setClient(null);
        }

        if (enable_events) {
            mEventsHandler.setClient(null);
        }
    }

    private void handleErrorStatusCodes(int statusCode) {
        String message;

        switch (statusCode) {
            case CommonStatusCodes.API_NOT_CONNECTED:
                message = mParentActivity.getString(R.string.api_not_connected);
                break;
            case CommonStatusCodes.CANCELED:
                message = mParentActivity.getString(R.string.api_cancelled);
                break;
            case CommonStatusCodes.DEVELOPER_ERROR:
                message = mParentActivity.getString(R.string.api_misconfigured);
                break;
            case CommonStatusCodes.ERROR:
                message = mParentActivity.getString(R.string.api_error);
                break;
            case CommonStatusCodes.INTERNAL_ERROR: case CommonStatusCodes.NETWORK_ERROR:
                message = mParentActivity.getString(R.string.api_internal_network_error);
                break;
            case CommonStatusCodes.INVALID_ACCOUNT:
                message = mParentActivity.getString(R.string.api_invalid_account);
                break;
            case CommonStatusCodes.TIMEOUT:
                message = mParentActivity.getString(R.string.api_timeout);
                break;
            case GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS:
                // we ignore this one
                return;
            default:
                message = mParentActivity.getString(R.string.api_unspecified) + statusCode;
                break;
        }

        new AlertDialog.Builder(mParentActivity).setMessage(message)
                .setNeutralButton(android.R.string.ok, null).show();
    }
}
