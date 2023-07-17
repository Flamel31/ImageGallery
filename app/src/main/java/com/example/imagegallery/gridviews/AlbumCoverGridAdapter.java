package com.example.imagegallery.gridviews;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.imagegallery.utilities.AlbumCover;
import com.example.imagegallery.utilities.PhotoManager;
import com.makeramen.roundedimageview.RoundedImageView;

public class AlbumCoverGridAdapter extends BaseAdapter {
    private static final int CACHE_SIZE = 70;
    private Activity mContext;
    private AlbumCover[] items;
    // Thumbnail cache
    private Bitmap[] thumbnailCache;
    private int[] thumbnailId;
    // PhotoManager to load thumbnail
    private PhotoManager photoManager;

    public AlbumCoverGridAdapter(@NonNull Activity mContext, @NonNull AlbumCover[] items){
        this.mContext = mContext;
        this.items = items;
        photoManager = new PhotoManager(mContext);
        loadThumbnailCache();
    }

    private void loadThumbnailCache(){
        thumbnailCache = new Bitmap[CACHE_SIZE];
        thumbnailId = new int[CACHE_SIZE];
        int i = 0;
        for(;i<items.length && i<CACHE_SIZE;i++){
            thumbnailCache[i] = getThumbnail(items[i].getCoverUri());
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
        // Retrive cover image
        RoundedImageView cover = new RoundedImageView(mContext);
        if(thumbnailId[i%CACHE_SIZE] != i){
            thumbnailCache[i%CACHE_SIZE] = getThumbnail(items[i].getCoverUri());
            thumbnailId [i%CACHE_SIZE] = i;
        }
        cover.setImageBitmap(thumbnailCache[i%CACHE_SIZE]);
        // Set ImageView cover
        cover.setCornerRadius((float)50);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setAdjustViewBounds(true);
        // Set TextView album name
        TextView textView = new TextView(mContext);
        textView.setTextSize(16);
        textView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        textView.append(items[i].getAlbumName());
        textView.append(" (" + items[i].getPhotoCount() + ")");
        // Add to linear layout
        LinearLayout linear = new LinearLayout(mContext);
        linear.setOrientation(LinearLayout.VERTICAL);
        linear.addView(cover);
        linear.addView(textView);
        return linear;
    }
}
