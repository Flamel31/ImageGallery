package com.example.imagegallery.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.imagegallery.R;

public class AddAlbumDialogFragment extends DialogFragment {

    // The activity that creates an instance of this dialog fragment must implement this interface
    // in order to receive event callbacks.
    // The method passes the albumName so that the activity can check the validity.
    public interface AddAlbumDialogListener {
        void onDialogPositiveClick(@NonNull String albumName);
    }

    // Instance of the interface used to deliver action events
    private AddAlbumDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        if(context instanceof AddAlbumDialogListener){
            listener = (AddAlbumDialogListener)context;
        }
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        // Edit Text
        final EditText editText = new EditText(getContext());
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setTitle(R.string.dialog_add_album)
                .setView(editText)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDialogPositiveClick(editText.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        // Create the AlertDialog object and return it
        return builder.create();
    }

}
