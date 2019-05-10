package ja.burhanrashid52.photoeditor;

import android.content.Context;
import android.util.AttributeSet;

import com.theartofdev.edmodo.cropper.CropImageView;

public class CropViewImageView extends CropImageView {
    public CropViewImageView(Context context) {
        super(context);
    }

    public CropViewImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void rotateImage(int degrees) {
//        super.rotateImage(degrees);
    }
}
