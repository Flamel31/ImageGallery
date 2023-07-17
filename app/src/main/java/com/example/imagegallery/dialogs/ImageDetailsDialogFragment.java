package com.example.imagegallery.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.imagegallery.R;

import java.text.DateFormat;
import java.util.Date;

public class ImageDetailsDialogFragment extends DialogFragment {

    // Bundle ARGS
    public static final String ARG_FILENAME = ImageDetailsDialogFragment.class.getName()+".extra.FILENAME";
    public static final String ARG_PATH = ImageDetailsDialogFragment.class.getName()+".extra.PATH";
    public static final String ARG_DATE_TAKEN = ImageDetailsDialogFragment.class.getName()+".extra.DATE_TAKEN";
    public static final String ARG_SIZE = ImageDetailsDialogFragment.class.getName()+".extra.SIZE";
    public static final String ARG_WIDTH = ImageDetailsDialogFragment.class.getName()+".extra.WIDTH";
    public static final String ARG_HEIGHT = ImageDetailsDialogFragment.class.getName()+".extra.HEIGHT";

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_image_details, null);
        // Setting TextView text
        if(bundle != null){
            // Filename
            ((TextView)view.findViewById(R.id.dialog_filename)).setText(bundle.getString(ARG_FILENAME));
            // Path
            ((TextView)view.findViewById(R.id.dialog_path)).setText(bundle.getString(ARG_PATH));
            // Date
            Date dateTime = new Date(bundle.getLong(ARG_DATE_TAKEN));
            String date = DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.SHORT).format(dateTime);
            ((TextView)view.findViewById(R.id.dialog_date_taken)).setText(date);
            // Size
            String size = (double)(bundle.getInt(ARG_SIZE)/(1024*1024))+" MB";
            ((TextView)view.findViewById(R.id.dialog_size)).setText(size);
            // Dimensions
            String dimensions = bundle.getInt(ARG_WIDTH)+"x"+bundle.getInt(ARG_HEIGHT);
            ((TextView)view.findViewById(R.id.dialog_dimensions)).setText(dimensions);
        }
        builder.setView(view)
                .setTitle(R.string.image_details)
                // Add action buttons
                .setNeutralButton(R.string.close,null);
        return builder.create();
    }

}
