package com.google.android.clockwork.tiles;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

@TargetApi(24)
public class TileProviderUpdateRequester {
    private final Context mContext;
    private final ComponentName mProviderComponent;

    public TileProviderUpdateRequester(Context context, ComponentName providerComponent) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        } else if (providerComponent == null) {
            throw new IllegalArgumentException("ProviderComponent cannot be null");
        } else {
            this.mContext = context;
            this.mProviderComponent = providerComponent;
        }
    }

    public final Intent buildIntentToRequestUpdateAll() {
        Intent intent = new Intent("android.support.wearable.tiles.ACTION_REQUEST_UPDATE_ALL");
        intent.setPackage("com.google.android.wearable.app");
        intent.putExtra("android.support.wearable.tiles.EXTRA_PROVIDER_COMPONENT", this.mProviderComponent);
        if (Build.VERSION.SDK_INT >= 31)
            intent.putExtra("android.support.wearable.tiles.EXTRA_PENDING_INTENT", PendingIntent.getActivity(this.mContext, 0, new Intent(""), PendingIntent.FLAG_IMMUTABLE));
        else
            intent.putExtra("android.support.wearable.tiles.EXTRA_PENDING_INTENT", PendingIntent.getActivity(this.mContext, 0, new Intent(""), 0));
        return intent;

    }

    public final void requestUpdateAll() {
        this.mContext.sendBroadcast(buildIntentToRequestUpdateAll());
    }
}