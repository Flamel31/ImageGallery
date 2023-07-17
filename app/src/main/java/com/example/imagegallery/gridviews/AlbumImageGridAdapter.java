package com.example.imagegallery.gridviews;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.imagegallery.R;
import com.example.imagegallery.utilities.AlbumImage;
import com.example.imagegallery.utilities.PhotoManager;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.Arrays;

public class AlbumImageGridAdapter extends BaseAdapter {
    private static final int CACHE_SIZE = 70;
    // Context
    private Activity mContext;
    // Items used
    private AlbumImage[] items;
    // Items selected
    private boolean[] itemsSelected;
    // Is selectionMode on?
    private boolean selectionMode;
    // Thumbnail cache
    private Bitmap[] thumbnailCache;
    private int[] thumbnailId;
    // PhotoManager to load thumbnail
    private PhotoManager photoManager;

    public AlbumImageGridAdapter(@NonNull Activity mContext, @NonNull AlbumImage[] items, boolean[] itemsSelected){
        this.mContext = mContext;
        this.items = items;
        this.itemsSelected = itemsSelected != null && itemsSelected.length == items.length ?
                itemsSelected : new boolean[items.length];
        photoManager = new PhotoManager(mContext);
        loadThumbnailCache();
    }

    private void loadThumbnailCache(){
        thumbnailCache = new Bitmap[CACHE_SIZE];
        thumbnailId = new int[CACHE_SIZE];
        int i = 0;
        for(;i<items.length && i<CACHE_SIZE;i++){
            thumbnailCache[i] = getThumbnail(items[i].getUri());
            thumbnailId[i] = i;
        }
        for(;i<CACHE_SIZE;i++)
            thumbnailId[i] = -1;
    }

    private Bitmap getThumbnail(Uri uri){
       return photoManager.getThumbnailSquare(uri,300);
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int i) {
        return items[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        // Inflate Layout
        if(convertView == null){
            LayoutInflater inflater = mContext.getLayoutInflater();
            convertView = inflater.inflate(R.layout.album_image_grid_item,viewGroup,false);
        }
        // Retrieve Bitmap
        RoundedImageView imageView = convertView.findViewById(R.id.album_image_grid_item);
        ImageView checkView = convertView.findViewById(R.id.check_grid_item);
        if(thumbnailId[i%CACHE_SIZE] != i){
            thumbnailCache[i%CACHE_SIZE] = getThumbnail(items[i].getUri());
            thumbnailId [i%CACHE_SIZE] = i;
        }
        imageView.setImageBitmap(thumbnailCache[i%CACHE_SIZE]);
        // Selection
        if(isSelectionMode() && isItemSelected(i)){
            imageView.setColorFilter(Color.argb(90,0,0,0));
            checkView.setVisibility(View.VISIBLE);
        }else{
            imageView.setColorFilter(Color.argb(0,0,0,0));
            checkView.setVisibility(View.GONE);
        }
        return convertView;
    }

    public boolean isItemSelected(int i){
        return itemsSelected[i];
    }

    public void toggleSelectionItem(int i){
        itemsSelected[i] = !itemsSelected[i];
        notifyDataSetChanged();
    }

    public void setSelectionAllItems(boolean value){
        Arrays.fill(itemsSelected, value);
        notifyDataSetChanged();
    }

    public boolean[] getSelectedArray() {
        return itemsSelected;
    }

    public Uri[] getSelectedItemsUri(){
        ArrayList<Uri> list = new ArrayList<>();
        for(int i=0;i<items.length;i++)
            if(itemsSelected[i])
                list.add(items[i].getUri());
        return list.toArray(new Uri[0]);
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }
}
