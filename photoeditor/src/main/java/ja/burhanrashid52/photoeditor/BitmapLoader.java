package ja.burhanrashid52.photoeditor;

import android.graphics.Bitmap;

public class BitmapLoader {
    public BitmapLoader() {
    }

    public BitmapLoader(Bitmap bitmap){
        this.bitmap = bitmap;
    }
    private Bitmap bitmap;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
