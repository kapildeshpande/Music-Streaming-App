package com.kapil.musicplayer.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.kapil.musicplayer.adapters.DbAdapter;
import com.kapil.musicplayer.model.Audio;

import java.util.ArrayList;
import java.util.HashSet;

public class FavouriteListHelper {

    private static final String TAG = "FavouriteListHelper";
    public static FavouriteListHelper favouriteListHelper = new FavouriteListHelper();
    public int currIndex = 0;
    public ArrayList<Audio> favouriteList;
    private Context context;

    private void loadCurrIndex () {

        Log.d(TAG, "loadCurrIndex: ");
        SharedPreferences pref = context.getSharedPreferences("FavouritePref", Context.MODE_PRIVATE);
        currIndex = pref.getInt("currIndex",0);
    }

    public void saveCurrIndex () {

        Log.d(TAG, "saveCurrIndex: ");
        SharedPreferences pref = context.getSharedPreferences("FavouritePref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("currIndex",currIndex);
        editor.apply();
    }

    public void loadFavouriteList(Context context) {
        Log.d(TAG, "loadFavouriteList: ");
        this.context = context;
        favouriteList = DbAdapter.getDbAdapterInstance(context).getFavouriteList();
    }
}
