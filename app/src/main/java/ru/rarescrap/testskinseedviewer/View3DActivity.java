package ru.rarescrap.testskinseedviewer;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import supercraftskins.viewer.BitmapUtils;
import supercraftskins.viewer.MinecraftSteveView;
import supercraftskins.viewer.ViewerConstants;

import static ru.rarescrap.testskinseedviewer.Dialogs.showPermissionDialog;
import static ru.rarescrap.testskinseedviewer.Dialogs.showSaveDialog;
import static supercraftskins.viewer.Constants.REQUEST_PERMISSION_SAVE;

public class View3DActivity extends Activity {
    public static final String EXTRA_FILE = "EXTRA_FILE";
    public static final String EXTRA_FILENAME = "EXTRA_FILENAME";
    public static final String EXTRA_BANNER = "EXTRA_BANNER";
    public static final String EXTRA_AD = "EXTRA_AD";
    public static final String EXTRA_AUTHORITY = "EXTRA_AUTHORITY";
    public String blockTextureFolder = "block_textures/brick.png";
    private ArrayList<HashMap> mPrefixPoses = new ArrayList<>();
    private MinecraftSteveView mMinecraftSteveView;
    private boolean SKIN_OUTER_VISIBLE = true;
    private String filename;
    private ImageView iv_clothes;
    private byte[] bytes;
    private byte[] blockBytes;
    private String screen_3d_id;
    private boolean ad_removed = false;
    private String authority;
    private LinearLayout block_textures_layout;
    private boolean blocks_choose_visibility = false;

