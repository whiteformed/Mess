package com.example.mess.Dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mess.R;

public class LoadingDialog extends DialogFragment {
    public static final String LOADING_TEXT = "loading_text";

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.LoadingDialog);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_loading, null);
        builder.setView(view);
        setCancelable(false);

        Bundle args = getArguments();
        if (args != null) {
            TextView txtLoading = view.findViewById(R.id.txtLoading);
            String loadingText = args.getString(LOADING_TEXT);
            txtLoading.setText(loadingText);
        }

        return builder.create();
    }


    @Override
    public void show(FragmentManager manager, String tag) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }
}
