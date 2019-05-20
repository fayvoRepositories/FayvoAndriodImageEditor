package ja.burhanrashid52.photoeditor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;

import java.lang.ref.WeakReference;

public class TransparentBitmap {

    private static TransparentBitmap transparentBitmap;
    private Bitmap bitmap;
    private WeakReference<Context> context;

    private TransparentBitmap(WeakReference<Context> context){
        this.context = context;
    }

    public static TransparentBitmap getInstance(WeakReference<Context> context){
        if(transparentBitmap == null){
            transparentBitmap = new TransparentBitmap(context);
        }
        return transparentBitmap;
    }

    public Bitmap getTransparentBitmap(){
        if(bitmap != null && !bitmap.isRecycled()){
            return bitmap;
        }
        Display display = ((Activity)context.get()).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmap);
        canvas.setDensity(30);
        canvas.drawColor(Color.parseColor("#01000000"));
        Log.e("Width", "" + width);
        Log.e("height", "" + height);
        Log.e("Bitmap Size =", "" + bitmap.getByteCount());
        return bitmap;
    }
}
