package com.bely.automotiveplayerclient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.media.session.PlaybackStateCompat;

public interface UIListener {
    void onMetadataChange(String title, String artist);
    void onAlbumartChange(Bitmap albumart);
    void onPlaybackStateChange(PlaybackStateCompat state);
    void onShowSettingBtn(Intent intent);

    void onShuffleMode(int shuffleMode);

    void onRepeatMode(int repeatMode);

    void onDurationChanged(long aLong);

    void onServiceConnected();

    void onServiceDisconnected();
}
