package com.kapil.musicplayer.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kapil.musicplayer.R;
import com.kapil.musicplayer.adapters.HomePageAdapter;
import com.kapil.musicplayer.helpers.HistoryListHelper;
import com.kapil.musicplayer.helpers.OnlineModeListHelper;

public class HistoryFragment extends Fragment {

    public HistoryFragment () {

    }

    private static final String TAG = "HistoryFragment";

    private RecyclerView recyclerView;
    private OnlineModeFragment.onlineModeFragmentCallback onlineModeFragmentCallback;
    private HomePageAdapter homePageAdapter;
    private TextView preText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        preText = (TextView) v.findViewById(R.id.preText);


        if (HistoryListHelper.historyModeListHelper.historyList != null &&  HistoryListHelper.historyModeListHelper.historyList.size() != 0) {
            preText.setVisibility(View.GONE);
        }

        if (HistoryListHelper.historyModeListHelper.historyList != null)
            initRecyclerView(v);
        return v;
    }

    private void initRecyclerView (View view) {
        Log.d(TAG, "initRecyclerView: ");

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        homePageAdapter = new HomePageAdapter(getContext(),HistoryListHelper.historyModeListHelper.historyList);
        recyclerView.setAdapter(homePageAdapter);

        DividerItemDecoration itemDecorator = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.divider));
        recyclerView.addItemDecoration(itemDecorator);
    }

}