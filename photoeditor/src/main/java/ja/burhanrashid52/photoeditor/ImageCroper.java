package ja.burhanrashid52.photoeditor;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class ImageCroper {

    public static final int CROP_IMAGE_RESULT = 100;
    public static final String EXTRA_CROP_IMAGE = "image_croper_image";

    private Activity activity;
    private Fragment fragment;
    private String path;

    public ImageCroper(CropBuilder cropBuilder) {
        this.activity = cropBuilder.activity;
        this.fragment = cropBuilder.fragment;
        this.path = cropBuilder.path;

        if (fragment != null) {
            Intent intent = new Intent(fragment.getContext(), CropImageActivity.class);
            intent.putExtra(EXTRA_CROP_IMAGE, path);
            fragment.startActivityForResult(intent, CROP_IMAGE_RESULT);
        }
        if(activity != null){
            Intent intent = new Intent(activity, CropImageActivity.class);
            intent.putExtra(EXTRA_CROP_IMAGE, path);
            activity.startActivityForResult(intent, CROP_IMAGE_RESULT);
        }
    }


    public static class CropBuilder {
        private Activity activity;
        private Fragment fragment;
        private String path;

        public CropBuilder(String path, Activity activity) {
            this.activity = activity;
            this.path = path;
        }

        public CropBuilder(String path, Fragment fragment) {
            this.fragment = fragment;
            this.path = path;
        }

        public ImageCroper start() {
            return new ImageCroper(this);
        }
    }

}
