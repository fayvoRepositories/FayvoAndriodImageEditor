package ja.burhanrashid52.photoeditor;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.oginotihiro.cropview.CropUtil;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static ja.burhanrashid52.photoeditor.ImageCroper.EXTRA_CROP_IMAGE;
import static ja.burhanrashid52.photoeditor.ImageCroper.IMAGE_OUTPUT_PATH;
import static ja.burhanrashid52.photoeditor.ImageCroper.IMAGE_PATH;

public class CropImageActivity extends AppCompatActivity implements View.OnClickListener, CropImageView.OnCropImageCompleteListener,  CropImageView.OnSetImageUriCompleteListener{

    private CropImageView mCropImageView;
    private ImageView resultIv;
    String path;
    String outputPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);
        mCropImageView = findViewById(R.id.cropView);
        resultIv = findViewById(R.id.resultIv);
        LinearLayout btnlay = findViewById(R.id.btnlay);
        Button doneBtn = findViewById(R.id.doneBtn);
        Button cancelBtn = findViewById(R.id.cancelBtn);

        doneBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        path = getIntent().getExtras().getString(IMAGE_PATH);
        outputPath = getIntent().getExtras().getString(IMAGE_OUTPUT_PATH);
        Uri source = Uri.fromFile(new File(path));
        mCropImageView.setImageUriAsync(source);
        mCropImageView.setVisibility(View.VISIBLE);
        btnlay.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.doneBtn) {
            mCropImageView.getCroppedImageAsync();
        } else if (id == R.id.cancelBtn) {
            onBackPressed();

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
    }

    public String savebitmap(Bitmap bitmapImage) {
        Uri destination = Uri.fromFile(new File(outputPath));
       boolean isSave =  CropUtil.saveOutput(CropImageActivity.this, destination, bitmapImage, 90);
       if(isSave)
           return path;
       return outputPath;

    }
    private String saveImage(Bitmap bitmap) {
//        String extension = path.substring(path.lastIndexOf("."));
        File file = new File(outputPath);
        String fileName = file.getAbsolutePath();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes);
            File f = new File(fileName);
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
            fileName = null;
        }
        return fileName;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCropImageView.setOnSetImageUriCompleteListener(this);
        mCropImageView.setOnCropImageCompleteListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCropImageView.setOnSetImageUriCompleteListener(null);
        mCropImageView.setOnCropImageCompleteListener(null);
    }

    @Override
    public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {

    }

    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {

        final Bitmap croppedBitmap = result.getBitmap();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultIv.setImageBitmap(croppedBitmap);
            }
        });
        String destination = savebitmap(croppedBitmap);
//        String destination = saveImage(croppedBitmap);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CROP_IMAGE, destination);
        setResult(RESULT_OK, intent);
        finish();
    }
}