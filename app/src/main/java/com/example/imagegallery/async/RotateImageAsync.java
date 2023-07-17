package com.example.imagegallery.async;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;

import com.example.imagegallery.ShowImageActivity;
import com.example.imagegallery.utilities.QueryManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

public class RotateImageAsync extends AsyncTask<Uri,Void, Boolean> {
    // Reference to the calling activity
    private WeakReference<ShowImageActivity> activityReference;
    // Rotation
    private float angle;

    public RotateImageAsync(ShowImageActivity activity,float angle){
        activityReference = new WeakReference<>(activity);
        this.angle = angle;
    }

    @Override
    protected Boolean doInBackground(Uri... uris) {
        // Check if the external storage is readable
        if(!QueryManager.isExternalStorageWritable()){
            return false;
        }
        for (Uri uri : uris) {
            ShowImageActivity activity = activityReference.get();
            if (activity != null) {
                Bitmap bitmap;
                try (InputStream is = activity.getContentResolver().openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(is);
                } catch (IOException e) { return false; }
                // Rotate
                Matrix matrix = new Matrix();
                matrix.setRotate(angle);
                Bitmap rotated = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,false);
                // Recycle memory
                bitmap.recycle();
                // Write on file
                try (OutputStream os = activity.getContentResolver().openOutputStream(uri)) {
                    rotated.compress(Bitmap.CompressFormat.JPEG,100,os);
                } catch (IOException e) { return false; }
                // Recycle memory
                rotated.recycle();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        ShowImageActivity activity = activityReference.get();
        if (activity != null)
            activity.postRotationTask(aBoolean);
    }
}
