package com.pixima.libmvgoogleplay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;

final class BitmapHelper {
    private static BitmapHelper INSTANCE;

    private BitmapHelper() {}

    static BitmapHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BitmapHelper();
        }

        return INSTANCE;
    }

    @Nullable Bitmap screenshot(@NonNull WebView webView) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);
            return bitmap;
        } catch (Exception ignored) {}

        return null;
    }
}
