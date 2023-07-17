package com.example.imagegallery.utilities;

import android.net.Uri;

import androidx.annotation.NonNull;

public class AlbumImage {
    // Identifier provided by ContentResolver
    private long id;
    // Uri location of the image
    private Uri uri;

    public AlbumImage(long id, Uri uri) {
        this.id = id;
        this.uri = uri;
    }

    public long getId() {
        return id;
    }

    public Uri getUri() {
        return uri;
    }

    @Override @NonNull
    public String toString() {
        return "AlbumImage{" +
                "id=" + id +
                ", uri=" + uri +
                '}';
    }
}
