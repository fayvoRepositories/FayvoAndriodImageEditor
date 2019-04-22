package com.fayvo.ui.main.camera.postpreview.mediapager.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.fayvo.BR;
import com.fayvo.R;
import com.fayvo.data.model.api.ApiError;
import com.fayvo.data.model.others.GalleryMedia;
import com.fayvo.databinding.ActivityPostPreviewBinding;
import com.fayvo.databinding.FragmentMediaPreviewBinding;
import com.fayvo.ui.base.BaseFragment;
import com.fayvo.ui.main.camera.postpreview.PostPreviewActivity;
import com.fayvo.ui.main.camera.postpreview.mediapager.fragment.burshEditor.PropertiesBSFragment;
import com.fayvo.ui.main.camera.postpreview.mediapager.fragment.saveImage.ImageSave;
import com.fayvo.ui.main.camera.postpreview.mediapager.fragment.stickerEditor.StickerBSFragment;
import com.fayvo.ui.main.camera.postpreview.mediapager.fragment.textEditor.TextEditorActivity;
import com.fayvo.utils.AppLogger;
import com.fayvo.utils.BitmapUtil;
import com.fayvo.utils.CommonUtils;
import com.fayvo.utils.MediaLoader;
import com.fayvo.utils.MessageAlert;
import com.fayvo.utils.PathUtil;
import com.fayvo.utils.RequestCodes;
import com.fayvo.utils.enums.PostEditing;
import com.fayvo.utils.gesture.OnSwipeTouchListener;
import com.fayvo.utils.interfaces.TaskListener;
import com.fayvo.utils.tags.CommonTags;
import com.fayvo.utils.tags.PostEditingTags;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import eu.inloop.localmessagemanager.LocalMessage;
import eu.inloop.localmessagemanager.LocalMessageCallback;
import eu.inloop.localmessagemanager.LocalMessageManager;
import ja.burhanrashid52.photoeditor.ImageCroper;
import ja.burhanrashid52.photoeditor.MultiTouchListener;
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.ViewType;

import static android.app.Activity.RESULT_OK;
import static com.fayvo.utils.BitmapUtil.applyOverlayOnImage;
import static ja.burhanrashid52.photoeditor.ImageCroper.EXTRA_CROP_BITMAP;

