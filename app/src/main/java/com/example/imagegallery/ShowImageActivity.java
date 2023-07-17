package com.example.imagegallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.imagegallery.async.RotateImageAsync;
import com.example.imagegallery.dialogs.AlbumPickerDialogFragment;
import com.example.imagegallery.dialogs.DeleteImageDialogFragment;
import com.example.imagegallery.dialogs.ImageDetailsDialogFragment;
import com.example.imagegallery.utilities.ImageDetails;
import com.example.imagegallery.utilities.PhotoManager;
import com.example.imagegallery.utilities.QueryManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.jsibbold.zoomage.ZoomageView;

import java.util.Set;

public class ShowImageActivity extends AppCompatActivity implements DeleteImageDialogFragment.DeleteImageDialogListener,
        AlbumPickerDialogFragment.AlbumPickerDialogListener {
    // Extra Key
    public static final String EXTRA_IMAGE_ID = ShowImageActivity.class.getName()+".extra.IMAGE_ID";
    public static final String EXTRA_IMAGE_URI = ShowImageActivity.class.getName()+".extra.IMAGE_URI";
    // LOG String
    private static final String TAG = ShowImageActivity.class.getSimpleName();
    // Request
    public static final int PERMISSIONS_REQUEST = 1;
    // Identifier of the image to display used by the ContentResolver to retrieve information
    // private long idImage;
    // Uri location of the image to display
    private Uri uriImage;
    // Id of the image to display used to query
    // the image details
    private long idImage;
    // Image Details
    private ImageDetails imageDetails;
    // Photo Manager
    private PhotoManager photoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_image);
        // AppBar
        MaterialToolbar toolbar = findViewById(R.id.show_image_toolbar);
        setSupportActionBar(toolbar);
        // Retrieve identifier and uri of the image to display
        Intent i = getIntent();
        uriImage = i.getParcelableExtra(EXTRA_IMAGE_URI);
        idImage = i.getLongExtra(EXTRA_IMAGE_ID,-1);
        // PhotoManager
        photoManager = new PhotoManager(ShowImageActivity.this);
        buildShowImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.show_image_app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks
        // on the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.action_rotate_left:
                new RotateImageAsync(ShowImageActivity.this,270).execute(uriImage);
                break;
            case R.id.action_rotate_right:
                new RotateImageAsync(ShowImageActivity.this,90).execute(uriImage);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * It will check the permission of accessing the external storage and then
     * proceed to set the ZoomageView Uri and retrieve the image details with
     * the query manager.
     */
    public void buildShowImage(){
        // Check if the external storage is readable
        if(!QueryManager.isExternalStorageReadable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            finish();
        }
        // Checking Permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(ShowImageActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(ShowImageActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
                return;
            }
        }
        // Set ZoomageView URI
        setZoomageUri();
        // Retrieve details of the image
        retrieveImageDetails(idImage);
    }

    /*
     * Action performed when the user confirm the action of deleting the image from
     * the DeleteImage Dialog.
     */
    @Override
    public void onDeleteImagePositiveClick() {
        // Check if the external storage is writable
        if(!QueryManager.isExternalStorageWritable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            finish();
        }
        photoManager.deleteImageFile(uriImage);
        Toast.makeText(getApplicationContext(),R.string.image_deleted,Toast.LENGTH_SHORT).show();
        finish();
    }

    /*
     * Action performed when the user select an album from the AlbumPicker Dialog.
     */
    @Override
    public void onAlbumPickerPositiveClick(@NonNull String albumName) {
        // Check if the external storage is readable
        if(!QueryManager.isExternalStorageWritable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            finish();
        }
        photoManager.moveFile(uriImage,albumName);
        Toast.makeText(getApplicationContext(),getString(R.string.image_moved_to,albumName),Toast.LENGTH_SHORT).show();
        finish();
    }

    /*
     * Create and show a dialog fragment with all the image details
     */
    public void manageDetails(View view){
        ImageDetailsDialogFragment dialogFragment = new ImageDetailsDialogFragment();
        // Bundle to pass parameters
        Bundle bundle = new Bundle();
        bundle.putString(ImageDetailsDialogFragment.ARG_FILENAME,imageDetails.getFileName());
        bundle.putString(ImageDetailsDialogFragment.ARG_PATH,imageDetails.getPath());
        bundle.putLong(ImageDetailsDialogFragment.ARG_DATE_TAKEN,imageDetails.getDateTaken().getTime());
        bundle.putInt(ImageDetailsDialogFragment.ARG_SIZE,imageDetails.getSize());
        bundle.putInt(ImageDetailsDialogFragment.ARG_WIDTH,imageDetails.getWidth());
        bundle.putInt(ImageDetailsDialogFragment.ARG_HEIGHT,imageDetails.getHeight());
        // Set Arguments
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(), ImageDetailsDialogFragment.class.getSimpleName());
    }

    /*
     * Manage the behaviour for the Share Button.
     */
    public void manageShare(View view){
        // Check if the external storage is readable
        if(!QueryManager.isExternalStorageReadable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            finish();
        }
        // Share intent
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uriImage);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
    }

    /*
     * Manage the behaviour for the Delete Button.
     */
    public void manageDelete(View view){
        DeleteImageDialogFragment dialogFragment = new DeleteImageDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), DeleteImageDialogFragment.class.getSimpleName());
    }

    /*
     * Manage the behaviour for the Move To Button.
     */
    public void manageMoveTo(View view){
        QueryManager queryManager = new QueryManager(ShowImageActivity.this);
        AlbumPickerDialogFragment dialogFragment = new AlbumPickerDialogFragment();
        // Query all albums name
        Set<String> albums = queryManager.queryAlbums();
        // Bundle arguments
        Bundle bundle = new Bundle();
        bundle.putStringArray(AlbumPickerDialogFragment.ARG_ALBUMS,albums.toArray(new String[0]));
        // Set arguments
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(),AlbumPickerDialogFragment.class.getSimpleName());
    }

    /*
     * Set the URI location in the Zoomage View.
     */
    private void setZoomageUri(){
        ZoomageView zoomageView = findViewById(R.id.show_image_zoomview);
        zoomageView.setImageURI(uriImage);
    }

    /*
     * Retrieve and store the image details.
     */
    private void retrieveImageDetails(long id){
        // Query Manager Setup
        QueryManager queryManager = new QueryManager(ShowImageActivity.this);
        imageDetails = queryManager.queryImageDetails(id);
        Log.i(TAG,imageDetails.toString());
    }

    /*
     * Action performed after the RotateImage Async Task ended
     * It will refresh teh Zoomage View.
     */

    public void postRotationTask(boolean bool){
        if(bool){
            ((ZoomageView)findViewById(R.id.show_image_zoomview)).setImageURI(null);
            setZoomageUri();
        } else{
            Toast.makeText(getApplicationContext(),R.string.error_encountered_rotation,Toast.LENGTH_SHORT).show();
        }
    }



    /*
     * Handle Request Permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSIONS_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                buildShowImage();
            }else{
                Toast.makeText(getApplicationContext(),R.string.write_external_perm_not_granted,Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}