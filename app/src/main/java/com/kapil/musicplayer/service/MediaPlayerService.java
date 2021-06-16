package com.kapil.musicplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.kapil.musicplayer.helpers.AudioListHelper;
import com.kapil.musicplayer.R;
import com.kapil.musicplayer.helpers.FavouriteListHelper;
import com.kapil.musicplayer.helpers.OnlineModeListHelper;
import com.kapil.musicplayer.helpers.SetPlaybackInfoListenerHelper;
import com.kapil.musicplayer.model.Audio;
import com.kapil.musicplayer.ui.MainActivity;
import com.kapil.musicplayer.ui.PlaybackInfoListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MediaPlayerService extends MediaBrowserServiceCompat
        implements  MediaPlayer.OnCompletionListener,MediaPlayer.OnSeekCompleteListener ,
                    MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,
                    AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "MediaPlayerService";

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    private MediaPlayer mediaPlayer;
    private MediaControllerCompat controllerCompat;
    private String url;
    private int resumePos = -1;
    public PlaybackInfoListener playbackInfoListener;
    private Timer timer;
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private int currState;

    private void newStartForeground(Notification notification) {

    }

    //MediaSession
    private MediaSessionCompat mediaSessionCompat;
    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay: " +Integer.toString(currState));

            if (currState == PlaybackStateCompat.STATE_PAUSED) {
                resumeMedia();
                Notification notification = builtNotification(PlaybackStateCompat.STATE_PLAYING);
                MediaPlayerService.this.startForeground(NOTIFICATION_ID,notification);
                mediaSessionCompat.setActive(true);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                currState = PlaybackStateCompat.STATE_PLAYING;
                return;
            }

            stopMedia();
            mediaSessionCompat.setActive(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            currState = PlaybackStateCompat.STATE_PLAYING;
            initMediaPlayer();
            Notification notification = builtNotification(PlaybackStateCompat.STATE_PLAYING);
            MediaPlayerService.this.startForeground(NOTIFICATION_ID,notification);
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause: ");

            pauseMedia();
            mediaSessionCompat.setActive(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            currState = PlaybackStateCompat.STATE_PAUSED;
            builtNotification(PlaybackStateCompat.STATE_PAUSED);
            Notification notification = builtNotification(PlaybackStateCompat.STATE_PAUSED);
            MediaPlayerService.this.stopForeground(true);
            NotificationManagerCompat.from(MediaPlayerService.this).notify(NOTIFICATION_ID,notification);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Log.d(TAG, "onCustomAction: " + action);

            switch (action) {
                case "resume":
                    resumeMedia();
                    Notification notification = builtNotification(PlaybackStateCompat.STATE_PLAYING);
                    MediaPlayerService.this.startForeground(NOTIFICATION_ID,notification);
                    mediaSessionCompat.setActive(true);
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    currState = PlaybackStateCompat.STATE_PLAYING;
                    break;
                case "setPlaybackInfoListener":
                    playbackInfoListener = SetPlaybackInfoListenerHelper.controller.getPlaybackInfoListener();
                    break;
                case "play":
                    Log.d(TAG, "onCustomAction: " +Integer.toString(currState));
                    stopMedia();
                    mediaSessionCompat.setActive(true);
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    currState = PlaybackStateCompat.STATE_PLAYING;
                    initMediaPlayer();
                    notification = builtNotification(PlaybackStateCompat.STATE_PLAYING);
                    MediaPlayerService.this.startForeground(NOTIFICATION_ID,notification);
            }
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext: ");
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);

            if (incSelectedFragmentCurrIndex()) {
                stopMedia();
                mediaSessionCompat.setActive(true);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                currState = PlaybackStateCompat.STATE_PLAYING;
                initMediaPlayer();
                Notification notification = builtNotification(PlaybackStateCompat.STATE_PLAYING);
                MediaPlayerService.this.startForeground(NOTIFICATION_ID, notification);
            } else {
                Toast.makeText(getApplicationContext(),"Audio List Completed",Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious: ");
            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);

            if (decSelectedFragmentCurrIndex()) {

                stopMedia();
                mediaSessionCompat.setActive(true);
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                currState = PlaybackStateCompat.STATE_PLAYING;
                initMediaPlayer();
                Notification notification = builtNotification(PlaybackStateCompat.STATE_PLAYING);
                MediaPlayerService.this.startForeground(NOTIFICATION_ID, notification);
            } else {
                Toast.makeText(getApplicationContext(),"Cannot skip to previous..",Toast.LENGTH_SHORT).show();
            }
        }

        private boolean incSelectedFragmentCurrIndex () {
            if (MainActivity.activeFragment == 0) {
                if (AudioListHelper.audioListHelper.currIndex >= 0 &&
                        AudioListHelper.audioListHelper.currIndex < AudioListHelper.audioListHelper.audioArrayList.size()-1) {
                    AudioListHelper.audioListHelper.currIndex++;
                    AudioListHelper.audioListHelper.saveCurrIndex();
                    return true;
                }
                return false;
            } else if (MainActivity.activeFragment == 1) {
                if (FavouriteListHelper.favouriteListHelper.currIndex >= 0 &&
                        FavouriteListHelper.favouriteListHelper.currIndex < FavouriteListHelper.favouriteListHelper.favouriteList.size()-1) {

                    FavouriteListHelper.favouriteListHelper.currIndex++;
                    FavouriteListHelper.favouriteListHelper.saveCurrIndex();
                    return true;
                }
                return false;
            } else {
                if (OnlineModeListHelper.onlineModeListHelper.currIndex >= 0 &&
                        OnlineModeListHelper.onlineModeListHelper.currIndex < OnlineModeListHelper.onlineModeListHelper.onlineList.size()-1) {

                    OnlineModeListHelper.onlineModeListHelper.currIndex++;
                    OnlineModeListHelper.onlineModeListHelper.saveCurrIndex();
                    return true;
                }
                return false;
            }
        }

        private boolean decSelectedFragmentCurrIndex () {
            if (MainActivity.activeFragment == 0) {
                if (AudioListHelper.audioListHelper.currIndex > 0 &&
                        AudioListHelper.audioListHelper.currIndex < AudioListHelper.audioListHelper.audioArrayList.size()) {
                    AudioListHelper.audioListHelper.currIndex--;
                    AudioListHelper.audioListHelper.saveCurrIndex();
                    return true;
                }
                return false;
            } else {
                if (FavouriteListHelper.favouriteListHelper.currIndex > 0 &&
                        FavouriteListHelper.favouriteListHelper.currIndex < FavouriteListHelper.favouriteListHelper.favouriteList.size()) {

                    FavouriteListHelper.favouriteListHelper.currIndex--;
                    FavouriteListHelper.favouriteListHelper.saveCurrIndex();
                    return true;
                }
                return false;
            }
        }

        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "onSeekTo: ");

            seekTo((int) pos);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {

            KeyEvent key = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            Log.d(TAG,"onMediaButtonEvent: " + String.valueOf(key.getKeyCode()));
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

    };

    private void handleIntent( Intent intent ) {

        Log.d(TAG, "handleIntent: " + intent.getAction());

        if( intent == null || intent.getAction() == null )
            return;
        String action = intent.getAction();
        if( action.equalsIgnoreCase( ACTION_PLAY ) ) {

            controllerCompat.getTransportControls().play();

        } else if( action.equalsIgnoreCase( ACTION_PAUSE ) ) {
            controllerCompat.getTransportControls().pause();
        } else if( action.equalsIgnoreCase( ACTION_PREVIOUS ) ) {
            controllerCompat.getTransportControls().skipToPrevious();
        } else if( action.equalsIgnoreCase( ACTION_NEXT ) ) {
            controllerCompat.getTransportControls().skipToNext();
        } else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
            controllerCompat.getTransportControls().stop();
        }
    }

    private static final int NOTIFICATION_ID = 102;

    private BroadcastReceiver becomingNoisyReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            controllerCompat.getTransportControls().pause();
        }
    };


    private void registerBecomingNoisyReciever() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReciever,intentFilter);
    }

    private void callStateListener () {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_OFFHOOK ||
                        state == TelephonyManager.CALL_STATE_RINGING) {
                    if (mediaPlayer != null) {
                        controllerCompat.getTransportControls().pause();
                        ongoingCall = true;
                    }
                }
                else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    if (mediaPlayer != null && ongoingCall) {
                        ongoingCall = false;
                        controllerCompat.getTransportControls().play();
                    }
                }
            }
        };
        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**************************     Listener Override Methods        *******************************/

    @Override
    public void onAudioFocusChange (int focusState) {

        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(TAG, "onAudioFocusChange: play");
                controllerCompat.getTransportControls().play();
                if (mediaPlayer!= null)
                    mediaPlayer.setVolume(1.0f,1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d(TAG, "onAudioFocusChange: pause");
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    controllerCompat.getTransportControls().pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d(TAG, "onAudioFocusChange: stop");
                stopMedia();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d(TAG, "onAudioFocusChange: duck");
                if (mediaPlayer!= null && mediaPlayer.isPlaying())
                    mediaPlayer.setVolume(1.0f,1.0f);
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion: ");
        stopMedia();

        setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
        currState = PlaybackStateCompat.STATE_NONE;

        playbackInfoListener.onPlaybackCompleted();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError: " + "what " + Integer.toString(what) + "extra " + Integer.toString(extra));
        stopMedia();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared: ");
        playMedia();
        Audio currAudio = getActiveFragmentSong();
        playbackInfoListener.onPlaybackStart(currAudio.getSongDuration());
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
       
    }

    /**************************     MediaPlayer Controller Methods         *******************************/

    private void playMedia () {
        Log.d(TAG, "playMedia: ");
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopMedia () {
        if (mediaPlayer != null) {
            mediaSessionCompat.setActive(false);
            mediaPlayer.stop();
            setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
            mediaPlayer.release();
            mediaPlayer = null;
        }
        removeNotification();
    }

    public void resumeMedia () {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (resumePos != -1)
                mediaPlayer.seekTo(resumePos);
            else
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition());
            resumePos = -1;
        }
    }

    public void pauseMedia () {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void seekTo (int position) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying())
                mediaPlayer.seekTo(position);
            else
                resumePos = position;
        }
    }

    private void initMediaPlayer () {
        Log.d(TAG, "initMediaPlayer: ");
        if (mediaPlayer == null)
            mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnErrorListener(this);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        //if OnlineModeFragment is active
        if (MainActivity.activeFragment == 2) {
            SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
            String token = sharedPreferences.getString("token", "");
            Log.d(TAG, "initMediaPlayer: " + token);
            Map<String, String> params = new HashMap<String, String>();
            String creds = String.format("%s:%s",token,"");
            String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
            params.put("Authorization", auth);

            try {
                url = getActiveFragmentSong().getData();
                Log.d(TAG, "initMediaPlayer: " + url);
                mediaPlayer.setDataSource(this, Uri.parse(url),params);
                initMediaSessionMetadata();
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }
        }
        else {

            try {
                url = getActiveFragmentSong().getData();
                Log.d(TAG, "initMediaPlayer: " + url);
                mediaPlayer.setDataSource(url);
                initMediaSessionMetadata();
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }
        }
    }

    private Audio getActiveFragmentSong() {
        switch (MainActivity.activeFragment) {
            case 0:
                return AudioListHelper.audioListHelper.audioArrayList.get(AudioListHelper.audioListHelper.currIndex);
            case 1:
                return FavouriteListHelper.favouriteListHelper.favouriteList.get(FavouriteListHelper.favouriteListHelper.currIndex);
            case 2:
                return OnlineModeListHelper.onlineModeListHelper.onlineList.get(OnlineModeListHelper.onlineModeListHelper.currIndex);
        }
        return null;
    }

    private void initMediaSession() {
        Log.d(TAG, "initMediaSession: ");

        ComponentName mediaButtonReciever = new ComponentName(getApplicationContext(),MediaButtonReceiver.class);
        //mediaSessionCompat = new MediaSessionCompat(getApplicationContext(),TAG);
        mediaSessionCompat = new MediaSessionCompat(getApplicationContext(),TAG,mediaButtonReciever,null);

        mediaSessionCompat.setCallback(mediaSessionCallback);
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        setSessionToken(mediaSessionCompat.getSessionToken());
        try {
            controllerCompat = new MediaControllerCompat(getApplicationContext(), mediaSessionCompat.getSessionToken());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMediaPlaybackState (int state) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();

        switch (state) {
            case PlaybackStateCompat.STATE_PLAYING:
                playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PAUSE);
                break;

            case PlaybackStateCompat.STATE_PAUSED:
                playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY);
                break;

            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
                break;

            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
                break;

        }
        playbackstateBuilder.setState(state,PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,0);
        mediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private void initMediaSessionMetadata () {
        Log.d(TAG, "initMediaSessionMetadata: ");

        Audio currAudio = getActiveFragmentSong();
        byte [] data;
        Bitmap bitmap;

        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        if (MainActivity.activeFragment != 2) {
            mmr.setDataSource(currAudio.getData());
            data = mmr.getEmbeddedPicture();
            if (data != null)
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            else
                bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }
        else {
            // convert the byte array to a bitmap
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap);
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,currAudio.getTitle());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currAudio.getArtist());
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        mediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    /**************************     Service LifeCycle Methods         *******************************/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");

        handleIntent(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        
        super.onCreate();

        timer = new Timer();
        ((AudioManager)getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(
                this,
                MediaButtonReceiver.class));

        if (!requestAudioFocus()) {
            stopSelf();
        }

        initMediaSession();
        callStateListener();
        registerBecomingNoisyReciever();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //Log.d(TAG, "run: ");
                try {
                    if (mediaPlayer != null && playbackInfoListener != null) {
                        if (resumePos != -1)
                            playbackInfoListener.onPositionChanged(resumePos);
                        else
                            playbackInfoListener.onPositionChanged(mediaPlayer.getCurrentPosition());
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        },0,1000);

        setMediaPlaybackState(PlaybackStateCompat.STATE_NONE);
        currState = PlaybackStateCompat.STATE_NONE;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        
        super.onDestroy();

        stopMedia();
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_NONE);
        }

        mediaSessionCompat.release();
        unregisterReceiver(becomingNoisyReciever);
        removeAudioFocus();
        AudioListHelper.audioListHelper.saveCurrIndex();
        timer.cancel();
        timer.purge();
    }

    /**************************     AudioFocus         *******************************/

    private boolean requestAudioFocus () {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,
                                                    AudioManager.AUDIOFOCUS_GAIN);

        return (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    }

    private void removeAudioFocus () {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) audioManager.abandonAudioFocus(this);
    }

    private Notification builtNotification (int playbackStatus) {
        Log.d(TAG, "builtNotification: ");
        int notificationAction;
        PendingIntent action;

        if (playbackStatus == PlaybackStateCompat.STATE_PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            action = generatePendingIntent(ACTION_PAUSE);
        } else {
            notificationAction = android.R.drawable.ic_media_play;
            action = generatePendingIntent(ACTION_PLAY);
        }

        MediaControllerCompat controller = mediaSessionCompat.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description;
//        if (mediaMetadata != null)
        description = mediaMetadata.getDescription();
        NotificationCompat.Builder notificationBuilder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "com.kapil.musicplayer.service;";
            String channelName = "MediaPlayerService";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            notificationBuilder = new NotificationCompat.Builder(MediaPlayerService.this, NOTIFICATION_CHANNEL_ID);
        }
        else
            notificationBuilder = (NotificationCompat.Builder)  new NotificationCompat.Builder(MediaPlayerService.this);

        notificationBuilder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().
                        setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSessionCompat.getSessionToken())
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_STOP)))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(description.getIconBitmap())
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText(description.getSubtitle())
                .setContentTitle(description.getTitle())
                .setSubText(description.getDescription())
                //launch UI
                .setContentIntent(controller.getSessionActivity())
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(MediaPlayerService.this,PlaybackStateCompat.ACTION_STOP))
                .addAction(new
                        NotificationCompat.Action(android.R.drawable.ic_media_previous, "previous", generatePendingIntent(ACTION_PREVIOUS)))
                .addAction(new
                        NotificationCompat.Action(notificationAction, "pause", action))
                .addAction(new
                        NotificationCompat.Action(android.R.drawable.ic_media_next, "next",
                        generatePendingIntent(ACTION_NEXT)));

        Notification notification = notificationBuilder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForeground(2, notification);
        return notification;
    }

    private PendingIntent generatePendingIntent (String intentAction) {
        Intent intent = new Intent(this,MediaPlayerService.class);
        intent.setAction(intentAction);
        return PendingIntent.getService(getApplicationContext(),1,intent,0);
    }

    private void removeNotification() {
        MediaPlayerService.this.stopForeground(true);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    }

    @Nullable
    @Override
    public MediaBrowserServiceCompat.BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new MediaBrowserServiceCompat.BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }


}
