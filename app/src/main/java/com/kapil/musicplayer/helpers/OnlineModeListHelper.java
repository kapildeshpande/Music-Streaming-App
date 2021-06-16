package com.kapil.musicplayer.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kapil.musicplayer.adapters.DbAdapter;
import com.kapil.musicplayer.model.Audio;
import com.kapil.musicplayer.ui.OnlineModeFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.util.List;

public class OnlineModeListHelper {

    private static final String TAG = "OnlineModeListHelper";
    public static OnlineModeListHelper onlineModeListHelper = new OnlineModeListHelper();
    public int currIndex = 0;
    public ArrayList<Audio> onlineList = new ArrayList<>();
    public ArrayList<Pair<Integer,Audio> > idToAudio = new ArrayList<>();
    private Context context;
    private String url = "https://music-streaming-service.herokuapp.com/music/display_all";

    //callback to set media information
    public interface SetCallback {

    }

    private void loadCurrIndex () {

        Log.d(TAG, "loadCurrIndex: ");
        SharedPreferences pref = context.getSharedPreferences("OnlineModePref", Context.MODE_PRIVATE);
        currIndex = pref.getInt("currIndex",0);
    }

    public void saveCurrIndex () {

        Log.d(TAG, "saveCurrIndex: ");
        SharedPreferences pref = context.getSharedPreferences("OnlineModePref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("currIndex",currIndex);
        editor.apply();
    }

    private void callAPI () {
        Log.d(TAG, "callAPI: ");
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseJSON(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: ");
                    }
                });
        requestQueue.add(stringRequest);
    }

    private void parseJSON(String response) {
        Log.d(TAG, "parseJSON: ");
        try {
//            JSONObject objResponse = new JSONObject(response);
            JSONArray arrHeadlines = new JSONArray(response);

//            for(int i = 0; i < arrHeadlines.length(); i++)
//                Log.d(TAG, "parseJSON: " +(arrHeadlines.getJSONObject(i).getString("title")));

            String prefixUrl = "https://music-streaming-service.herokuapp.com/music/";
            for (int i=0;i<arrHeadlines.length();i++) {
                Audio audio = new Audio();
                JSONObject obj = arrHeadlines.getJSONObject(i);

                audio.setTitle(obj.getString("title"));
                audio.setAlbum(obj.getString("album"));
                audio.setArtist(obj.getString("artist"));
                audio.setData(prefixUrl + obj.getString("title"));
                audio.setSongDuration(obj.getInt("duration"));
                int id = obj.getInt("id");
                idToAudio.add(new Pair<Integer, Audio>(id, audio));
                onlineModeListHelper.onlineList.add(audio);
            }

            for (int i=0;i<onlineList.size();i++) {
                Log.d(TAG,onlineList.get(i).getTitle());
            }

            HistoryListHelper.historyModeListHelper.loadAudio(context);

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "parseJSON: error");
        }
    }

    //load audio from server
    public void loadAudio (Context context) {
        Log.d(TAG, "loadAudio: ");
        this.context = context;
        callAPI();
    }
}
