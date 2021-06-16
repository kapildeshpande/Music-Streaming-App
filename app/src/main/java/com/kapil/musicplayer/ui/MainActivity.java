package com.kapil.musicplayer.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.kapil.musicplayer.R;
import com.kapil.musicplayer.adapters.DbAdapter;
import com.kapil.musicplayer.helpers.AudioListHelper;
import com.kapil.musicplayer.helpers.FavouriteListHelper;
import com.kapil.musicplayer.helpers.OnlineModeListHelper;
import com.kapil.musicplayer.helpers.SetPlaybackInfoListenerHelper;
import com.kapil.musicplayer.model.Audio;
import com.kapil.musicplayer.service.MediaPlayerService;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static int activeFragment = 0;

    //UI elements
    private SlidingUpPanelLayout slidingPaneLayout;
    private TextView displaySong;
    private TextView displayAlbum;
    private ImageView albumImage;

    private SeekBar seekBar;
    private ImageButton next;
    private ImageButton prev;
    private TextView currProgress;
    private TextView maxProgress;
    private ImageView playPause;
    private FloatingActionButton floatingActionButton;
    private ImageView centerImage;
    private LikeButton heartButton;

    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_NONE = 2;
    private int flag = 0;//if music is played first time

    private int currentState;
    private MediaBrowserCompat mediaBrowserCompat;
    private FavouriteFragment favouriteFragment;
    private boolean activityRestarted = false;

    private PlaybackInfoListener playbackListener = new PlaybackListener();

    private MediaBrowserCompat.ConnectionCallback mediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected () {
            super.onConnected();

            try {
                MediaControllerCompat mediaControllerCompat = new MediaControllerCompat(MainActivity.this,mediaBrowserCompat.getSessionToken());
                mediaControllerCompat.registerCallback(mediaControllerCompatCallback);
                MediaControllerCompat.setMediaController(MainActivity.this,mediaControllerCompat);

                currentState = PlaybackStateCompat.STATE_NONE;

                getMediaController().getTransportControls().sendCustomAction("setPlaybackInfoListener",null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
          }

    };

    private MediaControllerCompat.Callback mediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            Log.d(TAG, "onPlaybackStateChanged: " + Integer.toString(state.getState()));

            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }

            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING:
                    currentState = STATE_PLAYING;
                    playPause.setImageResource(R.drawable.pause);
                    floatingActionButton.setImageResource(R.drawable.pause);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    currentState = STATE_PAUSED;
                    playPause.setImageResource(R.drawable.play);
                    floatingActionButton.setImageResource(R.drawable.play);
                    break;
                case PlaybackStateCompat.STATE_NONE:
                    currentState = STATE_NONE;
                    playPause.setImageResource(R.drawable.play);
                    floatingActionButton.setImageResource(R.drawable.play);
                    break;
            }
        }


    };

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    public boolean checkPermissionREAD_EXTERNAL_STORAGE(
            final Context context) {

        Log.d(TAG, "checkPermissionREAD_EXTERNAL_STORAGE: ");

        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        showDialog("External storage", context,
                                Manifest.permission.READ_EXTERNAL_STORAGE);

                } else {
                    ActivityCompat
                            .requestPermissions(
                                    (Activity) context,
                                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }

                return false;

            } else {
                return true;
            }

        } else {
            return true;
        }
    }

    public void showDialog(final String msg, final Context context,
                           final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[] { permission },
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do your stuff

                } else {
                    Toast.makeText(MainActivity.this,  "Permission Denied",
                            Toast.LENGTH_SHORT).show();
                    checkPermissionREAD_EXTERNAL_STORAGE(this);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        Log.d(TAG, "onSaveInstanceState: ");
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState: ");

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");

        super.onDestroy();
        mediaBrowserCompat.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate: ");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        checkPermissionREAD_EXTERNAL_STORAGE(this);

        while (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

        }

        Log.d(TAG, "onCreate: permission granted");
        initialSetup();

//        if (savedInstanceState == null) {
//            activityRestarted = false;
//        } else {
//            activityRestarted = true;
//        }

        SetPlaybackInfoListenerHelper.controller.setPlaybackInfoListener(playbackListener);

        mediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, MediaPlayerService.class),
                mediaBrowserCompatConnectionCallback, getIntent().getExtras());

        mediaBrowserCompat.connect();

        currentState = PlaybackStateCompat.STATE_NONE;

        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        Log.d(TAG, "onProgressChanged: seekBar progress changed");
                        getMediaController().getTransportControls().seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    private void initialSetup () {
        if (!activityRestarted) {
            AudioListHelper.audioListHelper.loadAudio(MainActivity.this);
            FavouriteListHelper.favouriteListHelper.loadFavouriteList(MainActivity.this);
            OnlineModeListHelper.onlineModeListHelper.loadAudio(MainActivity.this);
            //HistoryModeListHelper.historyModeListHelper.loadAudio(MainActivity.this) => call by OnlistModeListHelper after loading its audio
        }


        SectionPagerAdapter sectionPagerAdapter = new SectionPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(sectionPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                activeFragment = tab.getPosition();
            }

            @Override
            public void onTabSelected(TabLayout.Tab tab){
                activeFragment = tab.getPosition();
            }
        });

        initMusicSnippet();

        setLikedButton();
    }

    private void onClickSongsFragmentRecyclerView (int position) {
        Log.d(TAG, "onClickRecyclerView: ");

        AudioListHelper.audioListHelper.currIndex = position;

        AudioListHelper.audioListHelper.saveCurrIndex();

        getMediaController().getTransportControls().sendCustomAction("play",null);
        setLikedButton();

    }

    private void onClickFavouriteFragmentRecyclerView (int position) {
        Log.d(TAG, "onClickFavouriteFragmentRecyclerView: ");

        FavouriteListHelper.favouriteListHelper.currIndex = position;
        FavouriteListHelper.favouriteListHelper.saveCurrIndex();

        getMediaController().getTransportControls().sendCustomAction("play",null);
        setLikedButton();

    }

    private void onClickOnlineModeFragmentRecyclerView (int position) {
        Log.d(TAG, "onClickOnlineModeFragmentRecyclerView: ");

        OnlineModeListHelper.onlineModeListHelper.currIndex = position;
        OnlineModeListHelper.onlineModeListHelper.saveCurrIndex();

        Log.d(TAG, "onClickOnlineModeFragmentRecyclerView: " + Integer.toString(position));

        getMediaController().getTransportControls().sendCustomAction("play",null);
        setLikedButton();

    }

    private void initMusicSnippet () {
        Log.d(TAG, "initMusicSnippet: ");

        Audio activeAudio = getActiveFragmentAudio();

        String albumArtUri = activeAudio.getAlbumArtUri();
        albumImage.setAdjustViewBounds(true);
        centerImage.setAdjustViewBounds(true);

        if (albumArtUri == null || albumArtUri.equals("")) {
            albumImage.setImageDrawable(getDrawable(R.mipmap.ic_launcher));
            centerImage.setImageDrawable(getDrawable(R.mipmap.ic_launcher));
        } else {
            Glide.with(MainActivity.this).asBitmap().load(activeAudio.getAlbumArtUri())
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                    .into(albumImage);

            Glide.with(MainActivity.this).asBitmap().load(activeAudio.getAlbumArtUri())
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                    .into(centerImage);
        }

        displaySong.setText(activeAudio.getTitle());
        displayAlbum.setText(activeAudio.getAlbum());
    }


    private void init() {

        Log.d(TAG, "init: initialising UI elements");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Main layout UI elements
        playPause = findViewById(R.id.playPauseButton);
        slidingPaneLayout = findViewById(R.id.activity_main);

        //Sliding Layout UI elements
        heartButton = findViewById(R.id.star_button);
        seekBar = findViewById(R.id.seekBar);
        next = findViewById(R.id.nextSong);
        prev = findViewById(R.id.prevSong);
        currProgress = findViewById(R.id.currProgress);
        maxProgress = findViewById(R.id.maxProgress);
        displaySong = findViewById(R.id.song_name);
        displayAlbum = findViewById(R.id.album_name);
        albumImage = findViewById(R.id.album_image);
        floatingActionButton = findViewById(R.id.playPause);
        centerImage = findViewById(R.id.center_image);

        slidingPaneLayout.setDragView(R.id.sliding_area);

        slidingPaneLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {

                if (slidingPaneLayout.getPanelState() == PanelState.COLLAPSED) {
                    playPause.setVisibility(View.VISIBLE);
                    heartButton.setVisibility(View.GONE);
                } else {
                    playPause.setVisibility(View.GONE);
                    if (activeFragment == 0)
                        heartButton.setVisibility(View.VISIBLE);
                }

            }
        });

        heartButton.setOnLikeListener(new OnLikeListener() {

            @Override
            public void liked(LikeButton likeButton) {

                if (activeFragment == 1 || activeFragment == 2)
                    return;

                Audio activeAudio = AudioListHelper.audioListHelper.audioArrayList.get(AudioListHelper.audioListHelper.currIndex);
                if (activeAudio != null && DbAdapter.getDbAdapterInstance(MainActivity.this).insert(activeAudio) ) {
                    FavouriteListHelper.favouriteListHelper.favouriteList.add(activeAudio);
                    favouriteFragment.dataUpdated();
                    Toast.makeText(MainActivity.this, "Song added to favourites", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Operation Failed", Toast.LENGTH_SHORT).show();
                    heartButton.setLiked(false);
                }
            }

            @Override
            public void unLiked(LikeButton likeButton) {

                if (activeFragment == 1  || activeFragment == 2)
                    return;

                Audio activeAudio = AudioListHelper.audioListHelper.audioArrayList.get(AudioListHelper.audioListHelper.currIndex);

                if (DbAdapter.getDbAdapterInstance(MainActivity.this).delete(activeAudio.getTitle())) {
                    FavouriteListHelper.favouriteListHelper.loadFavouriteList(MainActivity.this);
                    favouriteFragment.dataUpdated();
                    Toast.makeText(MainActivity.this, "Song removed from favourites", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Operation Failed", Toast.LENGTH_SHORT).show();
                    heartButton.setLiked(true);
                }
            }
        });
    }

    private Audio getActiveFragmentAudio () {
        switch (activeFragment) {
            case 0:
                return AudioListHelper.audioListHelper.audioArrayList.get(AudioListHelper.audioListHelper.currIndex);
            case 1:
                return FavouriteListHelper.favouriteListHelper.favouriteList.get(FavouriteListHelper.favouriteListHelper.currIndex);
            case 2:
                return OnlineModeListHelper.onlineModeListHelper.onlineList.get(OnlineModeListHelper.onlineModeListHelper.currIndex);

        }
        return null;
    }

    private void setLikedButton() {

        if (activeFragment == 1 || activeFragment == 2)
            return;

        Audio activeAudio = AudioListHelper.audioListHelper.audioArrayList.get(AudioListHelper.audioListHelper.currIndex);
        Boolean condi = DbAdapter.getDbAdapterInstance(MainActivity.this).searchIfPresent(activeAudio.getTitle());
        Log.d(TAG, "setLikedButton: " + Boolean.toString(condi));

        heartButton.setLiked(condi);
    }

    public void onClickNext (View v) {
        Log.d(TAG, "onClickNext: ");

        getMediaController().getTransportControls().skipToNext();
        setLikedButton();
    }

    public void onClickPrev (View v) {
        Log.d(TAG, "onClickPrev: ");

        getMediaController().getTransportControls().skipToPrevious();
        setLikedButton();
    }

    public void onClickPlayPause (View v) {

        Log.d(TAG, "onClickPlayPause: " + Integer.toString(currentState));

        if (flag == 0) {
            getMediaController().getTransportControls().play();
            flag = 1;
            return;
        }

        if (currentState == STATE_PAUSED) {
            getMediaController().getTransportControls().sendCustomAction("resume", null);
        } else if (currentState == STATE_NONE) {
            getMediaController().getTransportControls().play();
        } else {
            getMediaController().getTransportControls().pause();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");

        if (slidingPaneLayout != null &&
                slidingPaneLayout.getPanelState() == PanelState.EXPANDED ||
                slidingPaneLayout.getPanelState() == PanelState.ANCHORED) {
            slidingPaneLayout.setPanelState(PanelState.COLLAPSED);
        } else {
            this.moveTaskToBack(true);
        }
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onPlaybackCompleted() {
            Log.d(TAG, "onPlaybackCompleted: ");

            onClickNext(null);
        }

        @Override
        public void onPlaybackStart(final int maxDuration) {
            Log.d(TAG, "onPlaybackStart: ");

            initMusicSnippet();

            if (seekBar != null) {
                seekBar.setProgress(0);
                seekBar.setMax(maxDuration);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (maxProgress != null)
                        maxProgress.setText(convertMilliToTime(maxDuration));
                    if (currProgress != null)
                        currProgress.setText("00:00");
                }
            });
        }

        private String convertMilliToTime (int milli) {
            int seconds = (milli / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            String sec;
            if (seconds > 9)
                sec = Integer.toString(seconds);
            else
                sec = "0" + Integer.toString(seconds);

            String min;
            if (minutes > 9)
                min = Integer.toString(minutes);
            else
                min = "0" + Integer.toString(minutes);

            return (min + ":" + sec);
        }

        @Override
        public void onPositionChanged(final int currProgres) {
            //Log.d(TAG, "onPositionChanged: ");

            if (seekBar != null)
                seekBar.setProgress(currProgres);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currProgress != null)
                    currProgress.setText(convertMilliToTime(currProgres));
                }
            });
        }

    }

    public class SectionPagerAdapter extends FragmentPagerAdapter {

        SectionPagerAdapter (FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    SongsFragment songsFragment = new SongsFragment();
                    songsFragment.setListener(new SongsFragment.ListenerCallback() {
                        @Override
                        public void onClick(int position) {
                            onClickSongsFragmentRecyclerView(position);
                        }
                    });
                    return songsFragment;
                case 1:
                    favouriteFragment = new FavouriteFragment();
                    favouriteFragment.setListener(new FavouriteFragment.songFragmentCallback() {
                        @Override
                        public void onClick(int position) {
                            onClickFavouriteFragmentRecyclerView(position);
                        }
                    });
                    return favouriteFragment;
                case 2:
                    OnlineModeFragment onlineModeFragment = new OnlineModeFragment();
                    onlineModeFragment.setListener(new OnlineModeFragment.onlineModeFragmentCallback() {
                        @Override
                        public void onClick(int position) {
                            onClickOnlineModeFragmentRecyclerView(position);
                        }
                    });
                    return onlineModeFragment;
                case 3:
                    return new HistoryFragment();
            }

            return null;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Songs";
                case 1:
                    return "Favourite";
                case 2:
                    return "Online Mode";
                case 3:
                    return "History";
            }

            return null;
        }
    }

}
