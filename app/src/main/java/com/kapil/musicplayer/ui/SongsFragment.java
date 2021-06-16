package com.kapil.musicplayer.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kapil.musicplayer.R;
import com.kapil.musicplayer.adapters.HomePageAdapter;
import com.kapil.musicplayer.helpers.AudioListHelper;


/**
 * A simple {@link Fragment} subclass.
 */
public class SongsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ListenerCallback listenerCallback;

    interface ListenerCallback {
        void onClick(int position);
    }

    public void setListener (ListenerCallback listener) {
        this.listenerCallback = listener;
    }

    public SongsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_songs, container, false);
        initRecyclerView(view);
        return view;
    }

    private void initRecyclerView (View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        HomePageAdapter homePageAdapter = new HomePageAdapter(getContext(), AudioListHelper.audioListHelper.audioArrayList);
        recyclerView.setAdapter(homePageAdapter);

        DividerItemDecoration itemDecorator = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.divider));
        recyclerView.addItemDecoration(itemDecorator);

        homePageAdapter.setListener(new HomePageAdapter.Listener() {
            @Override
            public void onClick(int position) {
                if (listenerCallback != null) {
                    listenerCallback.onClick(position);
                }
            }
        });
    }

}
