package ja.burhanrashid52.photoeditor.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

public class BitmapFilters {

    public enum Filters {
        SEPIA, GREY, NORMAL, BRIGHT, INVERT, MONOCHROME, VIGNETTE;
        private static Filters[] value = values();
        public Filters next() {
            return value[(this.ordinal() + 1) % value.length];
        }

        public Filters previous() {
            if (this.ordinal() == 0) {
                return value[value.length - 1];
            } else {
                return value[((this.ordinal() - 1) % value.length)];
            }
        }
    }

    public static Bitmap getBitmapOnFilterValue(BitmapFilters.Filters bitmapFilter, Bitmap source) {
        Bitmap bitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        switch (bitmapFilter) {
            case SEPIA: {
                return getSepiaScaledBitmap(bitmap);
            }
            case GREY: {
                return getGreyScaledBitmap(bitmap);
            }

            case BRIGHT: {
                return getBrightScaledBitmap(bitmap);
            }
            case INVERT: {
                return invert(bitmap);
            }
            case MONOCHROME: {
                return monoChrome(bitmap);
            }
            case VIGNETTE: {
                return vignette(bitmap);
            }

        }
        return null;
    }

    private static Bitmap getGreyScaledBitmap(Bitmap source) {

        float[] GrayArray = {
                0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
                0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
                0.213f, 0.715f, 0.072f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
        };
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix(GrayArray);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setAntiAlias(true);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(filter);
        Canvas canvas = new Canvas(source);
        canvas.drawBitmap(source, 0, 0, paint);
        return source;
    }

    private static Bitmap getSepiaScaledBitmap(Bitmap source) {
        float[] sepiaArray = {1, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 0.8f, 0, 0,
                0, 0, 0, 1, 0};
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix(sepiaArray);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setAntiAlias(true);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(filter);
        Canvas canvas = new Canvas(source);
        canvas.drawBitmap(source, 0, 0, paint);
        return source;
    }


    private static Bitmap getBrightScaledBitmap(Bitmap source) {
        //contrast 0..10 1 default
        //brightness -255..255 0 is default
        int contrast = 2;
        int brightness = 3;
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Canvas canvas = new Canvas(source);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);

        return source;
    }


    private static Bitmap invert(Bitmap source) {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        -1, 0, 0, 0, 255,
                        0, -1, 0, 0, 255,
                        0, 0, -1, 0, 255,
                        0, 0, 0, 1, 0
                });

        Canvas canvas = new Canvas(source);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);

        return source;
    }

    private static Bitmap monoChrome(Bitmap source) {
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);

        Canvas canvas = new Canvas(source);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);

        return source;
    }

    private static Bitmap vignette(Bitmap source) {

        final int width = source.getWidth();
        final int height = source.getHeight();

        float radius = (float) (width / 1.2);
        int[] colors = new int[]{0, 0x55000000, 0xff000000};
        float[] positions = new float[]{0.0f, 0.5f, 1.0f};
        RadialGradient gradient = new RadialGradient(width / 2, height / 2, radius, colors, positions, Shader.TileMode.CLAMP);
        Canvas canvas = new Canvas(source);
        canvas.drawARGB(1, 0, 0, 0);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setShader(gradient);
        final Rect rect = new Rect(0, 0, source.getWidth(), source.getHeight());
        final RectF rectf = new RectF(rect);
        canvas.drawRect(rectf, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);

        return source;
    }


    private static Bitmap saturation(Bitmap source) {
        Log.d("Filter", "saturation");
        int value = 15;
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        cm.setSaturation((float) (value / 100.0));
        Canvas canvas = new Canvas(source);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);
        return source;
    }
}
