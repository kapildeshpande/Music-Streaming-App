package com.kapil.musicplayer.model;

import android.graphics.Bitmap;

public class Audio {

    private String data;
    private String title;
    private String album;
    private String artist;
    private int songDuration;
    private String albumArtUri;
    private Bitmap bitmap;

    public Audio(String data, String title, String album, String artist,int songDuration,String albumArtUri) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.songDuration = songDuration;
        this.albumArtUri = albumArtUri;
    }

    public Audio () {}


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap data) {
        this.bitmap = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setSongDuration(int songDuration) { this.songDuration = songDuration; }

    public int getSongDuration() { return songDuration; }

    public String getAlbumArtUri() {
        return albumArtUri;
    }

    public void setAlbumArtUri(String albumArtUri) {
        this.albumArtUri = albumArtUri;
    }
}