package com.example.imagegallery.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.imagegallery.R;

public class AlbumPickerDialogFragment extends DialogFragment {
    // Bundle ARGS
    public static final String ARG_ALBUMS = AlbumPickerDialogFragment.class.getName()+".arg.ALBUMS";

    public interface AlbumPickerDialogListener {
        void onAlbumPickerPositiveClick(@NonNull String albumName);
    }
    // Instance of the interface used to deliver action events
    private AlbumPickerDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        if(context instanceof AlbumPickerDialogListener){
            listener = (AlbumPickerDialogListener)context;
        }
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        Bundle bundle = getArguments();
        final String[] albums = bundle != null ? bundle.getStringArray(ARG_ALBUMS) : null;
        final NumberPicker numberPicker = new NumberPicker(getContext());
        if(albums != null){
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(albums.length-1);
            numberPicker.setDisplayedValues(albums);
            numberPicker.setValue(0);
        }
        builder.setTitle(R.string.dialog_choose_album)
                .setView(numberPicker)
                .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(albums != null )
                            listener.onAlbumPickerPositiveClick(albums[numberPicker.getValue()]);
                    }
                })
                .setNegativeButton(R.string.cancel,null);
        return builder.create();
    }


}
