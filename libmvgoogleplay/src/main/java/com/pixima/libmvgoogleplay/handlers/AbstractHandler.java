package com.pixima.libmvgoogleplay.handlers;

import android.app.Activity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

abstract class AbstractHandler<T> {
    Activity mParentActivity;

    T mClient = null;

    static Gson gson = new GsonBuilder().serializeNulls().create();

    AbstractHandler(Activity activity) {
        mParentActivity = activity;
    }

    public void setClient(T serviceClient) {
        mClient = serviceClient;
    }
}
