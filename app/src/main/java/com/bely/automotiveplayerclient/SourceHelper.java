package com.bely.automotiveplayerclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.service.media.MediaBrowserService;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SourceHelper {

    final String TAG = SourceHelper.class.getSimpleName();
    Handler mBGHandler;
    Context mContext;
    MediaControllerCompat mMediaController;
    MediaBrowserCompat mBrowser;
    UIListener mUIListener;
    boolean mRequestToPlay;
    private static SourceHelper mInstance;
    SourceHelper(Context context) {
        mContext = context;
        HandlerThread thread = new HandlerThread("StreamAppPlaybackHelper");
        thread.start();
        mBGHandler = new Handler(thread.getLooper());
    }

    public static SourceHelper getInstance() {
        if (mInstance == null) {
            synchronized (SourceHelper.class) {
                if (mInstance == null) {
                    mInstance = new SourceHelper(PlayerApp.getContext());
                }
            }
        }
        return mInstance;
    }



    public void connect(MusicPlayer.SourceInfo source) {
        Utils.browser = null;
        if (mBrowser != null && mBrowser.isConnected()) {
            mBrowser.disconnect();
        }
        if (mMediaController != null) {
            mMediaController.getTransportControls().stop();
            mMediaController.unregisterCallback(mSessionCallback);
            mMediaController = null;
        }
        resetPlaybackState();
        if (source == null) return;
        mUIListener.onMetadataChange("Connecting...", "");
        mUIListener.onAlbumartChange(source.getSourceIcon());
        checkSettingAvailability(source.getPkgName());
        mBGHandler.post(() -> {
            ComponentName component = new ComponentName(source.getPkgName(), source.getClsName());
            mBrowser = new MediaBrowserCompat(mContext, component, AppConnectCallback, null);
            mBrowser.connect();
        });
    }

    private void resetPlaybackState() {
        PlaybackStateCompat.Builder sb = new PlaybackStateCompat.Builder();
        sb.setState(PlaybackStateCompat.STATE_STOPPED, 0, 0);
        mUIListener.onPlaybackStateChange(sb.build());
    }

    private void checkSettingAvailability(String pkgname) {
        Intent intent = new Intent(Intent.ACTION_APPLICATION_PREFERENCES);
        intent.setPackage(pkgname);
        ResolveInfo info = mContext.getPackageManager().resolveActivity(intent, 0);
        if (info != null) {
            Intent settingIntent = new Intent(intent.getAction())
                    .setClassName(info.activityInfo.packageName, info.activityInfo.name);
            mUIListener.onShowSettingBtn(settingIntent);
        } else {
            mUIListener.onShowSettingBtn(null);
        }
    }


    public void play() {
        if (mMediaController == null) {
            mRequestToPlay = true;
        } else {
            mMediaController.getTransportControls().play();
        }
    }


    public void pause() {

    }


    public void stop() {

    }


    public void registerUIListener(UIListener listener) {
        mUIListener = listener;
    }


    public List<MusicPlayer.SourceInfo> loadSources() {
        return findStreamingMediaApps();
    }


    public void skipToPrev() {
        if (mMediaController != null) {
            mMediaController.getTransportControls().skipToPrevious();
        }
    }


    public void toggleplay() {
        if (mMediaController != null) {
            if (isPlaying()) {
                mMediaController.getTransportControls().pause();
            } else {
                mMediaController.getTransportControls().play();
            }
        }
    }

    private boolean isPlaying() {
        return mMediaController != null && mMediaController.getPlaybackState() != null &&
                (mMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING ||
                        mMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_BUFFERING);
    }


    public void skipToNext() {
        if (mMediaController != null) {
            mMediaController.getTransportControls().skipToNext();
        }
    }


    public void onCustomAction(PlaybackStateCompat.CustomAction a) {
        if (mMediaController != null) {
            mMediaController.getTransportControls().sendCustomAction(a, null);
        }
    }


    public void toggleShuffle() {
        if (mMediaController != null) {
            int shuffleM = mMediaController.getShuffleMode();
            if (shuffleM == PlaybackStateCompat.SHUFFLE_MODE_NONE || shuffleM == PlaybackStateCompat.SHUFFLE_MODE_INVALID) {
                shuffleM = PlaybackStateCompat.SHUFFLE_MODE_ALL;
            } else { //shuffle all or group
                shuffleM = PlaybackStateCompat.SHUFFLE_MODE_NONE;
            }
            mMediaController.getTransportControls().setShuffleMode(shuffleM);
        }
    }


    public void toggleRepeat() {
        if (mMediaController != null) {
            int repeatM = mMediaController.getRepeatMode();
            if (repeatM == PlaybackStateCompat.REPEAT_MODE_ALL || repeatM == PlaybackStateCompat.REPEAT_MODE_GROUP) {
                repeatM = PlaybackStateCompat.REPEAT_MODE_NONE;
            } else if (repeatM == PlaybackStateCompat.REPEAT_MODE_NONE || repeatM == PlaybackStateCompat.REPEAT_MODE_INVALID) {
                repeatM = PlaybackStateCompat.REPEAT_MODE_ONE;
            } else { //repeat one
                repeatM = PlaybackStateCompat.REPEAT_MODE_ALL;
            }
            mMediaController.getTransportControls().setRepeatMode(repeatM);
        }
    }


    private MediaControllerCompat.Callback mSessionCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onSessionDestroyed() {

        }

        @Override
        public void onSessionEvent(@NonNull String event, @Nullable Bundle extras) {

        }

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackStateCompat state) {
            if (mMediaController == null) return;
            mUIListener.onPlaybackStateChange(state);
            mUIListener.onShuffleMode(mMediaController.getShuffleMode());
            mUIListener.onRepeatMode(mMediaController.getRepeatMode());
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadataCompat metadata) {
            if (metadata == null) return;
            mUIListener.onMetadataChange(metadata.getString(MediaMetadata.METADATA_KEY_TITLE), metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
            mUIListener.onAlbumartChange(metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART));
            mUIListener.onDurationChanged(metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
        }

        @Override
        public void onQueueChanged(@Nullable List<MediaSessionCompat.QueueItem> queue) {

        }

        @Override
        public void onQueueTitleChanged(@Nullable CharSequence title) {

        }

        @Override
        public void onShuffleModeChanged(int shuffleMode) {
            super.onShuffleModeChanged(shuffleMode);
            mUIListener.onShuffleMode(mMediaController.getShuffleMode());
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            super.onRepeatModeChanged(repeatMode);
            mUIListener.onRepeatMode(mMediaController.getRepeatMode());
        }
    };

    private MediaBrowserCompat.ConnectionCallback AppConnectCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.i(TAG, "onConnected");
            mMediaController = new MediaControllerCompat(mContext, mBrowser.getSessionToken());
            mMediaController.registerCallback(mSessionCallback);

            mSessionCallback.onMetadataChanged(mMediaController.getMetadata());
            mSessionCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());

            if (mRequestToPlay) {
                mMediaController.getTransportControls().play();
                mRequestToPlay = false;
            }
            Utils.browser = mBrowser;
            mUIListener.onServiceConnected();
        }

        @Override
        public void onConnectionSuspended() {
            Utils.browser = null;
            mUIListener.onServiceDisconnected();
        }

        @Override
        public void onConnectionFailed() {
            Utils.browser = null;
            mUIListener.onServiceDisconnected();
        }
    };


    private List<MusicPlayer.SourceInfo> findStreamingMediaApps() {
        PackageManager packageManager = mContext.getPackageManager();

        Intent mediaBrowserServiceIntent = new Intent(MediaBrowserService.SERVICE_INTERFACE);

        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(mediaBrowserServiceIntent, PackageManager.GET_RESOLVED_FILTER);
        List<MusicPlayer.SourceInfo> list = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            String appName = resolveInfo.loadLabel(packageManager).toString();
            String packageName = resolveInfo.serviceInfo.packageName;

            Drawable appIcon = resolveInfo.loadIcon(packageManager);

            String serviceClassName = resolveInfo.serviceInfo.name;

            Log.d("MediaBrowserApps", "App Name: " + appName + ", Package Name: " + packageName +
                    ", Service Class: " + serviceClassName);

            Bitmap appIconBitmap = (appIcon instanceof BitmapDrawable) ?
                    ((BitmapDrawable) appIcon).getBitmap() : null;

            MusicPlayer.SourceInfo source = new MusicPlayer.SourceInfo(appName, packageName, serviceClassName, appIconBitmap, MusicPlayer.SourceInfo.SOURCE_TYPE.TYPE_APP);
            list.add(source);
        }
        return list;
    }
}
