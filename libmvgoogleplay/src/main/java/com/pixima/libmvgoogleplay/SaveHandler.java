package com.pixima.libmvgoogleplay;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.Objects;

class SaveHandler extends AbstractHandler<SnapshotsClient> {
    static final String INTERFACE_NAME = "__google_play_save";

    private static final int RC_LIST_SAVED_GAMES = 9009;

    private String mCurrentSaveName = "snapshotTemp";

    private WebView mWebView;

    SaveHandler(Activity activity, WebView webView) {
        super(activity);
        mWebView = webView;
    }

    @JavascriptInterface
    public void showSavedGamesUI() {
        Resources res = mParentActivity.getResources();
        mClient.getSelectSnapshotIntent(mParentActivity.getString(R.string.gplay_see_saved_games),
                true, true, res.getInteger(R.integer.gplay_saved_games_to_show))
                .addOnSuccessListener(intent ->
                        mParentActivity.startActivityForResult(intent, RC_LIST_SAVED_GAMES));
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) return;

        if (data.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
            SnapshotMetadata metadata = data.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
            mCurrentSaveName = metadata.getUniqueName();

            /* TODO: figure out how to load into MV */
        }
        else if (data.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
            String unique = Long.toString(System.currentTimeMillis());
            mCurrentSaveName = "snapshotTemp-" + unique;
            saveSnapshot(null);
        }
    }

    private void saveSnapshot(final SnapshotMetadata metadata) {
        final boolean useMetadata = metadata != null && metadata.getUniqueName() != null;
        SnapshotsClient.DataOrConflict<Snapshot> result;

        if (useMetadata) {
            Log.i(INTERFACE_NAME, "Opening snapshot using metadata: " + metadata);
            result = mClient.open(metadata,
                    SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED).getResult();
        }
        else {
            Log.i(INTERFACE_NAME, "Opening snapshot using currentSaveName: " + mCurrentSaveName);
            result = mClient.open(mCurrentSaveName, true,
                    SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED).getResult();
        }

        Snapshot snapshot = Objects.requireNonNull(result).isConflict() ?
                null
                : result.getData();

        Bitmap coverImage = BitmapHelper.getInstance().screenshot(mWebView);

        /* TODO: figure out how to get MV save data in here */
    }

    private Task<SnapshotMetadata> writeSnapshot(@NonNull Snapshot snapshot, byte[] data,
                                                 Bitmap coverImage, String desc) {
        snapshot.getSnapshotContents().writeBytes(data);

        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                .setCoverImage(coverImage)
                .setDescription(desc)
                .build();

        return mClient.commitAndClose(snapshot, metadataChange);
    }

    @NonNull private Task<byte[]> loadSnapshot(final String saveName) {
        int conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;

        return mClient.open(saveName, true, conflictResolutionPolicy)
                .addOnFailureListener(e ->
                        Log.e(INTERFACE_NAME, "Error while opening snapshot.", e))
                .continueWith(task -> {
                    try {
                        Snapshot snapshot = Objects.requireNonNull(task.getResult()).getData();
                        return Objects.requireNonNull(snapshot).getSnapshotContents().readFully();
                    } catch (IOException | NullPointerException e) {
                        Log.e(INTERFACE_NAME, "Error while reading snapshot.", e);
                    }

                    return null;
                }).addOnCompleteListener(task -> {
                    // do a thing
                });
    }
}