public class MediaPreviewFragment extends BaseFragment<FragmentMediaPreviewBinding, MediaPreviewViewModel>
        implements MediaPreviewFragmentNavigator, OnPhotoEditorListener,
        PropertiesBSFragment.Properties, StickerBSFragment.StickerListener,
        MultiTouchListener.DragDeleteListener {

    public final String TAG = MediaPreviewFragment.class.getSimpleName();
    public FragmentMediaPreviewBinding mFragmentMediaPreviewBinding;
    public boolean isStickerShow = false;
    public boolean isTextEditorShow = false;
    @Inject
    MediaPreviewViewModel mMediaPreviewViewModel;

    private GalleryMedia galleryMedia;
    private PhotoEditor photoEditor;
    private StickerBSFragment stickerBSFragment;
    private List<PhotoFilter> filters = new ArrayList<>();
    private LinearLayout delete;
    private ProgressDialog mProgressDialog;

    private Bitmap transparentBitmap;
    private View textRootView;
    private boolean isVideoPrepared;

    public static MediaPreviewFragment newInstance(GalleryMedia galleryMedia) {
        Bundle args = new Bundle();
        MediaPreviewFragment fragment = new MediaPreviewFragment();
        args.putParcelable(CommonTags.MODEL, galleryMedia);
        fragment.setArguments(args);
        return fragment;
    }

    private static void refreshGallery(String mCurrentPhotoPath, Context context) {
        if (context != null) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(mCurrentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);
        }
    }

    @Override
    public int getBindingVariable() {
        return BR.viewModel;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_media_preview;
    }

    @Override
    public MediaPreviewViewModel getViewModel() {
        return mMediaPreviewViewModel;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaPreviewViewModel.setNavigator(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFragmentMediaPreviewBinding = getViewDataBinding();
        if (getActivity() instanceof PostPreviewActivity) {
            delete = ((PostPreviewActivity) Objects.requireNonNull(getActivity())).getDeleteLayout();
        }

        if (getArguments() != null && getArguments().containsKey(CommonTags.MODEL))
            galleryMedia = getArguments().getParcelable(CommonTags.MODEL);

        if (galleryMedia != null) {
            mMediaPreviewViewModel.setGalleryMedia(galleryMedia);
            if (!galleryMedia.isVideo()) {
                mFragmentMediaPreviewBinding.videoView.setVisibility(View.GONE);
                mFragmentMediaPreviewBinding.videoProgressBar.setVisibility(View.GONE);
            }
            String mediaPath = galleryMedia.isVideo() || galleryMedia.getCroppedImage() == null ? galleryMedia.getPath() : galleryMedia.getCroppedImage();
            MediaLoader.loadMediaDrawableNoThumbnailWithCallbacks(mFragmentMediaPreviewBinding.ivThumbnail, mediaPath, new TaskListener<Drawable>() {
                @Override
                public void onResponse(Drawable response) {
                    if (!galleryMedia.isVideo()) {
                        mFragmentMediaPreviewBinding.ivThumbnail.setImageDrawable(response);
                        //Bitmap bitmap = ((BitmapDrawable) response).getBitmap();
                    } else {
                        mFragmentMediaPreviewBinding.viewEditorHolder.setVisibility(View.VISIBLE);
                    }

                    AppLogger.d("usm_media_preview", "MediaLoader: onResponse: path= " + galleryMedia.getPath());
                    //mFragmentMediaPreviewBinding.ivThumbnail.setImageDrawable(response);
                    setViewObserverForAnim(mFragmentMediaPreviewBinding.ivThumbnail);
                }

                @Override
                public void onError(ApiError apiError) {
                    AppLogger.d("usm_media_preview", "MediaLoader: onError: path= " + galleryMedia.getPath());

                    //startPostponedEnterTransition();
                    try {
                        if (getBaseActivity() instanceof PostPreviewActivity && ((PostPreviewActivity) getBaseActivity()).isTransitionPending()) {
                            ((PostPreviewActivity) getBaseActivity()).isTransitionPlayed = true;
                            ActivityCompat.startPostponedEnterTransition(getBaseActivity());
                        }
                    } catch (Exception ignore) {
                    }

                    if (galleryMedia.isVideo()) {
                        mFragmentMediaPreviewBinding.viewEditorHolder.setVisibility(View.VISIBLE);
                    }
                }
            });

            setTransparentOverlay();
            setOverlayEditor();

        }
    }

    private void setOverlayEditor() {

        if(photoEditor == null) {
            photoEditor = new PhotoEditor.Builder(getActivity(),
                    (PhotoEditorView) mFragmentMediaPreviewBinding.viewEditorHolder.getChildAt(0))
                    .setPinchTextScalable(true) // set flag to make text scalable when pinch
                    .setdragDeleteListener(this)
                    .setDeleteView(delete)
                    .build();

        }else{
            photoEditor.addAllView((PhotoEditorView) mFragmentMediaPreviewBinding.viewEditorHolder.getChildAt(0));
        }
        getPostPreviewActivity().addPhotoEditor(photoEditor);
        photoEditor.setOnPhotoEditorListener(MediaPreviewFragment.this);
        stickerBSFragment = new StickerBSFragment();
        stickerBSFragment.setStickerListener(MediaPreviewFragment.this);
//        if (delete != null) {
//            delete.setOnDragListener(new DragDropOnDragListener(photoEditor));
//        }
        setupFilters();
        setSwipeListener(photoEditor.getPhotoEditorView());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setSwipeListener(PhotoEditorView ivThumbnail) {
        ivThumbnail.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {

            @Override
            public void onSwipeLeft() {
                /*filterIndex--;
                if (filterIndex < 0) {
                    filterIndex = filters.size() - 1;
                }
                photoEditor.setFilterEffect(filters.get(filterIndex));*/
            }

            @Override
            public void onSwipeRight() {
               /* filterIndex++;
                if (filterIndex >= filters.size()) {
                    filterIndex = 0;
                }
                photoEditor.setFilterEffect(filters.get(filterIndex));*/
            }

            @Override
            public void onTouch() {
                if (!photoEditor.isBurshEnable()) {
                    showTextEditor(null);
                }

            }
        });
    }

    @Override
    public void setVideoPlayer() {
        try {
            if (TextUtils.isEmpty(mMediaPreviewViewModel.filePath()))
                return;
            mFragmentMediaPreviewBinding.videoView.setDataSource(mMediaPreviewViewModel.filePath());
            mFragmentMediaPreviewBinding.videoView.setLooping(true);
            mFragmentMediaPreviewBinding.videoView.prepareAsync(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    isVideoPrepared = true;

                    AppLogger.d("usm_media_preview", "onPrepared is called ");
                    updateVideoPlayState(getUserVisibleHint());
                    getViewModel().showProgressBar.set(false);

                }
            });
            mFragmentMediaPreviewBinding.videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {

                    AppLogger.d("usm_media_preview_video", "onError " +
                            " ErrorCodes i : " + i + " i1 : " + i1);
                    mFragmentMediaPreviewBinding.videoProgressBar.setVisibility(View.GONE);

                    if (getUserVisibleHint())
                        MessageAlert.showErrorMessage(getViewDataBinding().getRoot(), "Failed to play video. Unknown Error has Occurred.");
                    return false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTransparentOverlay() {
        photoEditor = getPostPreviewActivity().getCurrentPhotoEditor(galleryMedia.getId());
        if(photoEditor != null){
//            this.photoEditor = photoEditor;
            PhotoEditorView photoEditorView  = photoEditor.getPhotoEditorView();
            mFragmentMediaPreviewBinding.viewEditorHolder.removeAllViews();
            if(photoEditorView.getParent() != null) {
                ((ViewGroup)photoEditorView.getParent()).removeView(photoEditorView); // <- fix
            }
            mFragmentMediaPreviewBinding.viewEditorHolder.addView(photoEditorView);
            if(photoEditorView.getParent() != null) {
                ((ViewGroup)photoEditorView.getParent()).removeView(photoEditorView); // <- fix
            }
//            photoEditorView.removeAllViews();
            photoEditor.addAllView(photoEditorView);

        }else {
            PhotoEditorView photoEditorView = (PhotoEditorView)
                    getLayoutInflater().inflate(R.layout.photo_editor, (ViewGroup) getView(), false);
//                    getActivity().getLayoutInflater().inflate(R.layout.photo_editor, null);
            if (getActivity() != null) {
                if (transparentBitmap == null) {
                    int width, height;
                    if (getActivity() != null) {
                        if (getActivity().getWindowManager() != null) {
                            Display display = getActivity().getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            width = size.x;
                            height = size.y;
                            transparentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
                            Canvas canvas = new Canvas(transparentBitmap);
                            canvas.setDensity(80);
                            canvas.drawColor(Color.parseColor("#01000000"));
                            Log.e("Width", "" + width);
                            Log.e("height", "" + height);
                            Log.e("Bitmap Size =", "" + transparentBitmap.getByteCount());
                            photoEditorView.getSource().setImageBitmap(transparentBitmap);
                            mFragmentMediaPreviewBinding.viewEditorHolder.addView(photoEditorView);
                        }
                    }

                }
            }
        }
    }


    @Override
    public void onDestroy() {
        if (transparentBitmap != null) {
            transparentBitmap.recycle();
        }
        if (galleryMedia != null) {
            galleryMedia.setCroppedImage(null);
            galleryMedia.setOverlayImage(null);

            if (galleryMedia.isVideo()) {
                mFragmentMediaPreviewBinding.videoView.release();
            }

        }

        super.onDestroy();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        AppLogger.d("usm_media_preview", "setUserVisibleHint: isVisibleToUser= " + isVisibleToUser + " ,isVideoPrepared= " + isVideoPrepared);

        if (isResumed() && mMediaPreviewViewModel.isVideo() && isVideoPrepared) {
            updateVideoPlayState(isVisibleToUser);
        }
    }

    private void updateVideoPlayState(boolean isVisibleToUser) {
        AppLogger.d("usm_media_preview", "updateVideoPlayState: isVisibleToUser= " + isVisibleToUser);
        if (mFragmentMediaPreviewBinding != null)
            if (isVisibleToUser && !mFragmentMediaPreviewBinding.videoView.isPlaying()) {
                mFragmentMediaPreviewBinding.videoView.start();
            } else if (!isVisibleToUser && mFragmentMediaPreviewBinding.videoView.isPlaying()) {
                mFragmentMediaPreviewBinding.videoView.pause();
            }
    }

    @Override
    public void onResume() {
        super.onResume();


        if (mMediaPreviewViewModel.isVideo() && getUserVisibleHint() && isVideoPrepared)
            updateVideoPlayState(getUserVisibleHint());

    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMediaPreviewViewModel.isVideo() && mFragmentMediaPreviewBinding != null && mFragmentMediaPreviewBinding.videoView.isPlaying())
            mFragmentMediaPreviewBinding.videoView.pause();

    }

    public void updateDrawingLayoutVisibility(boolean show) {
        if (show)
            mMediaPreviewViewModel.setPostEditingType(PostEditing.DRAWING);

    }

    @Override
    public void undoDrawing() {
        photoEditor.undo();
    }

    @Override
    public void saveDrawing() {
        updateDrawingLayoutVisibility(false);
    }

    @Override
    public void updateParentToolsOptions(boolean show) {
        if (getActivity() instanceof PostPreviewActivity)
            if (show)
                getPostPreviewActivity().showToolsOptions();
            else
                getPostPreviewActivity().hideToolsOptions();
    }

    @Override
    public void showLoader() {
        mProgressDialog = CommonUtils.showLoadingDialog(getActivity(), false);
    }

    @Override
    public void hideLoader() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
        }
    }

    public boolean isVideo() {
        return galleryMedia.isVideo();
    }

    public void setSharedElementViews(int position) {

        if (getBaseActivity().getSharedElementCallback() != null) {
            getBaseActivity().getSharedElementCallback().setCurrentPosition(position);
            getBaseActivity().getSharedElementCallback().setSharedViews(getViewDataBinding().ivThumbnail);
        } else {
            ActivityCompat.startPostponedEnterTransition(getBaseActivity());
        }
    }

    private void setViewObserverForAnim(View view) {
        if (getActivity() instanceof PostPreviewActivity && getPostPreviewActivity().isTransitionPending()) {
            getPostPreviewActivity().isTransitionPlayed = true;
            view.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            view.getViewTreeObserver().removeOnPreDrawListener(this);
                            ActivityCompat.startPostponedEnterTransition(getBaseActivity());
                            return true;
                        }
                    });
        }
    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode, int size) {
        if (isEditingDisable()) {

            textRootView = rootView;
            rootView.setVisibility(View.GONE);
            Bundle bundle = new Bundle();
            bundle.putString(PostEditingTags.TEXT, text);
            bundle.putInt(PostEditingTags.TEXT_COLOR, colorCode);
            bundle.putInt(PostEditingTags.TEXT_SIZE, size);
            bundle.putBoolean(PostEditingTags.EDIT_EXISTING_TEXT, true);

            showTextEditor(bundle);
        }
    }

    public void showTextEditor(Bundle bundle) {
        if (isEditingDisable()) {
            if (getActivity() instanceof PostPreviewActivity && !((PostPreviewActivity) getActivity()).isLocked()) {
                getBaseActivity().lockTouch(true);
                getPostPreviewActivity().hideViewForDrawing();
                isTextEditorShow = true;
                photoEditor.setBrushDrawingMode(false);
                Intent intent = TextEditorActivity.newIntent(getContext());
                if (bundle != null)
                    intent.putExtras(bundle);
                startActivityForResult(intent, RequestCodes.CODE_TEXT_EDITOR);
                getActivity().overridePendingTransition(0, 0);

            }
        }
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
        if (ViewType.BRUSH_DRAWING == viewType) {
            if (getActivity() instanceof PostPreviewActivity) {
                getPostPreviewActivity().undoVisibility();
                getPostPreviewActivity().showHideDrawingLayout(true);
            }
        }
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
        if (ViewType.BRUSH_DRAWING == viewType) {
            if (getActivity() instanceof PostPreviewActivity)
                getPostPreviewActivity().showHideDrawingLayout(false);
        }
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [" + viewType + "]");
    }


    @SuppressLint("MissingPermission")
    public void saveImage(boolean isSave) {
//        GalleryMedia galleryMedia = mMediaPreviewViewModel.galleryMedia.get();
        if (!photoEditor.isEditingNotApplied()) {
            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(false)
                    .build();
            photoEditor.saveAsBitmap(saveSettings, new OnSaveBitmap() {
                @Override
                public void onBitmapReady(Bitmap saveBitmap) {
                    File overlayFile = saveBitmap(saveBitmap);
                    AppLogger.d("Image saved ", "Yes Saved successfully");
                    if (overlayFile != null && overlayFile.exists())
                        galleryMedia.setOverlayImage(overlayFile.getAbsolutePath());
//  mMediaPreviewViewModel.galleryMedia.get().setOverlayImage(path);
                    if (isSave) {
                        if (getActivity() instanceof ImageSave)
                            ((ImageSave) getActivity()).saveImage();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                }
            });
        } else {
            if (isSave) {
                if (getActivity() instanceof ImageSave)
                    ((ImageSave) getActivity()).saveImage();
            }
        }
    }

    public boolean isEditing() {
        return isDrawing() || isStickerShow || isTextEditorShow;
    }

    public boolean isEditingDisable() {
        return !isStickerShow && !isDrawingEnable() && !isTextEditorShow && !(getActivity() instanceof PostPreviewActivity && ((PostPreviewActivity) getActivity()).isSharedTransitionPending);
    }

    @SuppressLint("MissingPermission")
    public void saveFileInGallery(boolean isVideo) {

        GalleryMedia galleryMedia = mMediaPreviewViewModel.galleryMedia.get();

        boolean shouldReturn = !(getActivity() instanceof PostPreviewActivity) || galleryMedia == null;
        if (shouldReturn) {
            return;
        }
        if (!photoEditor.isEditingNotApplied()) {
            photoEditor.saveAsBitmap(new OnSaveBitmap() {
                @Override
                public void onBitmapReady(Bitmap saveBitmap) {
                    if (!galleryMedia.isVideo()) {
                        Bitmap back;
                        try {

                            String mediaPath = galleryMedia.isVideo() || galleryMedia.getCroppedImage() == null ? galleryMedia.getPath() : galleryMedia.getCroppedImage();

                            back = MediaStore.Images.Media.getBitmap(getActivity().
                                    getContentResolver(), Uri.fromFile(new File(mediaPath)));
                            Bitmap bitmap = applyOverlayOnImage(back, saveBitmap);
                            String path = getOutputFilePath(galleryMedia.getPath());
                            String result = BitmapUtil.savebitmap(bitmap, path);
                            getPostPreviewActivity().showInformationMessage(getString(R.string.message_save_media_success));
                            refreshGallery(result, getActivity());
                            refreshAndroidGallery(Uri.parse(result));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        File overlayFile = saveBitmap(saveBitmap);
                        if (getActivity() instanceof PostPreviewActivity && overlayFile != null) {
                            String output = getOutputFilePath(galleryMedia.getPath());
                            getPostPreviewActivity().execFFmpegBinary(galleryMedia.getPath(), overlayFile.getAbsolutePath(), output, isVideo, new TaskListener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    if (getActivity() != null) {
                                        refreshGallery(output, getActivity());
                                        refreshAndroidGallery(Uri.parse(output));
                                        getPostPreviewActivity().lockTouch(false);
                                    }
                                }

                                @Override
                                public void onError(ApiError apiError) {
                                    AppLogger.d("post_preview_ffmpeg onFailure", "Error");
                                    getPostPreviewActivity().lockTouch(false);
                                }

                            });
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    //hideLoader();
                }

                private String getOutputFilePath(String filePath) {

                    File src = new File(filePath);

                    // save edited media in Fayvo external folder and if file isn't created somehow then
                    // save renamed file in the same directory where is source media is.
                    String fileName = src.getName();
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));

                    String fileExtension = CommonUtils.getFileExtension(filePath);

                    File destination = PathUtil.getFilePath(getActivity(), false, fileName + "_edited_" + System.currentTimeMillis(), fileExtension);

                    // AppLogger.d("usm_media_preview_save_edited_1", "fileName= " + fileName + " ,fileExtension= " + fileExtension);

                    if (destination != null) {
                        //   AppLogger.d("usm_media_preview_save_edited_1", "destination= " + destination.getPath() + " absolutePath= " + destination.getAbsolutePath());
                        return destination.getPath();
                    } else {
                        String extension = filePath.substring(filePath.lastIndexOf("."));
                        return filePath.replace(extension, "_edited_" + System.currentTimeMillis() + extension);
                    }
                }
            });
        } else {

            String mediaPath = galleryMedia.isVideo() || galleryMedia.getCroppedImage() == null ? galleryMedia.getPath() : galleryMedia.getCroppedImage();

            // make copy of the file in the Fayvo folder as editing is not applied.
            mMediaPreviewViewModel.saveCopyOfFile(mediaPath, new TaskListener<String>() {
                @Override
                public void onResponse(String response) {
                    getPostPreviewActivity().showInformationMessage(getString(R.string.message_save_media_success));

                    if (response != null && getBaseActivity() != null) {
                        refreshGallery(response, getBaseActivity());
                        refreshAndroidGallery(Uri.parse(response));
                    }
                }

                @Override
                public void onError(ApiError apiError) {
                    getPostPreviewActivity().showInformationMessage(getString(R.string.message_save_media_failure));
                }
            });

        }
    }

    private File saveBitmap(Bitmap bitmapImage) {
        File file = PathUtil.getInternalUploadFilePath(getContext(), PathUtil.OVERLAY_IMAGE + "_" + System.currentTimeMillis(), ".png");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 90, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    private String saveBitmap(Bitmap bitmapImage, String path) {
        File file = new File(path);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
//                bitmapImage.recycle();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }

    @SuppressLint("MissingPermission")
    public void saveVideo(boolean isSave) {
        if (!photoEditor.isEditingNotApplied()) {
            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(false)
                    .build();
            photoEditor.saveAsBitmap(saveSettings, new OnSaveBitmap() {
                @Override
                public void onBitmapReady(Bitmap saveBitmap) {
                    File overlayFile = saveBitmap(saveBitmap);
                    AppLogger.d("Image saved ", "Yes Saved successfully");

                    if (overlayFile != null)
                        galleryMedia.setOverlayImage(overlayFile.getAbsolutePath());
//                    mMediaPreviewViewModel.galleryMedia.get().setOverlayImage(overlayFile.getAbsolutePath());
                    if (isSave) {
                        if (getActivity() instanceof ImageSave)
                            ((ImageSave) getActivity()).saveImage();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    hideLoader();
                }
            });
        } else {
            if (isSave) {
                if (getActivity() instanceof ImageSave)
                    ((ImageSave) getActivity()).saveImage();
            }
        }

    }

    public void showEmojiDialog() {
        isStickerShow = true;
        photoEditor.setBrushDrawingMode(false);
        if (stickerBSFragment.isAdded()) {
            return;
        } else {
            stickerBSFragment.show(getActivity().getSupportFragmentManager(), stickerBSFragment.getTag());
        }
    }

    public void showBush() {
        hideKeyboard();
        photoEditor.setBrushDrawingMode(true);
    }

    @Override
    public void onColorChanged(int colorCode) {
        photoEditor.setBrushColor(colorCode);
    }

    @Override
    public void onBrushSizeChanged(int brushSize) {
        photoEditor.setBrushSize(brushSize);
    }

    public int getBurshColor() {
        return photoEditor.getBrushColor();
    }

    public void setBurshColor(int colorCode) {
        photoEditor.setBrushColor(colorCode);
    }

    public float getBrushSize() {
        return photoEditor.getBrushSize();
    }

    public void disableDrawing() {
        photoEditor.onStopDrawing();
        photoEditor.setBrushDrawingMode(false);
    }

    public boolean isDrawing() {
        return photoEditor != null && photoEditor.isBurshEnable();
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

    @Override
    public void onStickerClick(Bitmap bitmap) {
        isStickerShow = false;
        photoEditor.addImage(bitmap, delete);
    }

    @Override
    public void stickerDismiss() {
        isStickerShow = false;
    }

    public void cropImage() {
        LocalMessageManager.getInstance().addListener(new LocalMessageCallback() {
            @Override
            public void handleMessage(@NonNull LocalMessage localMessage) {
                if (localMessage.getId() == EXTRA_CROP_BITMAP) {
                    if (localMessage.getObject() != null) {
                        Bitmap bitmap = (Bitmap) localMessage.getObject();

                        String extension = CommonUtils.getFileExtension(galleryMedia.getPath());
                        File destination = PathUtil.createCroppedFilePath(PathUtil.CROP_IMAGE + "_" + System.currentTimeMillis(), extension);
                        //AppLogger.d("usm_cropped_media", "path= " + destination.getAbsolutePath());
                        mMediaPreviewViewModel.saveBitmapAsFile(destination, bitmap, extension.toLowerCase().contains("png"), new TaskListener<File>() {
                            @Override
                            public void onResponse(File file) {

                                if (file != null) {

                                    AppLogger.d("usm_mediaUpload_cropped", "filePath= " + file.getAbsolutePath() + " ,space= " + file.getTotalSpace()
                                            + " ,canRead= " + file.canRead() + " ,canWrite= " + file.canWrite() + " ,length= " + file.length()
                                    );

                                    Glide.with(getBaseActivity()).load(file.getAbsoluteFile()).into(mFragmentMediaPreviewBinding.ivThumbnail);
                                    // mFragmentMediaPreviewBinding.ivThumbnail.setImageBitmap(bitmap);
                                    galleryMedia.setCroppedImage(file.getAbsolutePath());
                                }
                            }

                            @Override
                            public void onError(ApiError apiError) {

                            }
                        });
                    }
                }
                LocalMessageManager.getInstance().removeListener(this);
            }
        });

        if (getPostPreviewActivity().hasGalleryPermission())
            new ImageCroper.CropBuilder(galleryMedia.getPath(), this).start();
        else {
            getPostPreviewActivity().requestGalleryPermission(new TaskListener<Boolean>() {
                @Override
                public void onResponse(Boolean response) {
                    new ImageCroper.CropBuilder(galleryMedia.getPath(), MediaPreviewFragment.this).start();
                }

                @Override
                public void onError(ApiError apiError) {

                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {
            case RequestCodes.CODE_TEXT_EDITOR:
                if (resultCode == RESULT_OK) {
                    boolean editExistingText = data.getBooleanExtra(PostEditingTags.EDIT_EXISTING_TEXT, false);
                    String inputText = data.getStringExtra(PostEditingTags.TEXT);
                    int textColorCode = data.getIntExtra(PostEditingTags.TEXT_COLOR, Color.BLACK);
                    int textSize = data.getIntExtra(PostEditingTags.TEXT_SIZE, 40);


                    if (!editExistingText)
                        photoEditor.addText(inputText, textColorCode, delete, textSize);
                    else {
                        if (textRootView != null) {
                            textRootView.setVisibility(View.VISIBLE);
                            photoEditor.editText(textRootView, inputText, textColorCode, textSize);
                        }
                    }
                } else {
                    if (textRootView != null) {
                        textRootView.setVisibility(View.VISIBLE);
                    }
                }
                isTextEditorShow = false;
                if (getPostPreviewActivity() != null) {
                    getPostPreviewActivity().showEditorIcon();
                }
                break;
        }


    }

    private PostPreviewActivity getPostPreviewActivity() {
        return ((PostPreviewActivity) getActivity());
    }

    public boolean hasDrawing() {
        return !photoEditor.hasDrawing();
    }


    public void refreshAndroidGallery(Uri fileUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(fileUri);
            if (getActivity() != null) {
                getBaseActivity().sendBroadcast(mediaScanIntent);
            }
        } else {
            if (getActivity() != null) {
                getBaseActivity().sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_MOUNTED,
                        Uri.parse("file://" + Environment.getExternalStorageDirectory())));
            }
        }
    }

    public boolean isDrawingEnable() {
        if (photoEditor == null)
            return false;
        return photoEditor.isBurshEnable();
    }

    @Override
    public void onMove() {
        ((ActivityPostPreviewBinding) getPostPreviewActivity().getViewDataBinding()).ivDelete.setVisibility(View.GONE);
    }

    @Override
    public void onTouch() {
        ((ActivityPostPreviewBinding) getPostPreviewActivity().getViewDataBinding()).ivDelete.setVisibility(View.GONE);
    }

    @Override
    public void onDragStop() {
        ((ActivityPostPreviewBinding) getPostPreviewActivity().getViewDataBinding()).ivDelete.setVisibility(View.VISIBLE);
    }

}
