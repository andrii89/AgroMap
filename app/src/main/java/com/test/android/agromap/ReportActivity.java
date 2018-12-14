package com.test.android.agromap;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.test.android.agromap.database.AppDatabase;
import com.test.android.agromap.database.PolygonDao;
import com.test.android.agromap.database.PolygonData;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private static final int REQUEST_PHOTO = 1;

    Button mAddPhotoButton;
    Button mSaveButton;
    TextView mDateTextview;
    private ImageView mImageView;
    private EditText mDescriptionEditText;

    private AppDatabase mDb;

    private String date;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report);

        mDb = AppDatabase.getInstance(getApplicationContext());

        mImageView = findViewById(R.id.photo_image_view);

        Calendar calendar = Calendar.getInstance();
        Date currentViewDate = calendar.getTime();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.GERMAN);
        date = df.format(currentViewDate);

        mDateTextview = findViewById(R.id.date_textView);
        mDateTextview.setText(date);

        mDescriptionEditText = findViewById(R.id.description_edit_text);

        mAddPhotoButton = findViewById(R.id.add_photo_button);
        mAddPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        mSaveButton = findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String description = mDescriptionEditText.getText().toString();
                new insertAsyncTask(mDb.polygonDao()).execute(new PolygonData(description, date, mCurrentPhotoPath));
                finish();
            }
        });

    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = getPhotoFile(getPhotoFilename());
            } catch (IOException ex) {
                Toast.makeText(getApplicationContext(), R.string.IOExcepcion_toast, Toast.LENGTH_LONG).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.test.android.agromap.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_PHOTO);
            }
        }
    }

    public String getPhotoFilename(){
        return "IMG_" + date + "_";
    }

    public File getPhotoFile (String filename) throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(filename, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PHOTO && resultCode == RESULT_OK) {
            setPic();
        }
        galleryAddPic();
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    private static class insertAsyncTask extends AsyncTask<PolygonData, Void, Void> {

        private PolygonDao mAsyncTaskDao;

        insertAsyncTask(PolygonDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final PolygonData... params) {
            mAsyncTaskDao.insertReport(params[0]);
            return null;
        }
    }


}
