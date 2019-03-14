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

import com.oginotihiro.cropview.CropView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CropImageActivity extends AppCompatActivity implements View.OnClickListener {

    private CropView cropView;
    private ImageView resultIv;
    String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);
        cropView = findViewById(R.id.cropView);
        resultIv = findViewById(R.id.resultIv);
        LinearLayout btnlay = findViewById(R.id.btnlay);
        Button doneBtn = findViewById(R.id.doneBtn);
        Button cancelBtn = findViewById(R.id.cancelBtn);

        doneBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        path = getIntent().getExtras().getString("image");
        Uri source = Uri.parse(path);
        cropView.of(source).asSquare().initialize(CropImageActivity.this);
        cropView.setVisibility(View.VISIBLE);
        btnlay.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.doneBtn) {

            new Thread() {
                public void run() {
                    final Bitmap croppedBitmap = cropView.getOutput();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resultIv.setImageBitmap(croppedBitmap);
                        }
                    });
                    String destination = saveImage(croppedBitmap, ImagePath.getPath(CropImageActivity.this, Uri.parse(path)));
                    Intent intent = new Intent();
                    intent.putExtra("path", destination);
                    setResult(RESULT_OK, intent);
                    finish();
//                    }

                }
            }.start();
        } else if (id == R.id.cancelBtn) {
//            reset();
            onBackPressed();

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
    }

    private String saveImage(Bitmap bitmap, String path) {
        String file = path;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
//you can create a new file name "test.jpg" in sdcard folder.
            File f = new File(file);
            f.createNewFile();
//write the bytes in file
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
// remember close de FileOutput
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
            file = null;
        }
        return file;

    }
}