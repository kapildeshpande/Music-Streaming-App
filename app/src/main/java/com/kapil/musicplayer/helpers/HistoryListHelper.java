package com.kapil.musicplayer.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kapil.musicplayer.model.Audio;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HistoryListHelper {
    private static final String TAG = "HistoryListHelper";
    public static HistoryListHelper historyModeListHelper = new HistoryListHelper();
    public ArrayList<Audio> historyList = new ArrayList<>();
    private Context context;
    private String url = "https://music-streaming-service.herokuapp.com/user/history";

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
                        Log.d(TAG, "onErrorResponse: " + error.getMessage());
                    }
                })  {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                SharedPreferences sharedPreferences = context.getSharedPreferences("MySharedPref",context.MODE_PRIVATE);
                String token = sharedPreferences.getString("token", "");
                Map<String, String>  params = new HashMap<String, String>();
                String creds = String.format("%s:%s",token,"");
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                params.put("Authorization", auth);
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    private void parseJSON(String response) {
        Log.d(TAG, "parseJSON: ");
        try {
            JSONArray arrHeadlines = new JSONArray(response);

            for (int i=0;i<arrHeadlines.length();i++) {
                JSONObject obj = arrHeadlines.getJSONObject(i);
                int id = obj.getInt("music_id");
//                obj.getString("created_at");

                for (Pair<Integer, Audio> p : OnlineModeListHelper.onlineModeListHelper.idToAudio) {
                    if (p.first == id) {
                        historyModeListHelper.historyList.add(p.second);
                        break;
                    }
                }
            }

            for (int i=0;i<historyList.size();i++) {
                Log.d(TAG,historyList.get(i).getTitle());
            }

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
