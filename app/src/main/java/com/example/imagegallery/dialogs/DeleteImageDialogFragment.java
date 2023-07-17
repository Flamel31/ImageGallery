package com.example.imagegallery.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.imagegallery.R;

public class DeleteImageDialogFragment extends DialogFragment {

    public interface DeleteImageDialogListener {
        void onDeleteImagePositiveClick();
    }

    // Instance of the interface used to deliver action events
    private DeleteImageDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        if(context instanceof DeleteImageDialogListener){
            listener = (DeleteImageDialogListener)context;
        }
    }

    @Override @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(R.string.dialog_delete_image)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDeleteImagePositiveClick();
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
