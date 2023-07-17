package com.example.imagegallery.utilities;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.example.imagegallery.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoManager {
    // LOG String
    private static final String TAG = PhotoManager.class.getSimpleName();
    // Caller Activity for reference
    private WeakReference<Activity> activityReference;

    public PhotoManager(@NonNull Activity caller){
        activityReference = new WeakReference<>(caller);
    }

    /*
     * Create an image file with a collision resistant name and return the Uri
     * If the location parameter is true it will try to retrieve the location
     * and store it into the metadata
     */
    public Uri createImageFile(@NonNull String albumName,@NonNull String fileName){
        Log.i(TAG,"Creating temp file to store a new photo.");
        // Path: Pictures/ImageGallery/Album
        // Filename = IMG_yyyyMMdd_HHmmss.jpg
        Activity caller = activityReference.get();
        String path = File.separator+caller.getString(R.string.app_name)+File.separator+albumName;
        // Content Resolver
        ContentResolver resolver = caller.getApplicationContext().getContentResolver();
        // Create Content Values to insert
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, fileName);
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        // Difference between SDK INT >= 29 and below
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+path);
            contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        }else{
            String albumsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + path;
            File albumFolder = new File(albumsPath);
            if(!albumFolder.exists() && albumFolder.mkdirs()) Log.i(TAG,"SDK<29 Directory "+path+" created.");
            contentValues.put(MediaStore.Images.Media.DATA,albumFolder.getAbsolutePath()+File.separator+fileName+".jpg");
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    public Uri createImageFile(@NonNull String albumName){
        return createImageFile(albumName,getCollisionResistantFileName());
    }

    /*
     * Delete an image file starting from the Uri
     */
    public void deleteImageFile(@NonNull Uri uri){
        Log.i(TAG,"Deleting image "+uri.toString());
        Activity caller = activityReference.get();
        ContentResolver resolver = caller.getApplicationContext().getContentResolver();
        resolver.delete(uri,null,null);
    }

    /*
     * Move an image file to another album location
     */
    public void moveFile(@NonNull Uri uri,@NonNull String albumName){
        Activity caller = activityReference.get();
        String path = File.separator+caller.getString(R.string.app_name)+File.separator+albumName;
        // Content Resolver
        ContentResolver resolver = caller.getApplicationContext().getContentResolver();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            // Create Content Values to update
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+path);
            resolver.update(uri,contentValues,null,null);
            Log.i(TAG,"SDK>=29: Moving image "+uri.toString()+" to album "+albumName);
        }else{
            // Retrieve Filename to update DATA COLUMN
            try(Cursor cursor = resolver.query(
                    uri,new String[]{MediaStore.Images.Media.DISPLAY_NAME},null,null,null)) {
                if(cursor!= null && cursor.moveToNext()){
                    String fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                    Uri newUri = createImageFile(albumName,fileName);
                    try (InputStream is = caller.getContentResolver().openInputStream(uri);
                         OutputStream os = caller.getContentResolver().openOutputStream(newUri)) {
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if(bitmap != null) bitmap.compress(Bitmap.CompressFormat.JPEG,100,os);
                    } catch (IOException e) {
                        Log.i(TAG,"SDK<29: Cannot compress bitmap on newUri OutputStream.");
                    }
                    copyLocationInformation(uri,newUri);
                    deleteImageFile(uri);
                }
            }
        }

    }

    /*
     * Delete an album, and all the image inside it.
     */
    public void deleteAlbum(@NonNull String albumName){
        Activity caller = activityReference.get();
        ContentResolver resolver = caller.getApplicationContext().getContentResolver();
        String path = File.separator+caller.getString(R.string.app_name)+File.separator+albumName+File.separator;
        Uri collection;
        String selection;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        }else{
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            selection = MediaStore.Images.Media.DATA + " LIKE ?";
        }
        String[] selectionArgs = new String[]{"%" + path + "%"};
        int num = resolver.delete(collection,selection,selectionArgs);
        Log.i(TAG,"Deleting album "+albumName+" with "+num+" files.");
    }

    /*
     * Add Location Information to an image.
     */
    public void addLocationInformationToFile(@NonNull Uri uri,@NonNull Location location){
        Activity caller = activityReference.get();
        try (ParcelFileDescriptor pfd = caller.getContentResolver().openFileDescriptor(uri, "rw")) {
            if (pfd != null) {
                ExifInterface exif = new ExifInterface(pfd.getFileDescriptor());
                exif.setGpsInfo(location);
                exif.saveAttributes();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
     * Copy Location Information from a image to another one.
     */
    public void copyLocationInformation(@NonNull Uri oldUri,@NonNull Uri newUri){
        Activity caller = activityReference.get();
        try (ParcelFileDescriptor oldPfd = caller.getContentResolver().openFileDescriptor(oldUri, "r");
            ParcelFileDescriptor newPfd = caller.getContentResolver().openFileDescriptor(newUri, "rw")) {
            if (oldPfd != null && newPfd != null) {
                ExifInterface oldExif = new ExifInterface(oldPfd.getFileDescriptor());
                ExifInterface newExif = new ExifInterface(newPfd.getFileDescriptor());
                double[] latLong = oldExif.getLatLong();
                if(latLong != null){
                    newExif.setLatLong(latLong[0],latLong[1]);
                    newExif.saveAttributes();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
     * Return a square cropped thumbnail of an image
     */
    public Bitmap getThumbnailSquare(@NonNull Uri uri, int thumbnailSize){
        Activity caller = activityReference.get();
        Bitmap thumbnail;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                thumbnail = caller.getContentResolver().loadThumbnail(
                        uri, new Size(thumbnailSize, thumbnailSize), null);
            } catch (IOException ex) {
                Log.d(TAG,"SDK>=29 getThumbnailSquare contentResolver loadThumbnail",ex);
                return null;
            }
        }else{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            // Compute inSampleSize
            try(InputStream is = caller.getContentResolver().openInputStream(uri)){
                BitmapFactory.decodeStream(is,null,options);
                float widthScale = (float)options.outWidth/thumbnailSize;
                float heightScale = (float)options.outHeight/thumbnailSize;
                float scale = Math.min(widthScale, heightScale);
                int sampleSize = 1;
                while (sampleSize < scale) sampleSize *= 2;
                options.inJustDecodeBounds = false;
                options.inSampleSize = sampleSize!=1?sampleSize/2:sampleSize;
            }catch (IOException ex) {
                Log.d(TAG,"SDK<29 getThumbnailSquare calc inSampleSize",ex);
                return null;
            }
            // Get ImageScaled
            try(InputStream is = caller.getContentResolver().openInputStream(uri)){
                thumbnail = BitmapFactory.decodeStream(is,null, options);
            }catch (IOException ex) {
                Log.d(TAG,"SDK<29 getThumbnailSquare thumbnail decodeStream",ex);
                return null;
            }
        }
        // Square Crop
        if(thumbnail != null){
            int size = Math.min(thumbnail.getWidth(), thumbnail.getHeight());
            int x = (thumbnail.getWidth() / 2) - (size / 2);
            int y = (thumbnail.getHeight() / 2) - (size / 2);
            return Bitmap.createBitmap(thumbnail, x, y, size, size);
        }
        return null;
    }

    /*
     * Return a square cropped thumbnail of an image with a the shape of a circle.
     */
    public Bitmap getCircledBitmap(@NonNull Uri uri,int thumbnailSize) {
        Bitmap bitmap = getThumbnailSquare(uri,thumbnailSize);
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle((float)bitmap.getWidth() / 2, (float)bitmap.getHeight() / 2, (float)bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /*
     * Return a collision resistant filename for new photos.
     */
    private String getCollisionResistantFileName(){
        Activity caller = activityReference.get();
        return "IMG_" + new SimpleDateFormat(caller.getString(R.string.dateformat_pattern), Locale.getDefault()).format(new Date());
    }
}
