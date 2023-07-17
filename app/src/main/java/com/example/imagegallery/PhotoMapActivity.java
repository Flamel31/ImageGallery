package com.example.imagegallery;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.imagegallery.dialogs.GridPhotoDialogFragment;
import com.example.imagegallery.utilities.ImageMapLocation;
import com.example.imagegallery.utilities.PhotoManager;
import com.example.imagegallery.utilities.QueryManager;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Collection;

public class PhotoMapActivity extends AppCompatActivity {
    // LOG String
    private static final String TAG = PhotoMapActivity.class.getSimpleName();
    // Request
    private final int REQUEST_PERMISSIONS_REQUEST = 1;
    // Query Manager
    private QueryManager queryManager;
    // GeoPoints ArrayList Used to calculate map bounds
    private ArrayList<GeoPoint> geoPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_map);
        // Query Manager Setup
        queryManager = new QueryManager(PhotoMapActivity.this);
        // GeoPoints
        geoPoints = new ArrayList<>();
        // Check if the external storage is writable
        if(!QueryManager.isExternalStorageWritable()){
            Toast.makeText(getApplicationContext(),R.string.external_storage_not_available,Toast.LENGTH_LONG).show();
            finish();
        }
        // Request Permissions
        requestPermissionsIfNecessary(new String[] {
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                // INTERNET to download map tiles
                Manifest.permission.INTERNET,
                // ACCESS_MEDIA_LOCATION (on SDK >=29)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? Manifest.permission.ACCESS_MEDIA_LOCATION : null,
        });
    }

    private void buildPhotoMap(){
        // load/initialize the osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        // Map Tilesource
        MapView map = findViewById(R.id.photo_map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        // Add Marker for the latest 100 images with location
        Collection<ImageMapLocation> imageMapLocations = queryManager.queryImageLocations(100);
        PhotoManager photoManager = new PhotoManager(PhotoMapActivity.this);
        for(final ImageMapLocation iml : imageMapLocations){
            // Create Marker
            Marker marker = new Marker(map);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            // Set Position
            GeoPoint position = new GeoPoint(iml.getLatitude(),iml.getLongitude());
            marker.setPosition(position);
            // Add to Geopoints ArrayList
            geoPoints.add(position);
            // Set Marker Icon
            marker.setIcon(new BitmapDrawable(getResources(),photoManager.getCircledBitmap(iml.getThumbnailUri(),100)));
            // Add Listener to Open FragmentDialog
            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    // Create Dialog Fragment
                    GridPhotoDialogFragment dialogFragment = new GridPhotoDialogFragment();
                    // Prepare Arguments
                    Bundle bundle = new Bundle();
                    bundle.putLongArray(GridPhotoDialogFragment.ARG_ARRAY_ID,iml.getIdArray());
                    bundle.putParcelableArray(GridPhotoDialogFragment.ARG_ARRAY_URI,iml.getUriArray());
                    dialogFragment.setArguments(bundle);
                    // Show Dialog Fragment
                    dialogFragment.show(getSupportFragmentManager(),PhotoMapActivity.class.getSimpleName());
                    return true;
                }
            });
            map.getOverlays().add(marker);
        }
        // Set Map Bounds when loaded
        map.addOnFirstLayoutListener(new MapView.OnFirstLayoutListener() {
            @Override
            public void onFirstLayout(View v, int left, int top, int right, int bottom) {
                if(geoPoints.size() > 0){
                    MapView m = (MapView)v;
                    if(geoPoints.size() > 1){
                        BoundingBox box = BoundingBox.fromGeoPointsSafe(geoPoints).increaseByScale(1.5f);
                        m.zoomToBoundingBox(box,false);
                        m.invalidate();
                    }else{
                        IMapController mapController = m.getController();
                        mapController.setZoom(15.5);
                        mapController.setCenter(geoPoints.get(0));
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), R.string.media_location_request_perm_not_granted, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
            buildPhotoMap();
        }
    }

    private void requestPermissionsIfNecessary(@NonNull String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (permission != null && ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(PhotoMapActivity.this, permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST);
        }else{
            buildPhotoMap();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // needed for compass, my location overlays, v6.0.0 and up
        MapView map = findViewById(R.id.photo_map);
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // needed for compass, my location overlays, v6.0.0 and up
        MapView map = findViewById(R.id.photo_map);
        map.onPause();
    }
}