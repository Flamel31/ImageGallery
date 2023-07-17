package com.example.imagegallery.async;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.example.imagegallery.AlbumActivity;
import com.example.imagegallery.R;
import com.example.imagegallery.utilities.PhotoManager;

import java.lang.ref.WeakReference;

public class DeleteImagesAsync extends AsyncTask<Uri,Integer,Void> {
    // Reference to the calling activity
    private WeakReference<AlbumActivity> activityReference;
    // PhotoManager
    private PhotoManager photoManager;

    public DeleteImagesAsync(@NonNull AlbumActivity activity) {
        activityReference = new WeakReference<>(activity);
        photoManager = new PhotoManager(activity);
    }

    @Override
    protected Void doInBackground(Uri... uris) {
        int count = uris.length;
        for(int i=0;i<count;i++){
            photoManager.deleteImageFile(uris[i]);
            publishProgress((int) ((i / (float) count) * 100));
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        AlbumActivity activity = activityReference.get();
        if(activity != null)
            activity.progressUpdate(values[0]);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        AlbumActivity activity = activityReference.get();
        if(activity != null)
            activity.endSelectionMode(activity.getString(R.string.images_deleted));
        super.onPostExecute(aVoid);
    }
}
