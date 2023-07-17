package com.example.imagegallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.imagegallery.dialogs.AddAlbumDialogFragment;
import com.example.imagegallery.gridviews.AlbumCoverGridAdapter;
import com.example.imagegallery.utilities.AlbumCover;
import com.example.imagegallery.utilities.QueryManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GalleryActivity extends AppCompatActivity implements AddAlbumDialogFragment.AddAlbumDialogListener {
    // LOG String
    private static final String TAG = GalleryActivity.class.getSimpleName();
    // REQUESTS identifier
    public static final int PHOTO_REQUEST = 1;
    public static final int PERMISSIONS_REQUEST = 2;

    // FloatingActionButton OnClickListener
    private View.OnClickListener fabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AddAlbumDialogFragment dialogFragment = new AddAlbumDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), AddAlbumDialogFragment.class.getSimpleName());
        }
    };
    // Query Manager
    private QueryManager queryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        // AppBar
        MaterialToolbar toolbar = findViewById(R.id.gallery_toolbar);
        setSupportActionBar(toolbar);
        // Floating Action
        FloatingActionButton fab = findViewById(R.id.add_album_fab);
        fab.setOnClickListener(fabOnClickListener);
        // Query Manager Setup
        queryManager = new QueryManager(GalleryActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks
        // on the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.action_gallery_map){
            Log.i(TAG, "Starting PhotoMap activity.");
            Intent i = new Intent(this,PhotoMapActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Action called after the user inserted the album name on the
     * NewAlbum Dialog.
     */
    @Override
    public void onDialogPositiveClick(@NonNull String albumName) {
        // Sanitize albumName
        albumName = albumName.trim().replaceAll("[^a-zA-Z0-9.\\-]","");
        if(!albumName.isEmpty()){
            if(!queryManager.queryAlbumExists(albumName)){
                Intent i = new Intent(this,PhotoCapture.class);
                i.putExtra(PhotoCapture.EXTRA_ALBUM_NAME,albumName);
                startActivityForResult(i,PHOTO_REQUEST);
            }else{
                Toast.makeText(this, R.string.album_already_exist, Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, R.string.album_reserved_chars, Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Function that check the permissions to manage the external storage
     * then it build the GridView with the array of AlbumCover required to
     * the Query Manager.
     */
    private void buildGalleryTable() {
        // Check if the external storage is readable
        if(!QueryManager.isExternalStorageReadable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            finish();
        }
        // Checking Permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(GalleryActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(GalleryActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
                return;
            }
        }
        GridView gridView = findViewById(R.id.gallery_grid);
        AlbumCover[] albumCovers = queryManager.queryAlbumCovers().toArray(new AlbumCover[0]);
        gridView.setAdapter(new AlbumCoverGridAdapter(GalleryActivity.this,albumCovers));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                Object o = adapterView.getItemAtPosition(pos);
                if(o instanceof AlbumCover){
                    AlbumCover albumCover = (AlbumCover)o;
                    Intent i = new Intent(getApplicationContext(),AlbumActivity.class);
                    i.putExtra(AlbumActivity.EXTRA_ALBUM_NAME,albumCover.getAlbumName());
                    startActivity(i);
                }
            }
        });
    }

    /*
     * Handle Request Permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSIONS_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                buildGalleryTable();
            }else{
                Toast.makeText(getApplicationContext(),R.string.write_external_perm_not_granted,Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /*
     * Handle the result of the PHOTO REQUEST started after the user prompted
     * a new album. If the user Canceled the action it will show a Toast.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_CANCELED)
                Toast.makeText(this,R.string.album_cannot_be_empty,Toast.LENGTH_SHORT).show();
    }

    /*
     * Rebuild the album table each time the activity is resumed, in order
     * to keep the activity as updated as possible.
     */
    @Override
    public void onResume(){
        super.onResume();
        buildGalleryTable();
    }
}