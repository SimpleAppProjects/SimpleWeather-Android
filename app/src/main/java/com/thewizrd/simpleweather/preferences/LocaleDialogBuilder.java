package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.thewizrd.simpleweather.R;

import java.util.Locale;

public class LocaleDialogBuilder {
    private Context context;

    public LocaleDialogBuilder(@NonNull final Context context) {
        this.context = context;

        localeAdapter = new BaseAdapter() {
            private String[] locales = context.getResources().getAssets().getLocales();

            @Override
            public int getCount() {
                return locales.length;
            }

            @Override
            public Object getItem(int position) {
                return locales[position];
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView v = (CheckedTextView) convertView;
                if (v == null) {
                    v = (CheckedTextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.alertdialog_singlechoice_material, parent, false);
                }

                String tag = (String) getItem(position);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    v.setText(Locale.forLanguageTag(tag).getDisplayName());
                } else {
                    String[] split = tag.split("_");
                    v.setText(new Locale(split[0], split[1]).getDisplayName());
                }

                return v;
            }
        };
    }

    private BaseAdapter localeAdapter;

    public void show() {
        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Language")
                .setCancelable(true)
                .setSingleChoiceItems(localeAdapter, -1, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
    }
}
