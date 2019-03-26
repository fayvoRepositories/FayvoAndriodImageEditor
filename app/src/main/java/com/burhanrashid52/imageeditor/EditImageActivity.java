package com.burhanrashid52.imageeditor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.ChangeBounds;
import android.support.transition.TransitionManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.burhanrashid52.imageeditor.base.BaseActivity;
import com.burhanrashid52.imageeditor.filters.FilterListener;
import com.burhanrashid52.imageeditor.filters.FilterViewAdapter;
import com.burhanrashid52.imageeditor.tools.EditingToolsAdapter;
import com.burhanrashid52.imageeditor.tools.ToolType;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import ja.burhanrashid52.photoeditor.DragDropOnDragListener;
import ja.burhanrashid52.photoeditor.ImageCroper;
import ja.burhanrashid52.photoeditor.ImagePath;
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.ViewType;
import ja.burhanrashid52.photoeditor.PhotoFilter;

import static ja.burhanrashid52.photoeditor.ImageCroper.CROP_IMAGE_RESULT;
import static ja.burhanrashid52.photoeditor.ImageCroper.EXTRA_CROP_IMAGE;

public class EditImageActivity extends BaseActivity implements OnPhotoEditorListener,
        View.OnClickListener,
        PropertiesBSFragment.Properties,
        EmojiBSFragment.EmojiListener,
        StickerBSFragment.StickerListener, EditingToolsAdapter.OnItemSelected, FilterListener {

    private static final String TAG = EditImageActivity.class.getSimpleName();
    public static final String EXTRA_IMAGE_PATHS = "extra_image_paths";
    private static final int CAMERA_REQUEST = 52;
    private static final int PICK_REQUEST = 53;
    private PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;
    private PropertiesBSFragment mPropertiesBSFragment;
    private EmojiBSFragment mEmojiBSFragment;
    private StickerBSFragment mStickerBSFragment;
    private TextView mTxtCurrentTool;
    private VideoView videoView;
    private Typeface mWonderFont;
    private RecyclerView mRvTools, mRvFilters;
    private EditingToolsAdapter mEditingToolsAdapter = new EditingToolsAdapter(this);
    private FilterViewAdapter mFilterViewAdapter = new FilterViewAdapter(this);
    private ConstraintLayout mRootView;
    private ConstraintSet mConstraintSet = new ConstraintSet();
    private boolean mIsFilterVisible;
    private List<PhotoFilter> filters = new ArrayList<>();

    LinearLayout ivDelete;

    LinearLayout brushLayout;
    ConstraintLayout layoutTop;
    ImageView done;

    String videoPath = "";
    FFmpeg ffmpeg;
    String imagePath = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.activity_edit_image);

        initViews();
        ffmpeg = FFmpeg.getInstance(EditImageActivity.this);
        mWonderFont = Typeface.createFromAsset(getAssets(), "beyond_wonderland.ttf");
        setupFilters();
        mPropertiesBSFragment = new PropertiesBSFragment();
        mEmojiBSFragment = new EmojiBSFragment();
        mStickerBSFragment = new StickerBSFragment();
        mStickerBSFragment.setStickerListener(this);
        mEmojiBSFragment.setEmojiListener(this);

        mPropertiesBSFragment.setPropertiesChangeListener(this);