    private View.OnClickListener mPoseChangeClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int poseNum = ((Integer) view.getTag());
            mMinecraftSteveView.renderer.setPose(mPrefixPoses.get(poseNum));
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            filename = savedInstanceState.getString(EXTRA_FILENAME);
            bytes = savedInstanceState.getByteArray(EXTRA_FILE);
            screen_3d_id = savedInstanceState.getString(EXTRA_BANNER);
            ad_removed = savedInstanceState.getBoolean(EXTRA_AD, false);
            authority = savedInstanceState.getString(EXTRA_AUTHORITY);
        } else {
            if (getIntent().hasExtra(EXTRA_FILE)) {
                filename = getIntent().getStringExtra(EXTRA_FILENAME);
                bytes = getIntent().getByteArrayExtra(EXTRA_FILE);
                screen_3d_id = getIntent().getStringExtra(EXTRA_BANNER);
                ad_removed = getIntent().getBooleanExtra(EXTRA_AD, false);
                authority = getIntent().getStringExtra(EXTRA_AUTHORITY);
            } else {
                // defaults
                filename = "default";
                loadDefaultSkin();
                loadBlockTexture();
                screen_3d_id = "idk";
                ad_removed = true;
                authority = "authority";
            }
        }

        mMinecraftSteveView = findViewById(R.id.minecraft_steve_view);

        if (bytes == null || bytes.length == 0) {
            finish();
        } else {
            fillPoses();
            Bitmap characterBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap blockBitmap = BitmapFactory.decodeByteArray(blockBytes, 0, blockBytes.length);
            mMinecraftSteveView.setCharacter(characterBitmap, blockBitmap);
            setToolbar();
            makePoseButtons();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putByteArray(EXTRA_FILE, bytes);
        savedInstanceState.putString(EXTRA_FILENAME, filename);
        savedInstanceState.putString(EXTRA_BANNER, screen_3d_id);
        savedInstanceState.putBoolean(EXTRA_AD, ad_removed);
        savedInstanceState.putString(EXTRA_AUTHORITY, authority);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_SAVE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveSkin();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showPermissionDialog(this);
                }
            }
        }
    }

    private void setToolbar() {
        TextView toolbar_name = findViewById(R.id.tv_toolbar_name);
        toolbar_name.setText(filename);
        ImageView iv_save = findViewById(R.id.iv_save);
        ImageView iv_block = findViewById(R.id.iv_block);
        iv_clothes = findViewById(R.id.iv_clothes);
        ImageView iv_back = findViewById(R.id.iv_back);
        ImageView iv_share = findViewById(R.id.iv_share);

        iv_save.setImageDrawable(
                VectorDrawableCompat.create(getResources(), supercraftskins.viewer.R.drawable.ic_save, getTheme()));//TODO:move from viewer package
        if (SKIN_OUTER_VISIBLE) {
            iv_clothes.setImageDrawable(
                    VectorDrawableCompat.create(getResources(), supercraftskins.viewer.R.drawable.ic_clothes_black, getTheme()));
        } else {
            iv_clothes.setImageDrawable(
                    VectorDrawableCompat.create(getResources(), supercraftskins.viewer.R.drawable.ic_clothes, getTheme()));
        }
        iv_back.setImageDrawable(
                VectorDrawableCompat.create(getResources(), supercraftskins.viewer.R.drawable.ic_arrow_back, getTheme()));
        iv_share.setImageDrawable(
                VectorDrawableCompat.create(getResources(), supercraftskins.viewer.R.drawable.ic_share, getTheme()));
        iv_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(View3DActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveSkin();
                } else {
                    ActivityCompat.requestPermissions(View3DActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_SAVE);
                }
            }
        });
        iv_clothes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeClothes();
            }
        });
        iv_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareSkin();
            }
        });

        iv_block.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blocks_choose_visibility = !blocks_choose_visibility;
                if (blocks_choose_visibility)
                    block_textures_layout.setVisibility(View.VISIBLE);
                else
                    block_textures_layout.setVisibility(View.GONE);
            }
        });
        fillBlockTextures();
    }

    private void fillBlockTextures() {
        block_textures_layout = findViewById(R.id.block_choose);
        try {
            final String[] imageNames = getAssets().list("block_textures");
            for (final String filename : imageNames) {
                InputStream ims = getAssets().open("block_textures/" + filename);
                Drawable drawable = Drawable.createFromStream(ims, null);
                ImageButton blockBtn = new ImageButton(this);
                blockBtn.setBackground(drawable);
                blockBtn.setMinimumWidth(100);
                blockBtn.setMinimumHeight(100);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(15, 0, 0, 0);
                blockBtn.setLayoutParams(params);
                ims.close();
                blockBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeBlockTexture(filename);
                    }
                });
                block_textures_layout.addView(blockBtn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shareSkin() {
        if (authority == null) return;

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("image/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File imagesFolder = new File(getCacheDir(), "images");
        Uri uri;
        imagesFolder.mkdirs();
        File file = new File(imagesFolder, "shared_image.png");
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file);
            Bitmap shareImage = mMinecraftSteveView.renderer.getCurrentCapturedBitmap();
            if (shareImage != null) {
                if (!shareImage.isRecycled()) {
                    shareImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    stream.flush();
                    stream.close();
                    uri = FileProvider.getUriForFile(this, authority, file);
                    i.putExtra(Intent.EXTRA_STREAM, uri);
                    try {
                        startActivity(Intent.createChooser(i, getString(supercraftskins.viewer.R.string.share_via)));
                    } catch (ActivityNotFoundException ex) {
                        ex.printStackTrace();
                        Toast.makeText(this, supercraftskins.viewer.R.string.error_save, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, supercraftskins.viewer.R.string.error_save, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, supercraftskins.viewer.R.string.error_save, Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(this, supercraftskins.viewer.R.string.error_save, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSkin() {
        String name = "photo_" + filename;
        Bitmap bitmap = mMinecraftSteveView.getCurrentCapturedImage();
        if (bitmap != null) {
            showSaveDialog(this, name, bitmap, (ViewGroup) findViewById(R.id.main_layout));
        }
    }

    private void changeClothes() {
        if (mMinecraftSteveView == null) return;

        SKIN_OUTER_VISIBLE = !SKIN_OUTER_VISIBLE;
        mMinecraftSteveView.mRenderOptions.isSkinOuterVisible = SKIN_OUTER_VISIBLE;
        if (SKIN_OUTER_VISIBLE) {
            iv_clothes.setImageDrawable(
                    VectorDrawableCompat.create(getResources(), supercraftskins.viewer.R.drawable.ic_clothes_black, getTheme()));
        } else {
            iv_clothes.setImageDrawable(
                    VectorDrawableCompat.create(getResources(), supercraftskins.viewer.R.drawable.ic_clothes, getTheme()));
        }
    }

    public void makePoseButtons() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final LinearLayout layout = findViewById(R.id.pose_buttons);
                if (mPrefixPoses != null) {
                    int i = 0;
                    for (HashMap pose : mPrefixPoses) {
                        final View button = getLayoutInflater().inflate(R.layout.item_pose_button, null);
                        ImageView poseImage = button.findViewById(R.id.pose_image);
                        button.setTag(i);
                        button.setOnClickListener(mPoseChangeClicked);
                        Bitmap btnImage = Bitmap.createBitmap(mMinecraftSteveView.renderer.getCapturedBitmap(pose));

                        int wantedHeight = BitmapUtils.dpToPx(56);
                        int newWidth = btnImage.getWidth() * wantedHeight / btnImage.getHeight();

                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(btnImage, newWidth, wantedHeight, true);
                        btnImage.recycle(); // тут скорее всегобыла утечка ООМ

                        poseImage.setImageBitmap(scaledBitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                layout.addView(button);
                                LinearLayout.LayoutParams params =
                                        (LinearLayout.LayoutParams) button.getLayoutParams();
                                params.weight = 1.0f;
                                params.height = -1;
                                button.setLayoutParams(params);
                            }
                        });
                        i++;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMinecraftSteveView.mRenderOptions.autoRotate = true;
                            View button = layout.findViewWithTag(0);
                            button.performClick();
                        }
                    });
                }
            }
        });

        thread.start();
    }

    private void changeBlockTexture(String filename) {
        blockTextureFolder = "block_textures/" + filename;
        loadBlockTexture();
        Bitmap blockBitmap = BitmapFactory.decodeByteArray(blockBytes, 0, blockBytes.length);
        mMinecraftSteveView.renderer.setBlockTexture(blockBitmap);
    }

    private void fillPoses() {
        HashMap<String, float[]> mPrefixPose = new HashMap<>();
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_R, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_L, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_R, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_L, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_HEAD, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BODY, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BLOCK, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPoses.add(mPrefixPose);
        mPrefixPose = new HashMap<>();
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_R,
                new float[]{0.89056146f, 0.5127468f, 0.11757702f, 0.09538164f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_L,
                new float[]{0.9458451f, 0.2292861f, -0.22526388f, -0.045399264f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_R,
                new float[]{0.7351275f, 0.4914891f, 0.24182235f, 0.39943463f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_L,
                new float[]{0.53189284f, 0.6799792f, -0.4759623f, -0.16786362f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_HEAD, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BODY, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BLOCK, new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        mPrefixPoses.add(mPrefixPose);
        mPrefixPose = new HashMap<>();
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_R,
                new float[]{0.9729554f, -0.17940412f, 0.012184774f, 0.1449948f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_L,
                new float[]{0.97879684f, -0.20475f, 0.0034137974f, 0.0047449935f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_R,
                new float[]{0.37413788f, 0.80091035f, -0.34255487f, 0.31815028f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_L,
                new float[]{0.4397106f, 0.8819804f, -0.1691251f, -0.012722394f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_HEAD,
                new float[]{0.9565596f, 0.2649472f, -0.107496984f, -0.0569301f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BODY,
                new float[]{0.9219871f, -0.29619366f, -0.20858422f, 0.13675456f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BLOCK,
                new float[]{0.9219871f, -0.29619366f, -0.20858422f, 0.13675456f});
        mPrefixPoses.add(mPrefixPose);
        mPrefixPose = new HashMap<>();
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_R,
                new float[]{0.69669676f, 0.7109415f, 0.07648967f, 0.057665322f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_L,
                new float[]{0.7194714f, 0.69347525f, -0.0149209f, 0.0350787f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_R,
                new float[]{0.97088856f, 0.11284793f, -0.14384644f, 0.1547547f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_L,
                new float[]{0.9919256f, 0.08756049f, -0.0017471879f, -0.09172651f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_HEAD,
                new float[]{0.9926856f, -0.12025606f, -0.010507934f, -0.0018267899f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BODY,
                new float[]{0.9855581f, -0.10397133f, 0.13053563f, 0.028736172f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BLOCK,
                new float[]{0.9855581f, -0.10397133f, 0.13053563f, 0.028736172f});
        mPrefixPoses.add(mPrefixPose);
        mPrefixPose = new HashMap<>();
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_R,
                new float[]{0.97877663f, 0.194712f, -0.06340923f, 0.007932508f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_LEG_L,
                new float[]{0.9899411f, -0.13598476f, -0.0386494f, 0.005571018f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_R,
                new float[]{0.7524441f, 0.65814686f, -0.013395989f, 0.02216013f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_ARM_L,
                new float[]{0.80548674f, 0.5923842f, 0.0043073148f, -0.015921872f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_HEAD,
                new float[]{0.9740007f, -0.22080979f, -0.048766166f, -0.013698816f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BODY,
                new float[]{0.9873634f, -0.06602695f, -0.14402421f, -0.003329952f});
        mPrefixPose.put(ViewerConstants.SKIN_PART_BLOCK,
                new float[]{0.9873634f, -0.06602695f, -0.14402421f, -0.003329952f});
        mPrefixPoses.add(mPrefixPose);
    }

    private void loadDefaultSkin() {
        try {
            InputStream inputStream = getAssets().open("Default_Steve_Skin.png");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            bytes = stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBlockTexture() {
        try {
            InputStream inputStream = getAssets().open(blockTextureFolder);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            this.blockBytes = stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}