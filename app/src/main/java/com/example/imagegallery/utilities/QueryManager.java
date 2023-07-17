package com.example.imagegallery.utilities;

import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import com.example.imagegallery.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class QueryManager {
    // LOG String
    private static final String TAG = QueryManager.class.getSimpleName();
    // Caller Activity for reference
    private WeakReference<Activity> activityReference;

    public QueryManager(@NonNull Activity caller) {
        activityReference = new WeakReference<>(caller);
    }

    /*
     * This method retrieve all the albums in the gallery and return: the last
     * photo taken (to use as cover), the album name and the number of photos
     * in the album.
     */
    public Collection<AlbumCover> queryAlbumCovers() {
        Activity caller = activityReference.get();
        Uri collection;
        String[] projection;
        String selection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.RELATIVE_PATH};
            selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
            selection = MediaStore.Images.Media.DATA + " LIKE ?";
        }
        String path = File.separator+caller.getString(R.string.app_name)+File.separator;
        // Query all images inside the ImageGallery folder
        String[] selectionArgs = new String[]{"%" + path + "%"};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
        // Keep the uri of the last photo taken on each album
        try (Cursor cursor = Objects.requireNonNull(caller.getApplicationContext().getContentResolver().query(
                collection, projection, selection, selectionArgs, sortOrder)
        )) {
            HashMap<String, AlbumCover> albumCovers = new HashMap<>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                String data = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q?
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)):
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                String[] list = data.split(File.separator);
                String album = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q?
                        list[list.length - 1] : list[list.length - 2];
                AlbumCover ac = albumCovers.get(album);
                if (ac != null)
                    ac.increasePhotoCount();
                else
                    albumCovers.put(album, new AlbumCover(album, ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)));
            }
            return albumCovers.values();
        }
    }

    /*
     * This method retrieve all the photos available in a given album and return:
     * the uri of the image (used to build a thumbnail) and the id of the image.
     */
    public Collection<AlbumImage> queryAlbumImages(@NonNull String albumName){
        Activity caller = activityReference.get();
        Uri collection;
        String[] projection;
        String selection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.RELATIVE_PATH};
            selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
            selection = MediaStore.Images.Media.DATA + " LIKE ?";
        }
        String path = File.separator+caller.getString(R.string.app_name)+File.separator+albumName+File.separator;
        String[] selectionArgs = new String[]{"%" + path + "%"};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
        // Keep the uri of the last photo taken on each album
        try (Cursor cursor = Objects.requireNonNull(caller.getApplicationContext().getContentResolver().query(
                collection, projection, selection, selectionArgs, sortOrder)
        )) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            ArrayList<AlbumImage> list = new ArrayList<>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                list.add(new AlbumImage(id,ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)));
            }
            return list;
        }
    }

    /*
     * This method retrieve some information regarding an image starting from
     * the id of media content.
     */
    public ImageDetails queryImageDetails(long id){
        Activity caller = activityReference.get();
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        String[] projection = new String[]{
                MediaStore.Images.Media.DISPLAY_NAME,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                        MediaStore.Images.Media.RELATIVE_PATH : MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.WIDTH,MediaStore.Images.Media.HEIGHT};
        String selection = MediaStore.Images.Media._ID + " = ?";
        String[] selectionArgs = new String[]{String.valueOf(id)};
        // Keep the uri of the last photo taken on each album
        try (Cursor cursor = Objects.requireNonNull(caller.getApplicationContext().getContentResolver().query(
                collection, projection, selection, selectionArgs, null)
        )) {
            ImageDetails imageDetails = null;
            if (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                String data;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH));
                }else{
                    data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    data = data.substring(data.indexOf(Environment.DIRECTORY_PICTURES),data.lastIndexOf(File.separatorChar));
                }
                Date date = new Date(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))*1000);
                int size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                int width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                int height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
                imageDetails = new ImageDetails(name,data,date,size,width,height);
            }
            return imageDetails;
        }
    }

    /*
     * This method return true if there is an album with the name provided
     * as parameter.
     */
    public boolean queryAlbumExists(@NonNull String albumName){
        Activity caller = activityReference.get();
        Uri collection;
        String[] projection;
        String selection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            projection = new String[]{MediaStore.Images.Media.RELATIVE_PATH};
            selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            projection = new String[]{MediaStore.Images.Media.DATA};
            selection = MediaStore.Images.Media.DATA + " LIKE ?";
        }
        String path = caller.getString(R.string.app_name)+File.separator+albumName+File.separator;
        String[] selectionArgs = new String[]{"%"+path+"%"};
        try (Cursor cursor = Objects.requireNonNull(caller.getApplicationContext().getContentResolver().query(
                collection, projection, selection, selectionArgs, null)
        )) {
            return cursor.moveToNext();
        }
    }

    /*
     * This method retrieve and return a set of all the album name available in the
     * ImageGallery.
     */
    public Set<String> queryAlbums(){
        Activity caller = activityReference.get();
        Uri collection;
        String[] projection;
        String selection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            projection = new String[]{MediaStore.Images.Media.RELATIVE_PATH};
            selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            projection = new String[]{MediaStore.Images.Media.DATA};
            selection = MediaStore.Images.Media.DATA + " LIKE ?";
        }
        String path = File.separator+caller.getString(R.string.app_name)+File.separator;
        String[] selectionArgs = new String[]{"%"+path+"%"};
        try (Cursor cursor = Objects.requireNonNull(caller.getApplicationContext().getContentResolver().query(
                collection, projection, selection, selectionArgs, null)
        )) {
            HashSet<String> albums = new HashSet<>();
            int dataColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH) :
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            while(cursor.moveToNext()){
                String data = cursor.getString(dataColumn);
                String[] list = data.split(File.separator);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    albums.add(list[list.length - 1]);
                }else{
                    albums.add(list[list.length - 2]);
                }

            }
            return albums;
        }
    }

    /*
     * This method retrieve all the image with a geotag location then it returns
     * a collection of ImageMapLocation by grouping near points.
     * An ImageMapLocation will be marked on the map using the location and
     * image uri of the last photo taken in that location. It also contains
     * the uri and id of all the image near that location.
     */
    public Collection<ImageMapLocation> queryImageLocations(int n_latest){
        Activity caller = activityReference.get();
        Uri collection;
        String selection;
        String[] projection = new String[]{MediaStore.Images.Media._ID};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            selection = MediaStore.Images.Media.DATA + " LIKE ?";
        }
        String path = File.separator+caller.getString(R.string.app_name)+File.separator;
        String[] selectionArgs = new String[]{"%"+path+"%"};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
        // Keep the uri of the last photo taken on each album
        try (Cursor cursor = Objects.requireNonNull(caller.getApplicationContext().getContentResolver().query(
                collection, projection, selection, selectionArgs, sortOrder)
        )) {
            ArrayList<ImageMapLocation> list = new ArrayList<>();
            while (cursor.moveToNext() && list.size() < n_latest) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                // Get location data using the Exifinterface library.
                // Exception occurs if ACCESS_MEDIA_LOCATION permission isn't granted.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) uri = MediaStore.setRequireOriginal(uri);
                try(InputStream stream = caller.getContentResolver().openInputStream(uri)){
                    if(stream != null){
                        ExifInterface exifInterface = new ExifInterface(stream);
                        // If lat/long is null, fall back to the coordinates (0, 0).
                        double[] latLong = exifInterface.getLatLong();
                        if(latLong != null){
                            // Check if there is another ImageMapLocation near (0.1 km)
                            boolean b = false;
                            for(ImageMapLocation iml : list){
                                if(iml.distance(latLong[0],latLong[1])<=0.1){
                                    iml.addImage(id,uri);
                                    b = true;
                                    break;
                                }
                            }
                            // If there is no near ImageMapLocation add a new one.
                            if(!b) list.add(new ImageMapLocation(id,uri,latLong[0],latLong[1]));
                        }
                    }
                }catch (IOException ignored){}
            }
            return list;
        }
    }

    /* Checks if external storage is available for read and write*/
    public static boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /* Checks if external storage is available to at least read*/
    public static boolean isExternalStorageReadable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
    }
}