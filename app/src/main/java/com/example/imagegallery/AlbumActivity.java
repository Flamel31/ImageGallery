package com.example.imagegallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.imagegallery.async.DeleteImagesAsync;
import com.example.imagegallery.async.MoveImagesAsync;
import com.example.imagegallery.dialogs.AlbumPickerDialogFragment;
import com.example.imagegallery.dialogs.DeleteAlbumDialogFragment;
import com.example.imagegallery.dialogs.DeleteImageDialogFragment;
import com.example.imagegallery.gridviews.AlbumImageGridAdapter;
import com.example.imagegallery.utilities.AlbumImage;
import com.example.imagegallery.utilities.PhotoManager;
import com.example.imagegallery.utilities.QueryManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Set;

public class AlbumActivity extends AppCompatActivity implements DeleteAlbumDialogFragment.DeleteAlbumDialogListener,
        AlbumPickerDialogFragment.AlbumPickerDialogListener, DeleteImageDialogFragment.DeleteImageDialogListener {
    // Extra Key
    public static final String EXTRA_ALBUM_NAME = AlbumActivity.class.getName()+".extra.ALBUM_NAME";
    // SavedInstance Key
    public static final String SELECTED_ITEMS_KEY = AlbumActivity.class.getName()+".key.SELECTED_ITEMS";
    // REQUESTS identifier
    public static final int PHOTO_REQUEST = 1;
    public static final int PERMISSIONS_REQUEST = 2;
    // Album name reference
    private String albumName;
    // Query Manager
    private QueryManager queryManager;
    // PhotoManager
    private PhotoManager photoManager;
    // ActionMode
    private ActionMode selectionMode;
    // SavedInstance variables
    private boolean[] selectedItems;
    // Help preventing the user to start multiple AsyncTask for the same operation.
    private boolean asyncTaskInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        // Retrieve Intent Extras
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        // AppBar
        MaterialToolbar toolbar = findViewById(R.id.album_toolbar);
        toolbar.setTitle(albumName);
        setSupportActionBar(toolbar);
        // Query Manager Setup
        queryManager = new QueryManager(AlbumActivity.this);
        // PhotoManager
        photoManager = new PhotoManager(AlbumActivity.this);
        // If SavedIstance
        if(savedInstanceState != null){
            selectedItems = savedInstanceState.getBooleanArray(SELECTED_ITEMS_KEY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.album_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks
        // on the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.action_add_photo:
                Intent i = new Intent(this,PhotoCapture.class);
                i.putExtra(PhotoCapture.EXTRA_ALBUM_NAME,albumName);
                startActivityForResult(i,PHOTO_REQUEST);
                break;
            case R.id.action_delete_album:
                DeleteAlbumDialogFragment dialogFragment = new DeleteAlbumDialogFragment();
                dialogFragment.show(getSupportFragmentManager(), DeleteAlbumDialogFragment.class.getSimpleName());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Method called by the DeleteAlbum Dialog when clicked on a
     * positive button (Confirm Action).
     */
    @Override
    public void onDeleteAlbumPositiveClick() {
        // Check if the external storage is writable
        if(!QueryManager.isExternalStorageWritable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            finish();
        }
        photoManager.deleteAlbum(albumName);
        Toast.makeText(getApplicationContext(),R.string.album_deleted,Toast.LENGTH_SHORT).show();
        finish();
    }

    /*
     * Method used to build the grid that contains all the thumbnail image.
     */
    private void buildAlbumTable(){
        // Check if the external storage is readable
        if(!QueryManager.isExternalStorageReadable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            finish();
        }
        // Checking Permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(AlbumActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(AlbumActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
                return;
            }
        }
        GridView gridView = findViewById(R.id.album_grid);
        AlbumImage[] images = queryManager.queryAlbumImages(albumName).toArray(new AlbumImage[0]);
        gridView.setAdapter(new AlbumImageGridAdapter(AlbumActivity.this,images,selectedItems));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                // Keep the user from selecting when an Async Task is running
                if(asyncTaskInProgress) return;
                // If SelectionMode select otherwise start ShowImageIntent on click
                if(selectionMode != null){
                    AlbumImageGridAdapter adapter = (AlbumImageGridAdapter)adapterView.getAdapter();
                    adapter.toggleSelectionItem(pos);
                }else{
                    Object o = adapterView.getItemAtPosition(pos);
                    if(o instanceof AlbumImage){
                        AlbumImage albumImage = (AlbumImage)o;
                        Intent i = new Intent(getApplicationContext(),ShowImageActivity.class);
                        i.putExtra(ShowImageActivity.EXTRA_IMAGE_ID,albumImage.getId());
                        i.putExtra(ShowImageActivity.EXTRA_IMAGE_URI,albumImage.getUri());
                        startActivity(i);
                    }
                }
            }
        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long l) {
                if(selectionMode == null){
                    selectionMode = startActionMode(selectionModeCallback);
                    AlbumImageGridAdapter adapter = (AlbumImageGridAdapter)adapterView.getAdapter();
                    adapter.toggleSelectionItem(pos);
                }
                return true;
            }
        });
    }

    /*
     * SelectionMode Callback, called when starting the ActionMode
     */
    private ActionMode.Callback selectionModeCallback = new ActionMode.Callback(){
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.album_selection_mode, menu);
            // Retrieve GridAdapter
            GridView gridView = findViewById(R.id.album_grid);
            AlbumImageGridAdapter adapter = (AlbumImageGridAdapter)gridView.getAdapter();
            // Activate selectionMode on GridAdapter
            adapter.setSelectionMode(true);
            adapter.notifyDataSetChanged();
            // Set selectionMode title
            actionMode.setTitle(R.string.select_images);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            // Prevent the user to start multiple AsyncTask
            if(asyncTaskInProgress) return false;
            switch(menuItem.getItemId()){
                case R.id.action_delete_images:
                    DeleteImageDialogFragment deleteFragment = new DeleteImageDialogFragment();
                    deleteFragment.show(getSupportFragmentManager(),DeleteImageDialogFragment.class.getSimpleName());
                    return true;
                case R.id.action_move_images:
                    AlbumPickerDialogFragment dialogFragment = new AlbumPickerDialogFragment();
                    // Query all albums name
                    Set<String> albums = queryManager.queryAlbums();
                    // Bundle arguments
                    Bundle bundle = new Bundle();
                    bundle.putStringArray(AlbumPickerDialogFragment.ARG_ALBUMS,albums.toArray(new String[0]));
                    // Set arguments
                    dialogFragment.setArguments(bundle);
                    dialogFragment.show(getSupportFragmentManager(),AlbumPickerDialogFragment.class.getSimpleName());
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            // Retrieve GridAdapter
            GridView gridView = findViewById(R.id.album_grid);
            AlbumImageGridAdapter adapter = (AlbumImageGridAdapter)gridView.getAdapter();
            // Deselect all items
            adapter.setSelectionAllItems(false);
            // Deactivate selectionMode on GridAdapter
            adapter.setSelectionMode(false);
            // Set ActionMode selectionMode to null
            selectionMode = null;
            // Reset TaskInProgress
            asyncTaskInProgress = false;
        }
    };

    /*
     * Methods called by the Dialog Fragment when clicked on
     * a positive button (Confirm Action).
     */

    @Override
    public void onAlbumPickerPositiveClick(@NonNull String albumName) {
        if(albumName.equals(this.albumName)){
            Toast.makeText(getApplicationContext(),R.string.images_already_in_album,Toast.LENGTH_SHORT).show();
        }else{
            GridView gridView = findViewById(R.id.album_grid);
            AlbumImageGridAdapter adapter = (AlbumImageGridAdapter)gridView.getAdapter();
            prepareProgressUpdate();
            asyncTaskInProgress = true;
            new MoveImagesAsync(AlbumActivity.this,albumName).execute(adapter.getSelectedItemsUri());
        }
    }

    @Override
    public void onDeleteImagePositiveClick() {
        GridView gridView = findViewById(R.id.album_grid);
        AlbumImageGridAdapter adapter = (AlbumImageGridAdapter)gridView.getAdapter();
        prepareProgressUpdate();
        asyncTaskInProgress = true;
        new DeleteImagesAsync(AlbumActivity.this).execute(adapter.getSelectedItemsUri());
    }

    /*
     * Methods Called by the Async Task to update the UI
     */

    public void prepareProgressUpdate(){
        // Retrieve view
        ProgressBar bar = findViewById(R.id.album_progress_bar);
        TextView textView = findViewById(R.id.album_progress_text);
        // Set Text
        bar.setProgress(0);
        textView.setText(R.string.please_wait_moment);
        // Set Visibility
        bar.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
    }

    public void progressUpdate(int progress){
        // Set Progress
        String p = progress+"%";
        ((ProgressBar)findViewById(R.id.album_progress_bar)).setProgress(progress);
        ((TextView)findViewById(R.id.album_progress_text)).setText(p);
    }

    public void endSelectionMode(String toastMessage){
        if(selectionMode != null){
            // Set visibility GONE
            findViewById(R.id.album_progress_bar).setVisibility(View.GONE);
            findViewById(R.id.album_progress_text).setVisibility(View.GONE);
            // Stop selectionMode
            selectionMode.finish();
            // Rebuild Album Table
            buildAlbumTable();
            // Show Toast
            Toast.makeText(getApplicationContext(),toastMessage,Toast.LENGTH_SHORT).show();
        }
    }



    /*
     * Save Selection Mode Status and Selected Items
     * in order to rebuild on ConfigChange.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(selectionMode != null){
            GridView gridView = findViewById(R.id.album_grid);
            AlbumImageGridAdapter adapter = (AlbumImageGridAdapter)gridView.getAdapter();
            outState.putBooleanArray(SELECTED_ITEMS_KEY,adapter.getSelectedArray());
        }
        super.onSaveInstanceState(outState);
    }

    /*
     * Handle Request Permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSIONS_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                buildAlbumTable();
            }else{
                Toast.makeText(getApplicationContext(),R.string.write_external_perm_not_granted,Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /*
     * Rebuild the album table each time the activity is resumed, in order
     * to keep the activity as updated as possible.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Build/Refresh album table
        buildAlbumTable();
        // Restart Selection Mode if necessary
        if(selectedItems != null)
            selectionMode = startActionMode(selectionModeCallback);
    }
}