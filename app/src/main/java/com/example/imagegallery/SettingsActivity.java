package com.example.imagegallery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imagegallery.dialogs.AlbumPickerDialogFragment;
import com.example.imagegallery.utilities.QueryManager;

import java.util.Set;

public class SettingsActivity extends AppCompatActivity implements AlbumPickerDialogFragment.AlbumPickerDialogListener {
    // Request
    public static final int PERMISSIONS_REQUEST = 1;
    // Preferences
    private SharedPreferences preferences;
    // All albums name
    private Set<String> albums;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Load Preferences
        preferences = getSharedPreferences(getString(R.string.user_shared_pref), MODE_PRIVATE);
        // Build UI
        buildSettingsUI();
    }

    /*
     * Build the Settings Activity UI by retrieving various information
     * from the SharedPreferences.
     */
    public void buildSettingsUI(){
        // Set Geotag Switch Compat
        SwitchCompat geoTagSwitch = findViewById(R.id.geotag_switch);
        geoTagSwitch.setChecked(preferences.getBoolean(getString(R.string.pref_geo_tag),true));
        geoTagSwitch.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(getString(R.string.pref_geo_tag),b);
                editor.apply();
            }
        });
        // Set DarkMode Switch Compat
        SwitchCompat darkModeSwitch = findViewById(R.id.dark_mode_switch);
        darkModeSwitch.setChecked(preferences.getBoolean(getString(R.string.pref_dark_mode),false));
        darkModeSwitch.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(getString(R.string.pref_dark_mode),b);
                editor.apply();
                if(b) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });
        // Set Default Album Text
        TextView defaultAlbumTextView = findViewById(R.id.default_album_text);
        String defaultAlbum = preferences.getString(getString(R.string.pref_capture_album),null);
        defaultAlbumTextView.setText(defaultAlbum==null ? getString(R.string.no_default_album) : defaultAlbum);
    }

    /*
     * Manage the behaviour for the Change Default Album Button.
     */
    public void manageChangeDefaultAlbum(View view){
        // Check if the albums have been already requested
        if(albums == null){
            // Check if the external storage is readable
            if(!QueryManager.isExternalStorageReadable()){
                Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
                finish();
            }
            // Check Permissions to READ_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
                    return;
                }
            }
            // Create Query Manager
            QueryManager queryManager = new QueryManager(SettingsActivity.this);
            // Request album
            albums = queryManager.queryAlbums();
        }
        if(albums.isEmpty()){
            Toast.makeText(getApplicationContext(),R.string.no_album_to_choose,Toast.LENGTH_SHORT).show();
            return;
        }
        // Create Fragment
        AlbumPickerDialogFragment dialogFragment = new AlbumPickerDialogFragment();
        // Bundle arguments
        Bundle bundle = new Bundle();
        bundle.putStringArray(AlbumPickerDialogFragment.ARG_ALBUMS,albums.toArray(new String[0]));
        // Set arguments
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(),AlbumPickerDialogFragment.class.getSimpleName());
    }

    /*
     * Action performed after picking an album name from the AlbumPicker Dialog.
     */
    @Override
    public void onAlbumPickerPositiveClick(@NonNull String albumName) {
        // Change EditText
        TextView defaultAlbumTextView = findViewById(R.id.default_album_text);
        defaultAlbumTextView.setText(albumName);
        // Update SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(R.string.pref_capture_album),albumName);
        editor.apply();
    }

    /*
     * Handle Permissions request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSIONS_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                manageChangeDefaultAlbum(findViewById(R.id.button_change_default_album));
            }else{
                Toast.makeText(getApplicationContext(),R.string.read_external_perm_not_granted,Toast.LENGTH_SHORT).show();
            }
        }
    }
}