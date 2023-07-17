package com.example.imagegallery.utilities;

import android.net.Uri;

public class AlbumCover {
    // Name of the album
    private String albumName;
    // Uri of the last picture taken in the album
    private Uri coverUri;
    // Number of photos in the album
    private int photoCount;

    public AlbumCover(String albumName,Uri coverUri){
        this.albumName = albumName;
        this.coverUri = coverUri;
        photoCount = 1;
    }

    public void increasePhotoCount(){
        photoCount++;
    }

    public String getAlbumName() {
        return albumName;
    }

    public Uri getCoverUri() {
        return coverUri;
    }

    public int getPhotoCount() {
        return photoCount;
    }
}
