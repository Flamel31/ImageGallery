package com.example.imagegallery.utilities;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class ImageMapLocation {
    private double latitude;
    private double longitude;
    private ArrayList<Uri> listUri;
    private ArrayList<Long> listId;

    public ImageMapLocation(long id,@NonNull Uri uri,double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        listUri = new ArrayList<>();
        listId = new ArrayList<>();
        addImage(id,uri);
    }

    public Uri getThumbnailUri() {
        return listUri.get(0);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void addImage(long id, @NonNull Uri uri){
        listId.add(id);
        listUri.add(uri);
    }

    public long[] getIdArray(){
        long[] out = new long[listId.size()];
        for(int i=0;i<out.length;i++)
            out[i] = listId.get(i);
        return out;
    }

    public Uri[] getUriArray(){
        return listUri.toArray(new Uri[0]);
    }

    public double distance(double lat,double lon) {
        // The math module contains a function named toRadians which converts from
        // degrees to radians.
        double lat1 = Math.toRadians(latitude);
        double lon1 = Math.toRadians(longitude);
        double lat2 = Math.toRadians(lat);
        double lon2 = Math.toRadians(lon);
        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);
        double c = 2 * Math.asin(Math.sqrt(a));
        // Radius of earth in kilometers.
        double r = 6371;
        // calculate the result
        return (c * r);
    }


}
