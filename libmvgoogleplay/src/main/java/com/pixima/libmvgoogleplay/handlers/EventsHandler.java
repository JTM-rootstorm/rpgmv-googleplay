package com.pixima.libmvgoogleplay.handlers;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.JavascriptInterface;

import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EventsHandler extends AbstractHandler<EventsClient> {
    public static final String INTERFACE_NAME = "__google_play_events";

    private Map<String, EventShell> mEventsCache;

    public EventsHandler(Activity activity) {
        super(activity);

        mEventsCache = new HashMap<>();
    }

    @JavascriptInterface
    public void incrementEvent(String eventId, int stepAmount) {
        if (mClient != null) {
            mClient.increment(eventId, stepAmount);
        }
    }

    @JavascriptInterface
    public String getAllEventDataAsJson() {
        return !mEventsCache.isEmpty() ? gson.toJson(mEventsCache.values().toArray()) : null;
    }

    public void cacheEvents(boolean forceReload) {
        mClient.load(forceReload)
                .addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                    @Override
                    public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                        if (task.isSuccessful()) {
                            try {
                                EventBuffer eventBuffer = Objects.requireNonNull(task.getResult()).get();

                                int buffSize = eventBuffer != null ? eventBuffer.getCount() : 0;

                                for (int i = 0; i < buffSize; i++) {
                                    Event event = eventBuffer.get(i).freeze();
                                    EventShell shell = new EventShell(event);

                                    mEventsCache.put(event.getEventId(), shell);
                                }

                                if (eventBuffer != null) {
                                    eventBuffer.release();
                                }
                            }
                            catch (NullPointerException ignored) {}
                        }
                    }
                });
    }

    private static class EventShell {
        String id;
        String name;
        String desc;
        String formattedVal;
        Uri imageUri;
        long val;

        EventShell(Event event) {
            id = event.getEventId();
            name = event.getName();
            desc = event.getDescription();
            formattedVal = event.getFormattedValue();
            imageUri = event.getIconImageUri();
            val = event.getValue();
        }
    }
}
