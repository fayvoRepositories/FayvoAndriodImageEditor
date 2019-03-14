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

    private ImageCroper(){

    }
    public static ImageCroper build(String path, Activity activity){
        ImageCroper imageCroper = new ImageCroper();
        imageCroper.path = path;
        imageCroper.activity = activity;
        return imageCroper;
    }

    public static ImageCroper build(String path, Fragment fragment){
        ImageCroper imageCroper = new ImageCroper();
        imageCroper.path = path;
        imageCroper.fragment = fragment;
        return imageCroper;
    }

    public void start(){
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
}