//        LinearLayoutManager llmTools = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
//        mRvTools.setLayoutManager(llmTools);
//        mRvTools.setAdapter(mEditingToolsAdapter);

        LinearLayoutManager llmFilters = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRvFilters.setLayoutManager(llmFilters);
        mRvFilters.setAdapter(mFilterViewAdapter);


        //Typeface mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium);
        //Typeface mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf");

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                //.setDefaultTextTypeface(mTextRobotoTf)
                //.setDefaultEmojiTypeface(mEmojiTypeFace)
                .build(); // build photo editor sdk
        ivDelete.setOnDragListener(new DragDropOnDragListener(mPhotoEditor));
        mPhotoEditor.setOnPhotoEditorListener(this);
        loadFFMpegBinary();
        //Set Image Dynamically
        // mPhotoEditorView.getSource().setImageResource(R.drawable.color_palette);

    }

    private void loadFFMpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
        }
    }


    private void setupFilters() {
        filters.add(PhotoFilter.NONE);
        filters.add(PhotoFilter.AUTO_FIX);
        filters.add(PhotoFilter.BRIGHTNESS);
        filters.add(PhotoFilter.CONTRAST);
        filters.add(PhotoFilter.DOCUMENTARY);
        filters.add(PhotoFilter.DUE_TONE);
        filters.add(PhotoFilter.FILL_LIGHT);
        filters.add(PhotoFilter.FISH_EYE);
        filters.add(PhotoFilter.GRAIN);
        filters.add(PhotoFilter.GRAY_SCALE);
        filters.add(PhotoFilter.LOMISH);
        filters.add(PhotoFilter.NEGATIVE);
        filters.add(PhotoFilter.POSTERIZE);
        filters.add(PhotoFilter.SATURATE);
        filters.add(PhotoFilter.SEPIA);
        filters.add(PhotoFilter.SHARPEN);
        filters.add(PhotoFilter.TEMPERATURE);
        filters.add(PhotoFilter.TINT);
        filters.add(PhotoFilter.VIGNETTE);
        filters.add(PhotoFilter.CROSS_PROCESS);
        filters.add(PhotoFilter.BLACK_WHITE);
    }
    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
