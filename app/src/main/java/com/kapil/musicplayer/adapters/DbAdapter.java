package com.kapil.musicplayer.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.kapil.musicplayer.model.Audio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DbAdapter {

    private static final String TAG = "DbAdapter";

    private static final String DB_NAME = "favourite_audioList.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_NAME = "favourite_audio_table";
    private static final String COLUMN_title = "audio_title";
    private static final String COLUMN_data = "audio_data";
    private static final String COLUMN_album = "audio_album";
    private static final String COLUMN_artist = "audio_artist";
    private static final String COLUMN_songDuration = "audio_songDuration";
    private static final String COLUMN_albumArtURI = "audio_albumArtURI";

    private static String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
            "(" + COLUMN_title + " TEXT PRIMARY KEY UNIQUE NOT NULL, "
            + COLUMN_album + " TEXT, "
            + COLUMN_data + " TEXT UNIQUE, "
            + COLUMN_artist + " TEXT, "
            + COLUMN_albumArtURI + " TEXT, "
            + COLUMN_songDuration + " TEXT"
            + ")";

    private Context context;
    private SQLiteDatabase sqLiteDatabase;
    private static DbAdapter dbAdapter;

    private DbAdapter (Context context) {
        this.context = context;
        sqLiteDatabase = new FavouriteAudioListDBHelper(
                this.context,DB_NAME,null,DB_VERSION).getWritableDatabase();
    }

    public  static DbAdapter getDbAdapterInstance(Context context){
        if(dbAdapter==null){
            dbAdapter=new DbAdapter(context);
        }
        return dbAdapter;
    }

    public boolean insert (String title,String data,
                           String artist,String album,
                           String songDuration,String albumArtUri) {
        Log.d(TAG, "insert: ");

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_album,album);
        contentValues.put(COLUMN_artist,artist);
        contentValues.put(COLUMN_albumArtURI,albumArtUri);
        contentValues.put(COLUMN_songDuration,songDuration);
        contentValues.put(COLUMN_title,title);
        contentValues.put(COLUMN_data,data);

        return (sqLiteDatabase.insert(TABLE_NAME,null,contentValues) > 0);
    }

    public boolean insert (Audio audio) {
        Log.d(TAG, "insert: ");

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_album,audio.getAlbum());
        contentValues.put(COLUMN_artist,audio.getArtist());
        contentValues.put(COLUMN_albumArtURI,audio.getAlbumArtUri());
        contentValues.put(COLUMN_songDuration,Integer.toString(audio.getSongDuration()));
        contentValues.put(COLUMN_title,audio.getTitle());
        contentValues.put(COLUMN_data,audio.getData());

        return (sqLiteDatabase.insert(TABLE_NAME,null,contentValues) > 0);
    }

    public boolean delete (String titleName) {
        Log.d(TAG, "delete: ");

        String selection = COLUMN_title + " =?";
        String[] selectionArgs = { titleName };

        return sqLiteDatabase.delete(TABLE_NAME,selection,selectionArgs)>0;
    }

    public ArrayList<Audio> getFavouriteList () {
        Log.d(TAG, "getFavouriteList: ");
        ArrayList<Audio> favouriteList = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.query(TABLE_NAME,
                new String[] {COLUMN_data,COLUMN_title,COLUMN_album,COLUMN_artist,COLUMN_songDuration,COLUMN_albumArtURI},
                null,null,null,null,null,null);

        if(cursor!=null &cursor.getCount()>0){
            while(cursor.moveToNext()){
                Log.d(TAG, "getFavouriteList: ");

                Audio audio=new Audio(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        Integer.parseInt(cursor.getString(4)),
                        cursor.getString(5));
                favouriteList.add(audio);

            }
        }
        cursor.close();
        return favouriteList;
    }

    public boolean searchIfPresent (String searchItem) {

        String[] columns = { COLUMN_title };
        String selection = COLUMN_title + " =?";
        String[] selectionArgs = { searchItem };
        String limit = "1";

        Cursor cursor = sqLiteDatabase.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null, limit);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    private static class FavouriteAudioListDBHelper extends SQLiteOpenHelper {


        public FavouriteAudioListDBHelper (Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int dbVersion) {
            super(context,databaseName,factory,dbVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "onCreate: ");
            db.execSQL(CREATE_TABLE);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "onUpgrade: ");
        }
    }
}