package com.example.imagegallery.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.imagegallery.R;
import com.example.imagegallery.ShowImageActivity;
import com.example.imagegallery.gridviews.AlbumImageGridAdapter;
import com.example.imagegallery.utilities.AlbumImage;

public class GridPhotoDialogFragment extends DialogFragment {
    // Bundle ARGS
    public static final String ARG_ARRAY_ID = GridPhotoDialogFragment.class.getName()+".extra.ARRAY_ID";
    public static final String ARG_ARRAY_URI = GridPhotoDialogFragment.class.getName()+".extra.ARRAY_URI";

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.content_gallery, null);
        AlbumImage[] items;
        int numberOfPhoto = 0;
        if(bundle != null){
            Uri[] arrayUri = (Uri[])bundle.getParcelableArray(ARG_ARRAY_URI);
            long[] arrayId = bundle.getLongArray(ARG_ARRAY_ID);
            if(arrayUri != null && arrayId != null){
                items = new AlbumImage[arrayUri.length];
                numberOfPhoto = items.length;
                for(int i=0;i<items.length;i++)
                    items[i] = new AlbumImage(arrayId[i],arrayUri[i]);
                GridView gridView = view.findViewById(R.id.gallery_grid);
                gridView.setAdapter(new AlbumImageGridAdapter(requireActivity(),items,null));
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                        Object o = adapterView.getItemAtPosition(pos);
                        if (o instanceof AlbumImage) {
                            AlbumImage albumImage = (AlbumImage) o;
                            Intent i = new Intent(requireActivity(), ShowImageActivity.class);
                            i.putExtra(ShowImageActivity.EXTRA_IMAGE_ID, albumImage.getId());
                            i.putExtra(ShowImageActivity.EXTRA_IMAGE_URI, albumImage.getUri());
                            startActivity(i);
                        }
                    }
                });
            }
        }
        builder.setTitle("Photos near this location: "+numberOfPhoto)
                .setView(view)
                .setNegativeButton(R.string.close, null);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