//                    addTextViewToLayout("FAILED with output : "+s);
                    Log.d("ffmpeg onFailure", s);
                }

                @Override
                public void onSuccess(String s) {
//                    addTextViewToLayout("SUCCESS with output : "+s);
                    Log.d("ffmpeg onSuccess", s);
                    videoView.setVideoPath(videoPath);
                    videoView.start();
                }

                @Override
                public void onProgress(String s) {
                    Log.d("ffmpeg onProgress", s);
//                    addTextViewToLayout("progress : "+s);
//                    progressDialog.setMessage("Processing\n"+s);
                }

                @Override
                public void onStart() {
//                    outputLayout.removeAllViews();

                    Log.d("ffmpeg onStart", "Started command : ffmpeg " + command);
//                    progressDialog.setMessage("Processing...");
//                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d("ffmpeg onFinish", "Finished command : ffmpeg " + command);
//                    progressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    @SuppressLint("MissingPermission")
    private void initViews() {
        ImageView imgUndo;
        ImageView btnUndo;
        ImageView imgRedo;
        ImageView imgCamera;
        ImageView imgGallery;
        ImageView imgSave;
        ImageView imgClose;
        ImageView imgBrush;
        ImageView imgText;
        ImageView imgEmoji;
        ImageView imgSticker;
        ImageView icCrop;

        RecyclerView rvColor = findViewById(R.id.rvColors);

        brushLayout = findViewById(R.id.brushLayout);
        layoutTop = findViewById(R.id.layoutTop);
        icCrop = findViewById(R.id.icCrop);
        icCrop.setOnClickListener(this);
        done = findViewById(R.id.done);
        btnUndo = findViewById(R.id.btnUndo);
        done.setOnClickListener(this);
        btnUndo.setOnClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvColor.setLayoutManager(layoutManager);
        rvColor.setHasFixedSize(true);
        ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(this);
        colorPickerAdapter.setOnColorPickerClickListener(new ColorPickerAdapter.OnColorPickerClickListener() {
            @Override
            public void onColorPickerClickListener(int colorCode) {
                /*if (mProperties != null) {
                    dismiss();
                    mProperties.onColorChanged(colorCode);
                }*/
                mPhotoEditor.setBrushColor(colorCode);
            }
        });
        rvColor.setAdapter(colorPickerAdapter);

        mPhotoEditorView = findViewById(R.id.photoEditorView);
        mTxtCurrentTool = findViewById(R.id.txtCurrentTool);
        mRvTools = findViewById(R.id.rvConstraintTools);
        mRvFilters = findViewById(R.id.rvFilterView);
        mRootView = findViewById(R.id.rootView);
        videoView = findViewById(R.id.videoView);
        String path = "android.resource://" + getPackageName() + "/" + R.raw.sample;
//        videoView.setVideoURI(Uri.parse(path));
//        videoView.start();

        imgBrush = findViewById(R.id.ivBrush);
        imgBrush.setOnClickListener(this);

//        imgText = findViewById(R.id.ivText);
//        imgText.setOnClickListener(this);
//        mPhotoEditorView.setOnClickListener(this);
        setSwipeListener(mPhotoEditorView);
        imgEmoji = findViewById(R.id.ivEmoji);
        imgEmoji.setOnClickListener(this);
        imgSticker = findViewById(R.id.ivSticker);
        imgSticker.setOnClickListener(this);

        ivDelete = findViewById(R.id.ivDelete);


        imgUndo = findViewById(R.id.imgUndo);
        imgUndo.setOnClickListener(this);

        imgRedo = findViewById(R.id.imgRedo);
        imgRedo.setOnClickListener(this);

        imgCamera = findViewById(R.id.imgCamera);
        imgCamera.setOnClickListener(this);

        imgGallery = findViewById(R.id.imgGallery);
        imgGallery.setOnClickListener(this);

        imgSave = findViewById(R.id.imgSave);
        imgSave.setOnClickListener(this);

        imgClose = findViewById(R.id.imgClose);
        imgClose.setOnClickListener(this);

    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode, int size) {
        TextEditorDialogFragment textEditorDialogFragment =
                TextEditorDialogFragment.show(this, text, colorCode);
        textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
            @Override
            public void onDone(String inputText, int colorCode, int size) {
                mPhotoEditor.editText(rootView, inputText, colorCode, size);
                mTxtCurrentTool.setText(R.string.label_text);
            }
        });
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onAddViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onRemoveViewListener(int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener() called with: numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.icCrop:
                new ImageCroper.CropBuilder(imagePath, this).start();
                break;
            case R.id.done:
                slideDown(brushLayout);
                break;
            case R.id.btnUndo:
                mPhotoEditor.undo();
                break;

            case R.id.imgUndo:
                mPhotoEditor.undo();
                break;

            case R.id.imgRedo:
                mPhotoEditor.redo();
                break;

            case R.id.imgSave:
                saveImage();
                break;

            case R.id.imgClose:
                onBackPressed();
                break;

            case R.id.imgCamera:
//                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
                break;

            case R.id.imgGallery:
                /*Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST);*/
                new MaterialFilePicker()
                        .withActivity(this)
                        .withRequestCode(PICK_REQUEST)
//                        .withFilter(Pattern.compile(".*\\.mp4$")) // Filtering files and directories by file name using regexp
                        .withFilterDirectories(true) // Set directories filterable (false by default)
                        .withHiddenFiles(true) // Show hidden files and folders
                        .start();
                break;
            case R.id.ivBrush:
                mPhotoEditor.setBrushDrawingMode(true);
                mTxtCurrentTool.setText(R.string.label_brush);
                slideUp(brushLayout);
//                mPropertiesBSFragment.show(getSupportFragmentManager(), mPropertiesBSFragment.getTag());
                break;

            case R.id.photoEditorView:
                mPhotoEditor.setBrushDrawingMode(false);
                TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this);
                textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
                    @Override
                    public void onDone(String inputText, int colorCode, int size) {
                        mPhotoEditor.addText(inputText, colorCode, ivDelete, size);
                        mTxtCurrentTool.setText(R.string.label_text);
                    }
                });
                break;
            case R.id.ivEmoji:
                mEmojiBSFragment.show(getSupportFragmentManager(), mEmojiBSFragment.getTag());
                break;
            case R.id.ivSticker:
                mPhotoEditor.setBrushDrawingMode(false);
                mStickerBSFragment.show(getSupportFragmentManager(), mStickerBSFragment.getTag());
                break;
        }
    }

    @SuppressLint("MissingPermission")
    private void saveImage() {
        if (true) {
            if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showLoading("Saving...");
                File file = new File(Environment.getExternalStorageDirectory()
                        + File.separator + ""
                        + System.currentTimeMillis() + ".png");
                try {
                    file.createNewFile();

                    SaveSettings saveSettings = new SaveSettings.Builder()
                            .setClearViewsEnabled(true)
                            .setTransparencyEnabled(true)
                            .build();

                    mPhotoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                        @Override
                        public void onSuccess(@NonNull String imagePath) {
                            hideLoading();



                            showSnackbar("Image Saved Successfully");
                            mPhotoEditorView.getSource().setImageURI(Uri.fromFile(new File(imagePath)));

                            String outputPath = videoPath.replace(".mp4", "new.mp4");

                            String[] cmd = new String[]{"-y", "-i", videoPath, "-i",
                                    imagePath,
                                    "-preset", "ultrafast",
                                    "-filter_complex",
                                    "[1][0]scale2ref[i][m];[m][i]overlay[v]", "-map",
                                    "[v]", "-map", "0:a?", "-ac", "2", outputPath};



//                          String[] cmd  = command.split(" ");
                            Log.d("ffmpeg commond ", Arrays.toString(cmd));
                            if (cmd.length != 0) {
                                execFFmpegBinary(cmd);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            hideLoading();
                            showSnackbar("Failed to save Image");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    hideLoading();
                    showSnackbar(e.getMessage());
                }
            }
        } else {
            showLoading("Saving...");
            mPhotoEditor.saveAsBitmap(new OnSaveBitmap() {
                @Override
                public void onBitmapReady(Bitmap saveBitmap) {
                    hideLoading();
                    showSnackbar("Image Saved Successfully");
                    mPhotoEditorView.getSource().setImageBitmap(saveBitmap);
                }

                @Override
                public void onFailure(Exception e) {

                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

            switch (requestCode) {
                case CROP_IMAGE_RESULT:
                    try {
                        mPhotoEditor.clearAllViews();
                        String uri = data.getStringExtra(EXTRA_CROP_IMAGE);
                        imagePath = ImagePath.getPath(this, Uri.fromFile(new File(uri)));
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(new File(uri)));
                        mPhotoEditorView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case CAMERA_REQUEST:
                    mPhotoEditor.clearAllViews();
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    mPhotoEditorView.setImageBitmap(photo);
                    break;
                case PICK_REQUEST:
                    /*try {
                        mPhotoEditor.clearAllViews();
                        Uri uri = data.getData();
                        imagePath = ImagePath.getPath(this, uri);
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        mPhotoEditorView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/

                    videoPath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
                    Drawable myDrawable = getResources().getDrawable(R.drawable.transparent);
                    Bitmap anImage = ((BitmapDrawable) myDrawable).getBitmap();

                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(videoPath);
                    int width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    int height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    retriever.release();

                    Bitmap bitmap = getResizedBitmap(anImage, width, height);
                    mPhotoEditorView.getSource().setImageBitmap(bitmap);

//                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    videoView.setVideoPath(videoPath);
                    videoView.start();

                    break;
            }
        }


    }

    @Override
    public void onColorChanged(int colorCode) {
        mPhotoEditor.setBrushColor(colorCode);
        mTxtCurrentTool.setText(R.string.label_brush);
    }

    @Override
    public void onBrushSizeChanged(int brushSize) {
        mPhotoEditor.setBrushSize(brushSize);
        mTxtCurrentTool.setText(R.string.label_brush);
    }

    @Override
    public void onEmojiClick(String emojiUnicode) {
        mPhotoEditor.addEmoji(emojiUnicode, ivDelete);
        mTxtCurrentTool.setText(R.string.label_emoji);

    }

    @Override
    public void onStickerClick(Bitmap bitmap) {
        mPhotoEditor.addImage(bitmap, ivDelete);
        mTxtCurrentTool.setText(R.string.label_sticker);
    }

    @Override
    public void isPermissionGranted(boolean isGranted, String permission) {
        if (isGranted) {
            saveImage();
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you want to exit without saving image ?");
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveImage();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNeutralButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.create().show();

    }

    @Override
    public void onFilterSelected(PhotoFilter photoFilter) {
        mPhotoEditor.setFilterEffect(photoFilter);
    }

    @Override
    public void onToolSelected(ToolType toolType) {
        switch (toolType) {
            case BRUSH:
                mPhotoEditor.setBrushDrawingMode(true);
                mTxtCurrentTool.setText(R.string.label_brush);
                mPropertiesBSFragment.show(getSupportFragmentManager(), mPropertiesBSFragment.getTag());
                break;
            case TEXT:
                setAddTextEvent();
                break;
            case EMOJI:
                mEmojiBSFragment.show(getSupportFragmentManager(), mEmojiBSFragment.getTag());
                break;
            case STICKER:
                mStickerBSFragment.show(getSupportFragmentManager(), mStickerBSFragment.getTag());
                break;
            case ERASER:
                mPhotoEditor.brushEraser();
                mTxtCurrentTool.setText(R.string.label_eraser);
                break;
            case FILTER:
                mTxtCurrentTool.setText(R.string.label_filter);
                showFilter(true);
                break;

        }
    }


    void showFilter(boolean isVisible) {
        mIsFilterVisible = isVisible;
        mConstraintSet.clone(mRootView);

        if (isVisible) {
            mConstraintSet.clear(mRvFilters.getId(), ConstraintSet.START);
            mConstraintSet.connect(mRvFilters.getId(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START);
            mConstraintSet.connect(mRvFilters.getId(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END);
        } else {
            mConstraintSet.connect(mRvFilters.getId(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.END);
            mConstraintSet.clear(mRvFilters.getId(), ConstraintSet.END);
        }

        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(350);
        changeBounds.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
        TransitionManager.beginDelayedTransition(mRootView, changeBounds);

        mConstraintSet.applyTo(mRootView);
    }

    @Override
    public void onBackPressed() {
        if (mIsFilterVisible) {
            showFilter(false);
            mTxtCurrentTool.setText(R.string.app_name);
        } else if (!mPhotoEditor.isCacheEmpty()) {
            showSaveDialog();
        } else {
            super.onBackPressed();
        }
    }


    // slide the view from below itself to the current position
    public void slideUp(View view) {
        layoutTop.setVisibility(View.GONE);
        done.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                view.getHeight(),  // fromYDelta
                0);                // toYDelta
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    // slide the view from its current position to below itself
    public void slideDown(View view) {
        layoutTop.setVisibility(View.VISIBLE);
        done.setVisibility(View.GONE);
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                0,                 // fromYDelta
                view.getHeight()); // toYDelta
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.GONE);
        view.clearAnimation();
    }

    private int filterIndex = 0;

    @SuppressLint("ClickableViewAccessibility")
    private void setSwipeListener(PhotoEditorView ivThumbnail) {
        ivThumbnail.setOnTouchListener(new OnSwipeTouchListener(this) {

            @Override
            public void onSwipeLeft() {
                filterIndex--;
                if (filterIndex < 0) {
                    filterIndex = filters.size() - 1;
                }
                mPhotoEditor.setFilterEffect(filters.get(filterIndex));
            }

            @Override
            public void onSwipeRight() {
                filterIndex++;
                if (filterIndex >= filters.size()) {
                    filterIndex = 0;
                }
                mPhotoEditor.setFilterEffect(filters.get(filterIndex));
            }

            @Override
            public void onTouch() {
                setAddTextEvent();
            }
        });
    }

    public void setAddTextEvent() {
        mPhotoEditor.setBrushDrawingMode(false);
        TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this);
        textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
            @Override
            public void onDone(String inputText, int colorCode, int size) {
                mPhotoEditor.addText(inputText, colorCode, ivDelete, size);
                mTxtCurrentTool.setText(R.string.label_text);
            }
        });
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap output = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix m = new Matrix();
        m.setScale((float) newWidth / bm.getWidth(), (float) newHeight / bm.getHeight());
        canvas.drawBitmap(bm, m, new Paint());

        return output;
    }

    public void savebitmap(Bitmap bmp, String filePath) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 60, bytes);
        File f = new File(filePath);
//        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();

    }

}
