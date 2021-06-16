package com.kapil.musicplayer.helpers;

import android.util.Log;

import com.kapil.musicplayer.ui.PlaybackInfoListener;

public class SetPlaybackInfoListenerHelper {

    private static final String TAG = "SetPlaybackInfoListener";

    public static SetPlaybackInfoListenerHelper controller = new SetPlaybackInfoListenerHelper();
    private PlaybackInfoListener playbackInfoListener;

    public void setPlaybackInfoListener(PlaybackInfoListener playbackInfoListener) {
        Log.d(TAG, "setPlaybackInfoListener: ");
        this.playbackInfoListener = playbackInfoListener;
    }

    public PlaybackInfoListener getPlaybackInfoListener() {
        Log.d(TAG, "getPlaybackInfoListener: ");
        return playbackInfoListener;
    }
}
