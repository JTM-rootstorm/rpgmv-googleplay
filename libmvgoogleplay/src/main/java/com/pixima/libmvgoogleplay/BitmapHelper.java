package com.pixima.libmvgoogleplay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class BitmapHelper {
    private static BitmapHelper INSTANCE;

    private BitmapHelper() {}

    static BitmapHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BitmapHelper();
        }

        return INSTANCE;
    }

    @Nullable
    Bitmap screenshot(@NonNull WebView webView) {
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
