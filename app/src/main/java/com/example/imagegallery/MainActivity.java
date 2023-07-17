package com.example.imagegallery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {
    // LOG String
    private static final String TAG = MainActivity.class.getSimpleName();
    // REQUESTS identifier
    public static final int PHOTO_REQUEST = 1;
    // Album where photos are saved
    private String captureAlbum;
    // Preferences
    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Shared Preferences
        preferences = getSharedPreferences(getString(R.string.user_shared_pref), MODE_PRIVATE);
        // Dark Mode
        if(preferences.getBoolean(getString(R.string.pref_dark_mode),false))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        // Capture Album
        captureAlbum = preferences.getString(getString(R.string.pref_capture_album),null);
    }

    /*
     * Manage the behaviour for the Capture Button.
     */
    public void manageCapture(View view){
        Intent i = new Intent(this,PhotoCapture.class);
        i.putExtra(PhotoCapture.EXTRA_ALBUM_NAME, captureAlbum!=null ? captureAlbum : getString(R.string.pref_default_album));
        startActivityForResult(i,PHOTO_REQUEST);
    }

    /*
     * Manage the behaviour for the Gallery Button.
     */
    public void manageGallery(View view){
        Log.i(TAG,"Starting Gallery Activity.");
        Intent i = new Intent(this, GalleryActivity.class);
        startActivity(i);
    }

    /*
     * Manage the behaviour for the Settings Button.
     */
    public void manageSettings(View view){
        Log.i(TAG,"Starting Settings Activity.");
        Intent i = new Intent(this,SettingsActivity.class);
        startActivity(i);
    }

    /*
     * Manage the behaviour for the Credits Button.
     */
    public void manageCredits(View view){
        Log.i(TAG,"Starting Credits Activity.");
        Intent i = new Intent(this,CreditsActivity.class);
        startActivity(i);
    }

    /*
     * Manage the behaviour when the photo request activity terminated.
     * It will set the PREF_CAPTURE_ALBUM to the default option "Camera"
     * if there was no PREF_CAPTURE_ALBUM defined after the album creation.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(captureAlbum==null && requestCode == PHOTO_REQUEST && resultCode == RESULT_OK){
            // Shared Preferences
            SharedPreferences.Editor editor = preferences.edit();
            // Update Pref Capture Album
            captureAlbum = getString(R.string.pref_default_album);
            editor.putString(getString(R.string.pref_capture_album), getString(R.string.pref_default_album));
            editor.apply();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}