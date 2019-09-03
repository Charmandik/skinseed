package ru.rarescrap.testskinseedviewer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static supercraftskins.viewer.Constants.SAVED_SKINS;

public class Dialogs {
    public static void showPermissionDialog(final Activity activity) {
        if (activity.isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(supercraftskins.viewer.R.string.permission_msg)
                .setPositiveButton(supercraftskins.viewer.R.string.allow, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.finish();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", activity.getPackageName(), null));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton(supercraftskins.viewer.R.string.refuse, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.setCancelable(false);
        builder.create().show();
    }

    public static void showSaveDialog(final Activity activity, final String filename,
                                      final Bitmap skin_map, ViewGroup parent) {
        if (activity.isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_save, parent, false);
        builder.setView(view);
        builder.setTitle(R.string.dialog_save);
        final EditText editText = view.findViewById(R.id.et_filename);
        editText.setText(filename);
        builder.setNegativeButton(R.string.extwo_n, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (TextUtils.isEmpty(editText.getText().toString())) {
                            editText.setError(activity.getString(R.string.error_field_empty));
                        } else {
                            File root = new File(Environment.getExternalStorageDirectory(), SAVED_SKINS);
                            if (!root.exists()) {
                                root.mkdirs();
                            }
                            String name = editText.getText() + ".png";
                            File file = new File(root, name);
                            FileOutputStream out = null;
                            try {
                                out = new FileOutputStream(file);
                                skin_map.compress(Bitmap.CompressFormat.PNG, 100, out);
                                String result = activity.getString(R.string.skin_saved, file.getAbsolutePath());
                                MediaScannerConnection.scanFile(activity, new String[]{file.getPath()}, null,
                                        null);
                                Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(activity, R.string.error_save, Toast.LENGTH_LONG).show();
                            } finally {
                                try {
                                    if (out != null) {
                                        out.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                dialog.dismiss();
                            }
                        }
                    }
                });
    }
}
