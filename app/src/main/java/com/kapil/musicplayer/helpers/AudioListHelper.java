package com.kapil.musicplayer.helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.kapil.musicplayer.model.Audio;

import java.util.ArrayList;

/*
 * This class contains members and methods related to loading and storing audioList
 * It follows singleton pattern so both mainActivity and mediaPlayerService access same instance
 * Todo: Use Loaders to load data asynchronously
 */


public class AudioListHelper {

    private static final String TAG = "AudioList";

    public static AudioListHelper audioListHelper = new AudioListHelper();
    public int currIndex;
    public ArrayList<Audio> audioArrayList;
    private Context context;

    private void loadCurrIndex () {

        Log.d(TAG, "loadCurrIndex: ");
        SharedPreferences pref = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        currIndex = pref.getInt("currIndex",0);
    }

    public void saveCurrIndex () {

        Log.d(TAG, "saveCurrIndex: ");
        SharedPreferences pref = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("currIndex",currIndex);
        editor.apply();
    }

    private Bitmap getAlbumImage(String path) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        byte[] data = mmr.getEmbeddedPicture();
        if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
        return null;
    }

    public void loadAudio (Context context) {

        Log.d(TAG, "loadAudio: start loading data");

        this.context = context;
        currIndex = 0;
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE;
        String path = null;

        Cursor cursor = contentResolver.query(uri,null,selection,null,sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            audioArrayList = new ArrayList<>();
            while (cursor.moveToNext()) {
                Audio audio = new Audio();
                audio.setData(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                audio.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                audio.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                audio.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));

                Cursor cursorAlbum = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                        MediaStore.Audio.Albums._ID+ "=?",
                        new String[] {cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))},
                        null);

                if (cursorAlbum != null && cursorAlbum.getCount() > 0 && cursorAlbum.moveToFirst()) {
                    path = cursorAlbum.getString(cursorAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                }

                audio.setAlbumArtUri(path);

                cursorAlbum.close();


                audio.setSongDuration(Integer.parseInt(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))));

                audioArrayList.add(audio);
            }
        }
        cursor.close();

        Log.d(TAG, "loadAudio: loading completed");
        loadCurrIndex();

        new Thread(new Runnable(){
            public void run() {
                for(int i=0;i<audioArrayList.size();i++) {
                    audioArrayList.get(i).setBitmap(getAlbumImage(audioArrayList.get(i).getData()));
                }
            }
        }).start();
    }


}
