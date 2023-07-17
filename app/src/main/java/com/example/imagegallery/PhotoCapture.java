package com.example.imagegallery;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.imagegallery.utilities.PhotoManager;
import com.example.imagegallery.utilities.QueryManager;

public class PhotoCapture extends AppCompatActivity {
    // Extra KEY
    public static final String EXTRA_ALBUM_NAME = PhotoCapture.class.getName()+".extra.ALBUM_NAME";
    // REQUESTS identifier
    public static final int PHOTO_REQUEST = 1;
    public static final int LOCATION_REQUEST = 2;
    public static final int MEDIA_LOCATION_REQUEST = 3;
    // LOG String
    private static final String TAG = PhotoCapture.class.getSimpleName();
    // String album location of the image to capture
    private String albumName;
    // Location if the user don't grant permissions this is null
    private Location location;
    // PhotoManager
    private PhotoManager photoManager;
    // SharedPreferences
    private boolean shouldGeotag;
    // Uri of the last picture taken needed
    // to cancel the temp image in case of
    // RESULT_CANCELED
    private Uri lastPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get album name extra key
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        // Shared preferences
        SharedPreferences preferences = getSharedPreferences(getString(R.string.user_shared_pref), MODE_PRIVATE);
        shouldGeotag = preferences.getBoolean(getString(R.string.pref_geo_tag),true);
        // Photo Manager
        photoManager = new PhotoManager(PhotoCapture.this);
        checkingFundamentalPermissions();
    }

    /*
     * This function check the CAMERA and WRITE EXTERNAL permissions and request them
     * if needed. Then it it will pass to the Optional Permissions
     */
    public void checkingFundamentalPermissions(){
        // Checking Camera and Write External Storage Permission (required)
        if (ActivityCompat.checkSelfPermission(PhotoCapture.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(PhotoCapture.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG,"Requesting CAMERA and WRITE EXTERNAL STORAGE permissions.");
            ActivityCompat.requestPermissions(PhotoCapture.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PHOTO_REQUEST);
            return;
        }
        checkOptionalPermissions();
    }

    /*
     * This function check LOCATION and MEDIA ACCESS permission only if the user want
     * to geotag the new photo.
     * If the user want to geotag the new photo it will also retrieve the LastKnownLocation
     * from the LocationManager and register itself for updates.
     * Finally, the function will call the photoRequest method for the creation of an
     * IMAGE CAPTURE intent.
     */
    private void checkOptionalPermissions(){
        // If the user want to geotag the photo ask for permission
        // Default setting geotag=true
        if(shouldGeotag){
            // Checking Location Permission (optional)
            if (ActivityCompat.checkSelfPermission(PhotoCapture.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                Log.i(TAG,"Requesting OPTIONAL ACCESS FINE LOCATION permission");
                ActivityCompat.requestPermissions(PhotoCapture.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
                return;
            }
            // ONLY SDK >= 29
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
                    PhotoCapture.this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED){
                Log.i(TAG,"Requesting OPTIONAL ACCESS_MEDIA_LOCATION permission");
                ActivityCompat.requestPermissions(PhotoCapture.this, new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, MEDIA_LOCATION_REQUEST);
                return;
            }
            LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            if(locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location l) {
                        location = l;
                        LocationManager manager = (LocationManager)getSystemService(LOCATION_SERVICE);
                        if(manager!=null) manager.removeUpdates(this);
                    }
                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {}
                    @Override
                    public void onProviderEnabled(String s) {}
                    @Override
                    public void onProviderDisabled(String s) {}
                });
            }else{
                Log.i(TAG,"GPS Provider Disabled cannot geotag photo.");
            }
        }
        photoRequest();
    }

    /*
     * Handle Request Permissions
     * If the users doesn't grant CAMERA and WRITE EXTERNAL STORAGE permissions
     * the activity will end with an RESULT_CANCELED result. On the other hand
     * it will continue even if the user doesn't grant ACCESS FINE LOCATION
     * permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PHOTO_REQUEST) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    checkOptionalPermissions();
                else{
                    setResult(RESULT_CANCELED);
                    Toast.makeText(this, R.string.photo_request_perm_not_granted, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }else if(requestCode == LOCATION_REQUEST || requestCode == MEDIA_LOCATION_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                checkOptionalPermissions();
            }else{
                Log.i(TAG, "ACCESS FINE LOCATION or ACCESS MEDIA LOCATION permission not granted, starting a normal photo request.");
                photoRequest();
            }
        }
    }


    /*
     * Create a temporary file to store the photo taken by the ACTION_IMAGE_CAPTURE
     * intent.
     */
    private void photoRequest(){
        // Check if the external storage is writable
        if(!QueryManager.isExternalStorageWritable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }
        // Create image file with PhotoManager
        lastPhotoUri = photoManager.createImageFile(albumName);
        // Intent Capture Image
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // Specify Uri location where to save the photo
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, lastPhotoUri);
        Log.i(TAG,"Starting Camera Intent for results");
        startActivityForResult(cameraIntent, PHOTO_REQUEST);
    }

    /*
     * If the user requested to geotag the photo it will insert the location metadata
     * after the ACTION_IMAGE_CAPTURE result. In case of RESULT_CANCELED it will
     * remove the file created to store the photo.
     * Then the activity will end with the same result of the ACTION_IMAGE_CAPTURE activity.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_REQUEST){
            if(resultCode == RESULT_OK){
                if(location!=null) {
                    Log.i(TAG,"Add location info on:"+lastPhotoUri.toString());
                    photoManager.addLocationInformationToFile(lastPhotoUri, location);
                }
            }else{
                Log.i(TAG,"Delete temp image on:"+lastPhotoUri.toString());
                photoManager.deleteImageFile(lastPhotoUri);
            }
            setResult(resultCode);
            finish();
        }
    }
}