package ja.burhanrashid52.photoeditor;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class ImageCroper {

    public static final int CROP_IMAGE_RESULT = 100;
    public static final String EXTRA_CROP_IMAGE = "image_croper_image";
    public static final int EXTRA_CROP_BITMAP = 101;
    public static final int EXTRA_CROP_CANCEL = 102;
    public static final String IMAGE_PATH = "image_croper_path";
    public static final String IMAGE_ROTATE_SHOW = "image_rotate";
    public static final String IMAGE_ROTATE_ANGLE = "rotate";
    public static final String IMAGE_CROP_ID = "id";
    public static final String IMAGE_OUTPUT_PATH = "image_crop_path";

    private Activity activity;
    private Fragment fragment;
    private String path;
    private String outputPath;
    private boolean isRotateShow;
    private int rotateAngle;
    private long cropId;

    public ImageCroper(CropBuilder cropBuilder) {
        this.activity = cropBuilder.activity;
        this.fragment = cropBuilder.fragment;
        this.path = cropBuilder.path;
        this.outputPath = cropBuilder.outputPath;
        this.isRotateShow = cropBuilder.isRotateShow;
        this.rotateAngle = cropBuilder.rotateAngle;
        this.cropId = cropBuilder.cropId;

        if (fragment != null) {
            Intent intent = new Intent(fragment.getActivity(), CropImageActivity.class);
            intent.putExtra(IMAGE_PATH, path);
            intent.putExtra(IMAGE_ROTATE_SHOW, isRotateShow);
            intent.putExtra(IMAGE_ROTATE_ANGLE, rotateAngle);
            intent.putExtra(IMAGE_CROP_ID, cropId);
            fragment.startActivityForResult(intent, CROP_IMAGE_RESULT);
        }
        if (activity != null) {
            Intent intent = new Intent(activity, CropImageActivity.class);
            intent.putExtra(IMAGE_PATH, path);
            intent.putExtra(IMAGE_ROTATE_SHOW, isRotateShow);
            intent.putExtra(IMAGE_ROTATE_ANGLE, rotateAngle);
            intent.putExtra(IMAGE_CROP_ID, cropId);
            activity.startActivityForResult(intent, CROP_IMAGE_RESULT);
        }
    }


    public static class CropBuilder {
        private Activity activity;
        private Fragment fragment;
        private String path;
        private String outputPath;
        private boolean isRotateShow = true;
        private int rotateAngle;
        private long cropId;

        public CropBuilder(String path, String outputPath, Activity activity) {
            this.activity = activity;
            this.path = path;
            this.outputPath = outputPath;
        }

        public CropBuilder(String path, String outputPath, Fragment fragment) {
            this.fragment = fragment;
            this.path = path;
            this.outputPath = outputPath;
        }

        public CropBuilder(String path, Activity activity) {
            this.activity = activity;
            this.path = path;
            this.outputPath = "";
        }

        public CropBuilder(String path, Fragment fragment) {
            this.fragment = fragment;
            this.path = path;
            this.outputPath = "";
        }

        public CropBuilder setRotateShow(boolean isRotateShow) {
            this.isRotateShow = isRotateShow;
            return this;
        }

        public CropBuilder setRotateAngle(int rotateAngle) {
            this.rotateAngle = rotateAngle;
            return this;
        }

        public CropBuilder setCropId(long cropId){
            this.cropId = cropId;
            return this;
        }

        public ImageCroper start() {
            return new ImageCroper(this);
        }
    }

}
