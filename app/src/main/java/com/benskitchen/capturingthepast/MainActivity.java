package com.benskitchen.capturingthepast;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import info.androidhive.fontawesome.FontDrawable;
import capturingthepast.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int REQUEST_CODE_OPEN_DIRECTORY = 2;
    private static final int REQUEST_ACTION_OPEN_DOCUMENT_TREE = 3;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private String catRef = "";
    private String strRef = "";
    private String strItem = "";
    private String strSubItem = "";
    private String strPart = "";
    private String strArchon = "GB0000";
    private String strPrefix = "cpast";
    private String strNote = "";
    private String strLogFilename =  "CapturingThePastLog.csv";
    private int nCaptureCounter = 0;
    private int nCurrentRepo = 0;
    boolean bTimestamped = true;
    private String[] repos;
    private JSONArray repositories;
    private ArrayList<String> recentFiles = new ArrayList<String>();
    JSONArray recentFileStore = new JSONArray();
    char[] alphabet = new char[26];
    int nPart = 0;
    private String params = "ArchonParams.json";
    //private String paramsCSV = "CtpLog.csv";
    private JSONObject preferences;
    private String currentFolderPath = "";
    //private File currentPhoto;
    private String currentPhotoPath;
    private Spinner dropdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       // Log.i("Content ", " on create " );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dropdown = findViewById(R.id.spinnerRepo);
        final EditText tvCatRef = (EditText) findViewById(R.id.editTextRef);
        final EditText tvItemText = (EditText) findViewById(R.id.editTextItem);
        final EditText tvSubItemText = (EditText) findViewById(R.id.editTextSubItem);
        final EditText tvDetached = (EditText) findViewById(R.id.textViewPart);
        final TextView refText = (TextView) findViewById(R.id.textViewRef);
        final TextView noteText = (TextView) findViewById(R.id.textViewNote);
        final TextView repoLabel = (TextView) findViewById(R.id.repoLabel);
        final TextView refLabel = (TextView) findViewById(R.id.refLabel);
        final TextView itemLabel = (TextView) findViewById(R.id.itemLabel);
        final TextView subitemLabel = (TextView) findViewById(R.id.subItemLabel);
        final TextView detachedLabel = (TextView) findViewById(R.id.detachedLabel);
        //final TextView repoPresetNote = (TextView) findViewById(R.id.repository_presets_note);cl
        Button decItem = (Button) findViewById(R.id.buttonDecItem);
        Button incItem = (Button) findViewById(R.id.buttonincItem);
        Button decSubItem = (Button) findViewById(R.id.buttonDecSubItem);
        Button incSubItem = (Button) findViewById(R.id.buttonincSubItem);
        Button decPart = (Button) findViewById(R.id.buttonDecPart);
        Button incDetached = (Button) findViewById(R.id.buttonIncPart);
        Button camButton = (Button) findViewById(R.id.cameraButton);
        Button filesButton = (Button) findViewById(R.id.filesButton);
        Button addRepoButton = (Button) findViewById(R.id.addRepoButton);
        Button deleteRepoButton = (Button) findViewById(R.id.deleteRepoButton);
        Button infoButton = (Button) findViewById(R.id.infoButton);
        final Button btnClearNote = (Button) findViewById(R.id.buttonClearNote);
        final Button btnClearRef = (Button) findViewById(R.id.buttonClearRef);
        FontDrawable drawable = new FontDrawable(this, R.string.fa_paper_plane_solid, true, false);
        drawable.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        currentFolderPath = storageDir.getAbsolutePath();
        int i = 0;
        for (char letter = 'a'; letter <= 'z'; letter++) {
            alphabet[i++] = letter;
        }
        //writeArchons();
        readJsonData(params);
        try {
            strArchon = repositories.getJSONObject(dropdown.getSelectedItemPosition()).getString("Archon");

            //Log.i("Content ", "create pos "+ nCurrentRepo+ " " +strArchon );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    strArchon = repositories.getJSONObject(position).getString("Archon");
                    nCurrentRepo = position;
                    //Log.i("Content ", "Current pos "+ nCurrentRepo);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                refText.setText(createCatRef());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        repoLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEntryTips(getString(R.string.repo_description_heading), getString(R.string.repo_description_text));
            }
        });
        refLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEntryTips(getString(R.string.ref_description_heading), getString(R.string.ref_description_text));
            }
        });
        itemLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEntryTips(getString(R.string.item_description_heading), getString(R.string.item_descript_text));
            }
        });
        subitemLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEntryTips(getString(R.string.sub_item_description_heading), getString(R.string.sub_item_description_text));
            }
        });
        detachedLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEntryTips(getString(R.string.detached_description_heading), getString(R.string.detached_description_text));
            }
        });
        decItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int n = Integer.parseInt(tvItemText.getText().toString());
                    if (n > 2) {
                        tvItemText.setText("" + (n - 1));
                    } else {
                        tvItemText.setText("");
                    }
                } catch (NumberFormatException e) {
                    tvItemText.setText("");
                }
            }
        });
        incItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int n = Integer.parseInt(tvItemText.getText().toString());
                    tvItemText.setText("" + (n + 1));
                } catch (NumberFormatException e) {
                    if (tvItemText.getText().toString().equals("")) {
                        tvItemText.setText("" + 1);
                    } else {
                        tvItemText.setText("" + 1);
                    }
                }
            }
        });
        decSubItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int n = Integer.parseInt(tvSubItemText.getText().toString());
                    if (n > 2) {
                        tvSubItemText.setText("" + (n - 1));
                    } else {
                        tvSubItemText.setText("");
                    }
                } catch (NumberFormatException e) {
                    if (tvSubItemText.getText().toString().equals("")) {
                        tvSubItemText.setText("");
                    } else {
                        tvSubItemText.setText("" + 1);
                    }
                }
            }
        });
        incSubItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int n = Integer.parseInt(tvSubItemText.getText().toString());
                    tvSubItemText.setText("" + (n + 1));
                } catch (NumberFormatException e) {
                    if (tvSubItemText.getText().toString().equals("")) {
                        tvSubItemText.setText("" + 1);
                    } else {
                        tvSubItemText.setText("" + 1);
                    }
                }
            }
        });
        decPart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nPart--;
                if (nPart < 1) {
                    nPart = 0;
                    strPart = "";
                    tvDetached.setText(strPart);
                } else if (nPart < alphabet.length) {
                    strPart = "" + alphabet[nPart - 1];
                    tvDetached.setText(strPart);
                } else {
                    strPart = alphabet[nPart % alphabet.length] + ":" + nPart / alphabet.length;
                    tvDetached.setText(strPart);
                }
                refText.setText(createCatRef());
            }
        });
        btnClearNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noteText.setText("");
            }
        });
        btnClearRef.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvCatRef.setText("");
            }
        });
        incDetached.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nPart++;
                if (nPart < 1) {
                    strPart = "";
                    tvDetached.setText(strPart);
                } else if (nPart < alphabet.length) {
                    strPart = "" + alphabet[nPart - 1];
                    tvDetached.setText("" + alphabet[nPart - 1]);
                } else {
                    strPart = alphabet[nPart % alphabet.length] + ":" + nPart / alphabet.length;
                    tvDetached.setText(strPart);
                }
                refText.setText(createCatRef());
            }
        });
        tvDetached.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                strPart = tvDetached.getText().toString();
                refText.setText(createCatRef());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        tvCatRef.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                strRef = tvCatRef.getText().toString();
                refText.setText(createCatRef());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        tvItemText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                strItem = tvItemText.getText().toString();
                if (!strItem.equals("")) {
                    try {
                        Integer.parseInt(strItem);
                    } catch (Exception e) {
                        showMessage("This control accepts numeric input. All other characters are removed");
                        strItem = strItem.replaceAll("[^\\d.]", "");
                        tvItemText.setText(strItem);
                        tvItemText.setSelection(tvItemText.length());
                    }
                }
                refText.setText(createCatRef());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        tvSubItemText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                strSubItem = tvSubItemText.getText().toString();
                if (!strSubItem.equals("")) {
                    try {
                        Integer.parseInt(strSubItem);
                    } catch (Exception e) {
                        strSubItem = strSubItem.replaceAll("[^\\d.]", "");
                        tvSubItemText.setText(strSubItem);
                        tvSubItemText.setSelection(tvSubItemText.length());
                        showMessage("This control accepts numeric input. All other characters are removed");
                    }
                }
                refText.setText(createCatRef());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        noteText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                strNote = noteText.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        camButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.i("Content ", "Cam ");
                dispatchTakePictureIntent();
            }
        });

        filesButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                try {
                    //openFolder();
                    openGallery();
                } catch (Exception e) {
                  //  Log.e("Content", "Folder does not exist!", e);
                }
            }

        });
        infoButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onClick(View v) {
                showInfo();
            }
        });
        addRepoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRepo();
            }
        });
        deleteRepoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteRepoGui();
            }
        });
    }

    private void showMessage(String str) {
        Toast.makeText(this, str, LENGTH_SHORT).show();
    }

    private void showEntryTips(String heading, String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        TextView tvTip = new TextView(this);
        String tipText = "<h4>" + heading + "</h4><p>" + message + "</p>";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvTip.setMovementMethod(LinkMovementMethod.getInstance());
            tvTip.setText(Html.fromHtml(tipText, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvTip.setMovementMethod(LinkMovementMethod.getInstance());
            tvTip.setText(Html.fromHtml(tipText));
        }
        LinearLayout lpset = new LinearLayout(this);
        lpset.setOrientation(LinearLayout.VERTICAL);
        lpset.addView(tvTip);
        lpset.setPadding(50, 80, 50, 10);
        alertDialog.setView(lpset);
        alertDialog.setNegativeButton("CLose", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "content://media/internal/images/media"));
        startActivity(intent);
    }

    public void openFolder() throws FileNotFoundException {

        StorageManager sm = (StorageManager) this.getSystemService(Context.STORAGE_SERVICE);
        Intent intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
        //String startDir = "Android";
        //String startDir = "Download"; // Not choosable on an Android 11 device
        //String startDir = "DCIM";
        //  String startDir = "DCIM/Camera";  // replace "/", "%2F"
        String startDir = "Pictures" + File.separator + "CapturingThePast";
        Uri uri = intent.getParcelableExtra("android.provider.extra.INITIAL_URI");
        String scheme = uri.toString();
        scheme = scheme.replace("/root/", "/document/");
        scheme += "%3A" + startDir;
        uri = Uri.parse(scheme);
        intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        ((Activity) this).startActivityForResult(intent, REQUEST_ACTION_OPEN_DOCUMENT_TREE);
        return;

    }

    private String createCatRef() {
        catRef = strArchon;
        if (strRef.length() > 0) {
            catRef += "/" + strRef;
        }
        if (strItem.length() > 0) {
            catRef += "/" + strItem;
        }
        if (strSubItem.length() > 0) {
            catRef += "/" + strSubItem;
        }
        if (strPart.length() > 0) {
            catRef += strPart;
        }
        catRef = catRef.replaceAll("\\s+", "").toUpperCase();
        catRef = catRef.replaceAll("//", "/");
        catRef = catRef.replaceAll("/", "_");
        catRef = catRef.replaceAll("[!@#$%^&*]", "_");
        catRef = catRef.replaceAll("\\\\\\\\", "\\\\");
        catRef = catRef.replaceAll("\\\\", "_");
        if (catRef.equals("GB0000")) {
            catRef = "Ref";
        }
        catRef = catRef.replaceAll("_{2,}", "_");
        if (catRef.length() > 128) {
            Toast.makeText(this, "Your catalogue reference is very long and may result in unusable file names.", LENGTH_SHORT).show();
        }
        return catRef;
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private boolean dispatchTakePictureIntent() {
       // Log.i("Content ", "CamIntent ");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.benskitchen.capturingthepast.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            try {
                ExifInterface exif = new ExifInterface(currentPhotoPath);
               // String model = exif.getAttribute(ExifInterface.TAG_MODEL);
               // Log.i("Content ", "Image metadata for############################### " + currentPhotoPath + "  " + model);
                saveImageToGallery(bitmap, exif);
                dropdown.setSelection(nCurrentRepo);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, "temp.jpg");
        currentFolderPath = storageDir.getAbsolutePath();
        currentPhotoPath = image.getAbsolutePath();
        //Log.i("Content: ", "Image Legacy " + currentPhotoPath);
        return image;
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd[' ']['T'][H:mm[:ss[.S]]][X]");

    private void saveImageToGallery(Bitmap bitmap, ExifInterface exif) {
        OutputStream fos;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String humanisedTime = "" + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(LocalDateTime.now());
        String imageFileName = strPrefix + "_" + timeStamp + "_" + catRef + ".jpg";
        String strCSV = "\"" + humanisedTime + "\",\"" + catRef + "\",\"" + imageFileName + "\",\"" + strNote + "\"";
        writePublicLog(strCSV);
//        boolean bSaved = false;
//        try {
//                ContentResolver contentResolver = getContentResolver();
//                ContentValues contentValues = new ContentValues();
//                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName);
//                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
//                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "CapturingThePast");
//                Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//                fos = contentResolver.openOutputStream(Objects.requireNonNull(imageUri));
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//                Objects.requireNonNull(fos);
//                int n = nCaptureCounter + 1;
//                setCaptureCounter(n, catRef);
//                String strToastMessage = "Image saved";
//                Toast.makeText(this, strToastMessage, LENGTH_SHORT).show();
//                bSaved = true;
//        } catch (Exception e) {
//            Toast.makeText(this, "Image not saved\n" + e.getMessage(), LENGTH_SHORT).show();
//        }
//        if(bSaved){
//
//        }
//////////////////////////////////////////***/

//        ContentValues values = new ContentValues();
//        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
//        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
//        values.put(MediaStore.MediaColumns.DATE_ADDED, now);
//        values.put(MediaStore.MediaColumns.DATE_MODIFIED, now);
//        ContentResolver resolver = context.getContentResolver();
        ////////////////////////*
        ///Uri uri = resolver.insert( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values );


        ContentResolver contentResolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "CapturingThePast");
        Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);


        try (ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(imageUri, "w")) {
            FileDescriptor fd = pfd.getFileDescriptor();

            try (OutputStream stream = new FileOutputStream(fd)) {
                // Perform operations on "stream".
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            }

            // Synch data with disk. It's mandatory to be able later to call writeExif
            fd.sync();    // <---- HERE THE SOLUTION
            int n = nCaptureCounter + 1;
            setCaptureCounter(n, catRef);
            String strToastMessage = "Image saved";
            Toast.makeText(this, strToastMessage, LENGTH_SHORT).show();
            writeExif(imageUri, exif);

        } catch (IOException e) {
            Toast.makeText(this, "Image not saved\n" + e.getMessage(), LENGTH_SHORT).show();
            e.printStackTrace();
        }


    }



    private void writeExif(Uri uri, ExifInterface exif) {
//        String model = exif.getAttribute(ExifInterface.TAG_MODEL);
//        Log.i("Content ", "Image metadata for############################### " + currentPhotoPath + "  " + model);

        try (ParcelFileDescriptor imagePfd = getContentResolver().openFileDescriptor(uri, "rw")) {
            ExifInterface exifNew = new ExifInterface(imagePfd.getFileDescriptor());
            final String userComment = "Capturing the Past image " +catRef+ " " + strNote;
            exifNew.setAttribute(ExifInterface.TAG_F_NUMBER, exif.getAttribute(ExifInterface.TAG_F_NUMBER));
            exifNew.setAttribute(ExifInterface.TAG_APERTURE_VALUE, exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE));
            exifNew.setAttribute(ExifInterface.TAG_ARTIST, exif.getAttribute(ExifInterface.TAG_ARTIST));
            exifNew.setAttribute(ExifInterface.TAG_BITS_PER_SAMPLE, exif.getAttribute(ExifInterface.TAG_BITS_PER_SAMPLE));
            exifNew.setAttribute(ExifInterface.TAG_BRIGHTNESS_VALUE, exif.getAttribute(ExifInterface.TAG_BRIGHTNESS_VALUE));
            exifNew.setAttribute(ExifInterface.TAG_CFA_PATTERN, exif.getAttribute(ExifInterface.TAG_CFA_PATTERN));
            exifNew.setAttribute(ExifInterface.TAG_COLOR_SPACE, exif.getAttribute(ExifInterface.TAG_COLOR_SPACE));
            //exifNew.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, exif.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION));
            exifNew.setAttribute(ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL, exif.getAttribute(ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL));
            exifNew.setAttribute(ExifInterface.TAG_COMPRESSION, exif.getAttribute(ExifInterface.TAG_COMPRESSION));
            exifNew.setAttribute(ExifInterface.TAG_CONTRAST, exif.getAttribute(ExifInterface.TAG_CONTRAST));
            exifNew.setAttribute(ExifInterface.TAG_COPYRIGHT, exif.getAttribute(ExifInterface.TAG_COPYRIGHT));
            exifNew.setAttribute(ExifInterface.TAG_CUSTOM_RENDERED, exif.getAttribute(ExifInterface.TAG_CUSTOM_RENDERED));
            exifNew.setAttribute(ExifInterface.TAG_DATETIME, exif.getAttribute(ExifInterface.TAG_DATETIME));
            exifNew.setAttribute(ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION, exif.getAttribute(ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION));
            exifNew.setAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO, exif.getAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO));
            exifNew.setAttribute(ExifInterface.TAG_DNG_VERSION, exif.getAttribute(ExifInterface.TAG_DNG_VERSION));
            exifNew.setAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE));
            //exifNew.setAttribute(ExifInterface.TAG_EXIF_VERSION, exif.getAttribute(ExifInterface.TAG_EXIF_VERSION));
            exifNew.setAttribute(ExifInterface.TAG_EXPOSURE_INDEX, exif.getAttribute(ExifInterface.TAG_EXPOSURE_INDEX));
            exifNew.setAttribute(ExifInterface.TAG_EXPOSURE_MODE, exif.getAttribute(ExifInterface.TAG_EXPOSURE_MODE));
            exifNew.setAttribute(ExifInterface.TAG_EXPOSURE_PROGRAM, exif.getAttribute(ExifInterface.TAG_EXPOSURE_PROGRAM));
            exifNew.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME));
            exifNew.setAttribute(ExifInterface.TAG_RECOMMENDED_EXPOSURE_INDEX, exif.getAttribute(ExifInterface.TAG_RECOMMENDED_EXPOSURE_INDEX));
            exifNew.setAttribute(ExifInterface.TAG_FILE_SOURCE, exif.getAttribute(ExifInterface.TAG_FILE_SOURCE));
            exifNew.setAttribute(ExifInterface.TAG_FLASH, exif.getAttribute(ExifInterface.TAG_FLASH));
           // exifNew.setAttribute(ExifInterface.TAG_FLASHPIX_VERSION, exif.getAttribute(ExifInterface.TAG_FLASHPIX_VERSION));
            exifNew.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH));
            exifNew.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM));
            exifNew.setAttribute(ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT, exif.getAttribute(ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT));
            exifNew.setAttribute(ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION, exif.getAttribute(ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION));
            exifNew.setAttribute(ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION, exif.getAttribute(ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION));
            exifNew.setAttribute(ExifInterface.TAG_GAIN_CONTROL, exif.getAttribute(ExifInterface.TAG_GAIN_CONTROL));
            exifNew.setAttribute(ExifInterface.TAG_GAMMA, exif.getAttribute(ExifInterface.TAG_GAMMA));
            exifNew.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
            exifNew.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF));
            exifNew.setAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION, exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DEST_BEARING, exif.getAttribute(ExifInterface.TAG_GPS_DEST_BEARING));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE, exif.getAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE_REF, exif.getAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE_REF));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE, exif.getAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE_REF, exif.getAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE_REF));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DEST_BEARING_REF, exif.getAttribute((ExifInterface.TAG_GPS_DEST_BEARING_REF)));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE, exif.getAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE_REF, exif.getAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE_REF));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DOP, exif.getAttribute(ExifInterface.TAG_GPS_DOP));
            exifNew.setAttribute(ExifInterface.TAG_GPS_DIFFERENTIAL, exif.getAttribute(ExifInterface.TAG_GPS_DIFFERENTIAL));
            exifNew.setAttribute(ExifInterface.TAG_GPS_H_POSITIONING_ERROR, exif.getAttribute(ExifInterface.TAG_GPS_H_POSITIONING_ERROR));
            exifNew.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION));
            exifNew.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF));
            exifNew.setAttribute(ExifInterface.TAG_GPS_MAP_DATUM, exif.getAttribute(ExifInterface.TAG_GPS_MAP_DATUM));
            exifNew.setAttribute(ExifInterface.TAG_GPS_MEASURE_MODE, exif.getAttribute(ExifInterface.TAG_GPS_MEASURE_MODE));
            exifNew.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD));
            exifNew.setAttribute(ExifInterface.TAG_GPS_SATELLITES, exif.getAttribute(ExifInterface.TAG_GPS_SATELLITES));
            exifNew.setAttribute(ExifInterface.TAG_GPS_SPEED, exif.getAttribute((ExifInterface.TAG_GPS_SPEED)));
            exifNew.setAttribute(ExifInterface.TAG_GPS_SPEED_REF, exif.getAttribute(ExifInterface.TAG_GPS_SPEED_REF));
            exifNew.setAttribute(ExifInterface.TAG_GPS_STATUS, exif.getAttribute(ExifInterface.TAG_GPS_STATUS));
            exifNew.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP));
            exifNew.setAttribute(ExifInterface.TAG_GPS_TRACK, exif.getAttribute(ExifInterface.TAG_GPS_TRACK));
            exifNew.setAttribute(ExifInterface.TAG_GPS_TRACK_REF, exif.getAttribute(ExifInterface.TAG_GPS_TRACK_REF));
            exifNew.setAttribute(ExifInterface.TAG_GPS_VERSION_ID, exif.getAttribute(ExifInterface.TAG_GPS_VERSION_ID));
            exifNew.setAttribute(ExifInterface.TAG_ISO_SPEED, exif.getAttribute(ExifInterface.TAG_ISO_SPEED));
            exifNew.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, exif.getAttribute(userComment)); //ExifInterface.TAG_IMAGE_DESCRIPTION));
            exifNew.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            exifNew.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
            exifNew.setAttribute(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT, exif.getAttribute(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT));
            exifNew.setAttribute(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, exif.getAttribute(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH));
            exifNew.setAttribute(ExifInterface.TAG_LENS_MAKE, exif.getAttribute(ExifInterface.TAG_LENS_MAKE));
            exifNew.setAttribute(ExifInterface.TAG_LENS_SPECIFICATION, exif.getAttribute(ExifInterface.TAG_LENS_SPECIFICATION));
            exifNew.setAttribute(ExifInterface.TAG_LENS_MODEL, exif.getAttribute(ExifInterface.TAG_LENS_MODEL));
            exifNew.setAttribute(ExifInterface.TAG_LIGHT_SOURCE, exif.getAttribute(ExifInterface.TAG_LIGHT_SOURCE));
            exifNew.setAttribute(ExifInterface.TAG_MAKE, exif.getAttribute(ExifInterface.TAG_MAKE));
            exifNew.setAttribute(ExifInterface.TAG_MODEL, exif.getAttribute(ExifInterface.TAG_MODEL));
            exifNew.setAttribute(ExifInterface.TAG_MAKER_NOTE, exif.getAttribute(ExifInterface.TAG_MAKER_NOTE));
            exifNew.setAttribute(ExifInterface.TAG_MAX_APERTURE_VALUE, exif.getAttribute(ExifInterface.TAG_MAX_APERTURE_VALUE));
            exifNew.setAttribute(ExifInterface.TAG_NEW_SUBFILE_TYPE, exif.getAttribute(ExifInterface.TAG_NEW_SUBFILE_TYPE));
            exifNew.setAttribute(ExifInterface.TAG_OECF, exif.getAttribute(ExifInterface.TAG_OECF));
            exifNew.setAttribute((ExifInterface.TAG_ORIENTATION), exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            exifNew.setAttribute(ExifInterface.TAG_OFFSET_TIME, exif.getAttribute(ExifInterface.TAG_OFFSET_TIME));
            exifNew.setAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED, exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED));
            exifNew.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL));
            exifNew.setAttribute(ExifInterface.TAG_ORF_ASPECT_FRAME, exif.getAttribute(ExifInterface.TAG_ORF_ASPECT_FRAME));
            exifNew.setAttribute(ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH, exif.getAttribute(ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH));
            exifNew.setAttribute(ExifInterface.TAG_ORF_PREVIEW_IMAGE_START, exif.getAttribute(ExifInterface.TAG_ORF_PREVIEW_IMAGE_START));
            exifNew.setAttribute(ExifInterface.TAG_ORF_THUMBNAIL_IMAGE, exif.getAttribute(ExifInterface.TAG_ORF_THUMBNAIL_IMAGE));
            exifNew.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY));
            exifNew.setAttribute(ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION, exif.getAttribute(ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION));
            exifNew.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, exif.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION));
            exifNew.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, exif.getAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION));
            exifNew.setAttribute(ExifInterface.TAG_REFERENCE_BLACK_WHITE, exif.getAttribute(ExifInterface.TAG_REFERENCE_BLACK_WHITE));
            exifNew.setAttribute(ExifInterface.TAG_RELATED_SOUND_FILE, exif.getAttribute(ExifInterface.TAG_RELATED_SOUND_FILE));
            exifNew.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, exif.getAttribute(ExifInterface.TAG_RESOLUTION_UNIT));
            exifNew.setAttribute(ExifInterface.TAG_ROWS_PER_STRIP, exif.getAttribute(ExifInterface.TAG_ROWS_PER_STRIP));
            exifNew.setAttribute(ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER, exif.getAttribute(ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER));
            exifNew.setAttribute(ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER, exif.getAttribute(ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER));
            exifNew.setAttribute(ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER, exif.getAttribute(ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER));
            exifNew.setAttribute(ExifInterface.TAG_RW2_SENSOR_TOP_BORDER, exif.getAttribute(ExifInterface.TAG_RW2_SENSOR_TOP_BORDER));


            exifNew.setAttribute(ExifInterface.TAG_RW2_JPG_FROM_RAW, exif.getAttribute(ExifInterface.TAG_RW2_JPG_FROM_RAW));
            exifNew.setAttribute(ExifInterface.TAG_RW2_ISO, exif.getAttribute(ExifInterface.TAG_RW2_ISO));


            exifNew.setAttribute(ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH, exif.getAttribute(ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH));
            exifNew.setAttribute(ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH, exif.getAttribute(ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH));
            //exifNew.setAttribute(ExifInterface.TAG_THUMBNAIL_ORIENTATION, exif.getAttribute(ExifInterface.TAG_THUMBNAIL_ORIENTATION));
            exifNew.setAttribute(ExifInterface.TAG_TRANSFER_FUNCTION, exif.getAttribute(ExifInterface.TAG_TRANSFER_FUNCTION));
            //exifNew.setAttribute(ExifInterface.TAG_USER_COMMENT, exif.getAttribute(ExifInterface.TAG_USER_COMMENT));
            exifNew.setAttribute(ExifInterface.TAG_WHITE_POINT, exif.getAttribute(ExifInterface.TAG_WHITE_POINT));
            exifNew.setAttribute(ExifInterface.TAG_WHITE_BALANCE, exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE));
            exifNew.setAttribute(ExifInterface.TAG_X_RESOLUTION, exif.getAttribute(ExifInterface.TAG_X_RESOLUTION));
            exifNew.setAttribute(ExifInterface.TAG_XMP, exif.getAttribute(ExifInterface.TAG_XMP));
            exifNew.setAttribute(ExifInterface.TAG_Y_RESOLUTION, exif.getAttribute(ExifInterface.TAG_Y_RESOLUTION));
            exifNew.setAttribute(ExifInterface.TAG_Y_CB_CR_COEFFICIENTS, exif.getAttribute(ExifInterface.TAG_Y_CB_CR_COEFFICIENTS));
            exifNew.setAttribute(ExifInterface.TAG_Y_CB_CR_POSITIONING, exif.getAttribute(ExifInterface.TAG_Y_CB_CR_POSITIONING));
            exifNew.setAttribute(ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING, exif.getAttribute(ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING));
            exifNew.setAttribute(ExifInterface.TAG_ISO_SPEED_LATITUDE_ZZZ, exif.getAttribute(ExifInterface.TAG_ISO_SPEED_LATITUDE_ZZZ));
            exifNew.setAttribute(ExifInterface.TAG_ISO_SPEED_LATITUDE_YYY, exif.getAttribute(ExifInterface.TAG_ISO_SPEED_LATITUDE_YYY));
            exifNew.setAttribute(ExifInterface.TAG_USER_COMMENT, userComment);

            exifNew.saveAttributes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writePublicLog(String str) {
        Uri contentUri = MediaStore.Files.getContentUri("external");
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
        String[] selectionArgs = new String[]{Environment.DIRECTORY_DOCUMENTS + "/CapturingThePast/"};
        Cursor cursor = getContentResolver().query(contentUri, null, selection, selectionArgs, null);
        Uri uri = null;
        if (cursor.getCount() == 0) {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "CapturingThePastLog");       //file name
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");        //file extension, will automatically add to file
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/CapturingThePast/");     //end "/" is not mandatory
                Uri uri2 = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);      //important!
                OutputStream outputStream = getContentResolver().openOutputStream(uri2);
                str = "\"Date\", \"Cat Ref\", \"Filename\", \"Note\"\n" + str;  // Add a header row to the string input as this is the opening line
                outputStream.write(str.getBytes());
                outputStream.close();
            } catch (IOException e) {
                Toast.makeText(this, "Fail to create log", LENGTH_SHORT).show();
            }
        } else {
            while (cursor.moveToNext()) {
                int disName = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                int disID = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
                //if(disName>=0 && disID>=0) {
                //if(disName>=0){

                    String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                    if (fileName.equals(strLogFilename)) {
                        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));

                        uri = ContentUris.withAppendedId(contentUri, id);
                        break;
                    }
               // }
            }
            if (uri == null) {
                Toast.makeText(this, "\"" + strLogFilename + "\" not found", LENGTH_SHORT).show();
            } else {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    int size = inputStream.available();
                    byte[] bytes = new byte[size];
                    inputStream.read(bytes);
                    inputStream.close();
                    String strFileTxt = new String(bytes, StandardCharsets.UTF_8);
                    strFileTxt += "\n";
                    strFileTxt += str;
                    OutputStream outputStream = getContentResolver().openOutputStream(uri, "rwt");      //overwrite mode, see below
                    outputStream.write(strFileTxt.getBytes());
                    outputStream.close();
                } catch (IOException e) {
                    Toast.makeText(this, "Fail to read file", LENGTH_SHORT).show();
                }
            }
        }
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    private void writeArchons() {
       // Log.i("Content ", "Write Archons");
        String jsonString = "{\"strPrefix\":\"cpast\",\"bTimestamp\":\"TRUE\",\"nCaptureCount\":\"0\",\"recentFiles\":[],\"data\":[{\"Repository\":\"Repository - GB0000\",\"Archon\":\"GB0000\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Archives and Cornish Studies Service\",\"Archon\":\"GB0021\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Bedfordshire Archives & Record Service\",\"Archon\":\"GB0004\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Berkshire Record Office\",\"Archon\":\"GB0005\",\"Enabled\":\"TRUE\"},{\"Repository\":\"British Library Manuscript Collections\",\"Archon\":\"GB0058\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Brotherton Library - Leeds University\",\"Archon\":\"GB1471\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Buckinghamshire Archives\",\"Archon\":\"GB0008\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cambridge University Library: Department of Manuscripts and University Archives\",\"Archon\":\"GB0012\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cambridgeshire Archives\",\"Archon\":\"GB0010\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Ceredigion Archives\",\"Archon\":\"GB0212\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Chester Archives and Local Studies\",\"Archon\":\"GB0017\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Coventry Archives and Local Record Office\",\"Archon\":\"GB0144\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Barrow)\",\"Archon\":\"GB0025\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Carlisle)\",\"Archon\":\"GB0023\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Kendal)\",\"Archon\":\"GB0024\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Whitehaven)\",\"Archon\":\"GB1831\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derby Local Studies and Family History Library\",\"Archon\":\"GB1160\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derbyshire Local Studies Library\",\"Archon\":\"GB1944\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derbyshire Record Office\",\"Archon\":\"GB0026\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Devon Archives and Local Studies Service (South WestHeritage Trust)\",\"Archon\":\"GB0027\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dorset History Centre\",\"Archon\":\"GB0031\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dr Williams’s Library\",\"Archon\":\"GB0123\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dudley Archives and Local History Centre\",\"Archon\":\"GB0145\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Durham County Record Office\",\"Archon\":\"GB0032\",\"Enabled\":\"TRUE\"},{\"Repository\":\"East Sussex and Brighton and Hove Record Office\",\"Archon\":\"GB0179\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Essex Record Office\",\"Archon\":\"GB0037\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Gloucestershire Archives\",\"Archon\":\"GB0040\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Hampshire Archives and Local Studies\",\"Archon\":\"GB0041\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Herefordshire Archives and Records Centre\",\"Archon\":\"GB0044\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Hertfordshire Archives and Local Studies\",\"Archon\":\"GB0046\",\"Enabled\":\"TRUE\"},{\"Repository\":\"John Rylands Library\",\"Archon\":\"GB3191\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Kent History and Library Centre\",\"Archon\":\"GB0051\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Lambeth Palace Library\",\"Archon\":\"GB0109\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Lincolnshire Archives\",\"Archon\":\"GB0057\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Liverpool Record Office\",\"Archon\":\"GB1623\",\"Enabled\":\"TRUE\"},{\"Repository\":\"London Metropolitan Archives\",\"Archon\":\"GB0074\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Manchester City Archives\",\"Archon\":\"GB0127\",\"Enabled\":\"TRUE\"},{\"Repository\":\"National Library of Scotland\",\"Archon\":\"GB0233\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Norfolk Record Office\",\"Archon\":\"GB0153\",\"Enabled\":\"TRUE\"},{\"Repository\":\"North Yorkshire County Record Office\",\"Archon\":\"GB0191\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northamptonshire Archives\",\"Archon\":\"GB0154\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northumberland Archives\",\"Archon\":\"GB0155\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northumberland Record Office - Morpeth\",\"Archon\":\"GB1834\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Nottinghamshire Archives\",\"Archon\":\"GB0157\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Oxford University: Bodleian Library - Special Collections\",\"Archon\":\"GB0161\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Oxfordshire History Centre\",\"Archon\":\"GB0160\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Religious Society of Friends Library\",\"Archon\":\"GB0111\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Sheffield City Archives\",\"Archon\":\"GB1163\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Sheffield Local Studies Library\",\"Archon\":\"GB1783\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Shropshire Archives\",\"Archon\":\"GB0166\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Somerset Heritage Centre\",\"Archon\":\"GB0168\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Suffolk Record Office - Bury St Edmunds Branch\",\"Archon\":\"GB0174\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Suffolk Record Office - Ipswich Branch\",\"Archon\":\"GB0173\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Surrey History Centre\",\"Archon\":\"GB0176\",\"Enabled\":\"TRUE\"},{\"Repository\":\"The National Archives - Kew\",\"Archon\":\"GB0066\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Tyne & Wear Archives\",\"Archon\":\"GB0183\",\"Enabled\":\"TRUE\"},{\"Repository\":\"University of Birmingham: Cadbury Research Library\",\"Archon\":\"GB0150\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Ushaw College Library (Durham University Special Collections)\",\"Archon\":\"GB0033\",\"Enabled\":\"TRUE\"},{\"Repository\":\"WellcomeCollection\",\"Archon\":\"GB0120\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Sussex Record Office\",\"Archon\":\"GB0182\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Bradford)\",\"Archon\":\"GB0202\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Calderdale)\",\"Archon\":\"GB0203\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Kirklees)\",\"Archon\":\"GB0204\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Leeds)\",\"Archon\":\"GB0205\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Wiltshire and Swindon History Centre\",\"Archon\":\"GB0190\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Wolverhampton City Archives\",\"Archon\":\"GB0149\",\"Enabled\":\"TRUE\"},{\"Repository\":\"York City Archives\",\"Archon\":\"GBYORK\",\"Enabled\":\"TRUE\"}]}";
        //String jsonString = "{\"strPrefix\":\"cpast\",\"bTimestamp\":\"TRUE\",\"nCaptureCount\":\"0\",\"data\":[{\"Repository\":\"Repository - GB0000\",\"Archon\":\"GB0000\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Archives and Cornish Studies Service\",\"Archon\":\"GB0021\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Bedfordshire Archives & Record Service\",\"Archon\":\"GB0004\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Berkshire Record Office\",\"Archon\":\"GB0005\",\"Enabled\":\"TRUE\"},{\"Repository\":\"British Library Manuscript Collections\",\"Archon\":\"GB0058\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Brotherton Library - Leeds University\",\"Archon\":\"GB1471\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Buckinghamshire Archives\",\"Archon\":\"GB0008\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cambridge University Library: Department of Manuscripts and University Archives\",\"Archon\":\"GB0012\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cambridgeshire Archives\",\"Archon\":\"GB0010\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Ceredigion Archives\",\"Archon\":\"GB0212\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Chester Archives and Local Studies\",\"Archon\":\"GB0017\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Coventry Archives and Local Record Office\",\"Archon\":\"GB0144\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Barrow)\",\"Archon\":\"GB0025\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Carlisle)\",\"Archon\":\"GB0023\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Kendal)\",\"Archon\":\"GB0024\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Whitehaven)\",\"Archon\":\"GB1831\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derby Local Studies and Family History Library\",\"Archon\":\"GB1160\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derbyshire Local Studies Library\",\"Archon\":\"GB1944\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derbyshire Record Office\",\"Archon\":\"GB0026\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Devon Archives and Local Studies Service (South WestHeritage Trust)\",\"Archon\":\"GB0027\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dorset History Centre\",\"Archon\":\"GB0031\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dr Williams’s Library\",\"Archon\":\"GB0123\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dudley Archives and Local History Centre\",\"Archon\":\"GB0145\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Durham County Record Office\",\"Archon\":\"GB0032\",\"Enabled\":\"TRUE\"},{\"Repository\":\"East Sussex and Brighton and Hove Record Office\",\"Archon\":\"GB0179\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Essex Record Office\",\"Archon\":\"GB0037\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Gloucestershire Archives\",\"Archon\":\"GB0040\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Hampshire Archives and Local Studies\",\"Archon\":\"GB0041\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Herefordshire Archives and Records Centre\",\"Archon\":\"GB0044\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Hertfordshire Archives and Local Studies\",\"Archon\":\"GB0046\",\"Enabled\":\"TRUE\"},{\"Repository\":\"John Rylands Library\",\"Archon\":\"GB3191\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Kent History and Library Centre\",\"Archon\":\"GB0051\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Lambeth Palace Library\",\"Archon\":\"GB0109\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Lincolnshire Archives\",\"Archon\":\"GB0057\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Liverpool Record Office\",\"Archon\":\"GB1623\",\"Enabled\":\"TRUE\"},{\"Repository\":\"London Metropolitan Archives\",\"Archon\":\"GB0074\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Manchester City Archives\",\"Archon\":\"GB0127\",\"Enabled\":\"TRUE\"},{\"Repository\":\"National Library of Scotland\",\"Archon\":\"GB0233\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Norfolk Record Office\",\"Archon\":\"GB0153\",\"Enabled\":\"TRUE\"},{\"Repository\":\"North Yorkshire County Record Office\",\"Archon\":\"GB0191\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northamptonshire Archives\",\"Archon\":\"GB0154\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northumberland Archives\",\"Archon\":\"GB0155\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northumberland Record Office - Morpeth\",\"Archon\":\"GB1834\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Nottinghamshire Archives\",\"Archon\":\"GB0157\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Oxford University: Bodleian Library - Special Collections\",\"Archon\":\"GB0161\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Oxfordshire History Centre\",\"Archon\":\"GB0160\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Religious Society of Friends Library\",\"Archon\":\"GB0111\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Sheffield City Archives\",\"Archon\":\"GB1163\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Sheffield Local Studies Library\",\"Archon\":\"GB1783\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Shropshire Archives\",\"Archon\":\"GB0166\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Somerset Heritage Centre\",\"Archon\":\"GB0168\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Suffolk Record Office - Bury St Edmunds Branch\",\"Archon\":\"GB0174\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Suffolk Record Office - Ipswich Branch\",\"Archon\":\"GB0173\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Surrey History Centre\",\"Archon\":\"GB0176\",\"Enabled\":\"TRUE\"},{\"Repository\":\"The National Archives - Kew\",\"Archon\":\"GB0066\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Tyne & Wear Archives\",\"Archon\":\"GB0183\",\"Enabled\":\"TRUE\"},{\"Repository\":\"University of Birmingham: Cadbury Research Library\",\"Archon\":\"GB0150\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Ushaw College Library (Durham University Special Collections)\",\"Archon\":\"GB0033\",\"Enabled\":\"TRUE\"},{\"Repository\":\"WellcomeCollection\",\"Archon\":\"GB0120\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Sussex Record Office\",\"Archon\":\"GB0182\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Bradford)\",\"Archon\":\"GB0202\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Calderdale)\",\"Archon\":\"GB0203\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Kirklees)\",\"Archon\":\"GB0204\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Leeds)\",\"Archon\":\"GB0205\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Wiltshire and Swindon History Centre\",\"Archon\":\"GB0190\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Wolverhampton City Archives\",\"Archon\":\"GB0149\",\"Enabled\":\"TRUE\"},{\"Repository\":\"York City Archives\",\"Archon\":\"GBYORK\",\"Enabled\":\"TRUE\"}]}";
        createAndSaveFile(params, jsonString);
    }

    private void resetArchons(String str) {
        String jsonString = "{\"strPrefix\":\"cpast\",\"bTimestamp\":\"TRUE\",\"nCaptureCount\":\"0\",\"recentFiles\":[],\"data\":[{\"Repository\":\"Repository - GB0000\",\"Archon\":\"GB0000\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Archives and Cornish Studies Service\",\"Archon\":\"GB0021\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Bedfordshire Archives & Record Service\",\"Archon\":\"GB0004\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Berkshire Record Office\",\"Archon\":\"GB0005\",\"Enabled\":\"TRUE\"},{\"Repository\":\"British Library Manuscript Collections\",\"Archon\":\"GB0058\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Brotherton Library - Leeds University\",\"Archon\":\"GB1471\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Buckinghamshire Archives\",\"Archon\":\"GB0008\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cambridge University Library: Department of Manuscripts and University Archives\",\"Archon\":\"GB0012\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cambridgeshire Archives\",\"Archon\":\"GB0010\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Ceredigion Archives\",\"Archon\":\"GB0212\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Chester Archives and Local Studies\",\"Archon\":\"GB0017\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Coventry Archives and Local Record Office\",\"Archon\":\"GB0144\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Barrow)\",\"Archon\":\"GB0025\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Carlisle)\",\"Archon\":\"GB0023\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Kendal)\",\"Archon\":\"GB0024\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria Archive Service (Whitehaven)\",\"Archon\":\"GB1831\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derby Local Studies and Family History Library\",\"Archon\":\"GB1160\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derbyshire Local Studies Library\",\"Archon\":\"GB1944\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Derbyshire Record Office\",\"Archon\":\"GB0026\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Devon Archives and Local Studies Service (South WestHeritage Trust)\",\"Archon\":\"GB0027\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dorset History Centre\",\"Archon\":\"GB0031\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dr Williams’s Library\",\"Archon\":\"GB0123\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Dudley Archives and Local History Centre\",\"Archon\":\"GB0145\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Durham County Record Office\",\"Archon\":\"GB0032\",\"Enabled\":\"TRUE\"},{\"Repository\":\"East Sussex and Brighton and Hove Record Office\",\"Archon\":\"GB0179\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Essex Record Office\",\"Archon\":\"GB0037\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Gloucestershire Archives\",\"Archon\":\"GB0040\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Hampshire Archives and Local Studies\",\"Archon\":\"GB0041\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Herefordshire Archives and Records Centre\",\"Archon\":\"GB0044\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Hertfordshire Archives and Local Studies\",\"Archon\":\"GB0046\",\"Enabled\":\"TRUE\"},{\"Repository\":\"John Rylands Library\",\"Archon\":\"GB3191\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Kent History and Library Centre\",\"Archon\":\"GB0051\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Lambeth Palace Library\",\"Archon\":\"GB0109\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Lincolnshire Archives\",\"Archon\":\"GB0057\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Liverpool Record Office\",\"Archon\":\"GB1623\",\"Enabled\":\"TRUE\"},{\"Repository\":\"London Metropolitan Archives\",\"Archon\":\"GB0074\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Manchester City Archives\",\"Archon\":\"GB0127\",\"Enabled\":\"TRUE\"},{\"Repository\":\"National Library of Scotland\",\"Archon\":\"GB0233\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Norfolk Record Office\",\"Archon\":\"GB0153\",\"Enabled\":\"TRUE\"},{\"Repository\":\"North Yorkshire County Record Office\",\"Archon\":\"GB0191\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northamptonshire Archives\",\"Archon\":\"GB0154\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northumberland Archives\",\"Archon\":\"GB0155\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Northumberland Record Office - Morpeth\",\"Archon\":\"GB1834\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Nottinghamshire Archives\",\"Archon\":\"GB0157\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Oxford University: Bodleian Library - Special Collections\",\"Archon\":\"GB0161\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Oxfordshire History Centre\",\"Archon\":\"GB0160\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Religious Society of Friends Library\",\"Archon\":\"GB0111\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Sheffield City Archives\",\"Archon\":\"GB1163\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Sheffield Local Studies Library\",\"Archon\":\"GB1783\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Shropshire Archives\",\"Archon\":\"GB0166\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Somerset Heritage Centre\",\"Archon\":\"GB0168\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Suffolk Record Office - Bury St Edmunds Branch\",\"Archon\":\"GB0174\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Suffolk Record Office - Ipswich Branch\",\"Archon\":\"GB0173\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Surrey History Centre\",\"Archon\":\"GB0176\",\"Enabled\":\"TRUE\"},{\"Repository\":\"The National Archives - Kew\",\"Archon\":\"GB0066\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Tyne & Wear Archives\",\"Archon\":\"GB0183\",\"Enabled\":\"TRUE\"},{\"Repository\":\"University of Birmingham: Cadbury Research Library\",\"Archon\":\"GB0150\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Ushaw College Library (Durham University Special Collections)\",\"Archon\":\"GB0033\",\"Enabled\":\"TRUE\"},{\"Repository\":\"WellcomeCollection\",\"Archon\":\"GB0120\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Sussex Record Office\",\"Archon\":\"GB0182\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Bradford)\",\"Archon\":\"GB0202\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Calderdale)\",\"Archon\":\"GB0203\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Kirklees)\",\"Archon\":\"GB0204\",\"Enabled\":\"TRUE\"},{\"Repository\":\"West Yorkshire Archive Service (Leeds)\",\"Archon\":\"GB0205\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Wiltshire and Swindon History Centre\",\"Archon\":\"GB0190\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Wolverhampton City Archives\",\"Archon\":\"GB0149\",\"Enabled\":\"TRUE\"},{\"Repository\":\"York City Archives\",\"Archon\":\"GBYORK\",\"Enabled\":\"TRUE\"}]}";
        if (str.equals("short")) {
            //jsonString = "{\"strPrefix\":\"cpast\",\"bTimestamp\":\"TRUE\",\"nCaptureCount\":\"0\",\"recentFiles\":[],\"data\":[{\"Repository\":\"Repository - GB0000\",\"Archon\":\"GB0000\",\"Enabled\":\"TRUE\"},{\"Repository\":\"The National Archives - Kew\",\"Archon\":\"GB0066\",\"Enabled\":\"TRUE\"}]}";
            //jsonString = "{\"strPrefix\":\"cpast\",\"bTimestamp\":\"TRUE\",\"nCaptureCount\":\"0\",\"recentFiles\":[],\"data\":[{\"Repository\":\"Default Repository - GB0000\",\"Archon\":\"GB0000\",\"Enabled\":\"TRUE\"},{\"Repository\":\"TNA\",\"Archon\":\"GB0066\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria\",\"Archon\":\"GB0023\",\"Enabled\":\"TRUE\"},{\"Repository\":\"East Sussex\",\"Archon\":\"GB0179\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Staffordshire\",\"Archon\":\"GB0169\",\"Enabled\":\"TRUE\"}]}";
            jsonString = "{\"strPrefix\":\"cpast\",\"bTimestamp\":\"TRUE\",\"nCaptureCount\":\"0\",\"recentFiles\":[],\"data\":[{\"Repository\":\"Default Repository - GB0000\",\"Archon\":\"GB0000\",\"Enabled\":\"TRUE\"},{\"Repository\":\"TNA\",\"Archon\":\"GB0066\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Cumbria\",\"Archon\":\"GB0023\",\"Enabled\":\"TRUE\"},{\"Repository\":\"East Sussex\",\"Archon\":\"GB0179\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Staffordshire\",\"Archon\":\"GB0169\",\"Enabled\":\"TRUE\"},{\"Repository\":\"St Bartholomew's Hospital Archives\",\"Archon\":\"GB0405\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Royal London Hospital Archives\",\"Archon\":\"GB0387\",\"Enabled\":\"TRUE\"}]}";
        }
        else if (str.equals("alternative")) {
            jsonString = "{\"strPrefix\":\"cpast\",\"bTimestamp\":\"TRUE\",\"nCaptureCount\":\"0\",\"recentFiles\":[],\"data\":[{\"Repository\":\"Repository - Example\",\"Archon\":\"EXAMPLE\",\"Enabled\":\"TRUE\"},{\"Repository\":\"Mum's papers\",\"Archon\":\"GBMUM\",\"Enabled\":\"TRUE\"}]}";
        }
        createAndSaveFile(params, jsonString);
    }

    public void createAndSaveFile(String params, String jsonString) {
        try {
            FileWriter file = new FileWriter("/data/data/" + getApplicationContext().getPackageName() + "/" + params);
            file.write(jsonString);
            file.flush();
            file.close();
            readJsonData(params);
        } catch (IOException e) {
            //Log.i("Content ", " JSON Writing Exception *******************************************************************************  JSON Writing Exception ");
            e.printStackTrace();
        }
    }

    public void readJsonData(String params) {
        try {
            File f = new File("/data/data/" + getPackageName() + "/" + params);
            FileInputStream is = new FileInputStream(f);
            int size = is.available();
            if (size > 0) {
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String strResponse = new String(buffer);
                applyJsonSettings(strResponse);
            } else {
                writeArchons();
            }
        } catch (IOException e) {
            writeArchons();
            e.printStackTrace();
        }
    }

    private void applyJsonSettings(String jsonString) {
        String output = jsonString;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonString);
            strPrefix = jsonObject.getString("strPrefix");
            bTimestamped = jsonObject.getBoolean("bTimestamp");
            nCaptureCounter = jsonObject.getInt("nCaptureCount");

            repositories = jsonObject.getJSONArray("data");
            repos = new String[repositories.length()];
            int length = repositories.length();
            int nEnabled = 0;
            for (int i = 0; i < length; i++) {
                JSONObject jo = repositories.getJSONObject(i);
                repos[i] = jo.getString("Repository");
                String st = jo.getString("Enabled");
                boolean b = Boolean.parseBoolean(st);
                if (b) {
                    nEnabled++;
                }
            }
            //Log.i("Content ", "AJS2 Apply bTimestamped=" + bTimestamped + "..strPrefix=" + strPrefix + "..-.." + length + " repos.." + nEnabled + " enabled - nCurrentRepo "+nCurrentRepo);
            Spinner dropdownd = findViewById(R.id.spinnerRepo);
            ArrayAdapter dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, repos);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dropdownd.setAdapter(dataAdapter);
        } catch (JSONException e) {
            writeArchons();
           // Log.i("Content ", "AJS Exception xxxxx");
            e.printStackTrace();
        }
        try {
            jsonObject = new JSONObject(jsonString);
            recentFileStore = jsonObject.getJSONArray("recentFiles");
            recentFiles = new ArrayList<String>();//(recentFileStore);
            for (int j = 0; j < recentFileStore.length(); j++) {
                recentFiles.add((String) recentFileStore.get(j));
                //Log.i("Content ", "string " + recentFiles.get(j));
            }
        } catch (JSONException e) {
            //Log.i("Content ", "AJS Secondary Exception xxxxx");
            e.printStackTrace();
        }
    }

    private void writePreferences() {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("strPrefix", strPrefix);
            String strTimeStamped = "FALSE";
            if (bTimestamped) {
                strTimeStamped = "TRUE";
            }
            jsonObj.put("bTimestamp", strTimeStamped);
            jsonObj.put("nCaptureCount", nCaptureCounter);
            jsonObj.put("recentFiles", recentFileStore);
            jsonObj.put("data", repositories);
            String output = jsonObj.toString();
            createAndSaveFile(params, output);
        } catch (JSONException e) {
            e.printStackTrace();
            //Log.i("Content ", "preference writing exception");
        }
    }

    private void addRepo() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        TextView tvTitle = new TextView(this);
        String tittleText = "";
        tittleText += getString(R.string.add_repo_heading);
        tvTitle.setMovementMethod(LinkMovementMethod.getInstance());
        tvTitle.setText(Html.fromHtml(tittleText, Html.FROM_HTML_MODE_LEGACY));
        TextView tvTip = new TextView(this);
        //String sLink = " <a href=https://discovery.nationalarchives.gov.uk/browse/a/A >TNA's</a>";//String privacyLink = " and our <a href=file://"+currentFolderPath+"/>Privacy Policy</a>";
        String tipText = getString(R.string.tna_tip);
        tipText += getString(R.string.repo_tip);
        tvTip.setMovementMethod(LinkMovementMethod.getInstance());
        tvTip.setText(Html.fromHtml(tipText, Html.FROM_HTML_MODE_LEGACY));
        TextView textView = new TextView(this);
        textView.setText(R.string.repo_name_hint);
        textView.setTextSize(18f);
        tvTip.setTextSize(18f);
        final EditText inputRepo = new EditText(MainActivity.this);
        inputRepo.setHint(R.string.repo_hint);
        final EditText inputArchon = new EditText(MainActivity.this);
        inputArchon.setHint(R.string.archon_hint);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(tvTitle);
        linearLayout.addView(inputRepo);
        linearLayout.addView(textView);
        linearLayout.addView(inputArchon);
        linearLayout.addView(tvTip);
        linearLayout.setPadding(50, 80, 50, 10);
        alertDialog.setView(linearLayout);
        alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String strRepo = inputRepo.getText().toString();
                String strArchon = inputArchon.getText().toString();
                strArchon = strArchon.replaceAll("\\s+", "").toUpperCase();
                strArchon = strArchon.replaceAll("/", "_");
                JSONObject jsonObj = new JSONObject();
                try {
                    jsonObj.put("Repository", strRepo);
                    jsonObj.put("Archon", strArchon);
                    jsonObj.put("Enabled", "TRUE");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JSONArray list = new JSONArray();
                list.put(jsonObj);
                int len = repositories.length();
                if (repositories != null) {
                    try {
                        for (int i = 0; i < len; i++) {
                            list.put(repositories.get(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                repositories = list;
                writePreferences();
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    private void deleteRepository(int n) {
        try {
            JSONArray list = new JSONArray();
            int len = repositories.length();
            if (repositories != null) {
                for (int i = 0; i < len; i++) {
                    //Excluding the item at position
                    if (i != n) {
                        list.put(repositories.get(i));
                    }
                }
            }
            repositories = list;
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("strPrefix", strPrefix);
            String strTimeStamped = "FALSE";
            if (bTimestamped) {
                strTimeStamped = "TRUE";
            }
            jsonObj.put("bTimestamp", strTimeStamped);
            jsonObj.put("data", repositories);
            jsonObj.put("nCaptureCount", nCaptureCounter);
            jsonObj.put("recentFiles", recentFileStore);
            String output = jsonObj.toString();
      //      Log.i("Content ", "Delete Repo " + n);
            createAndSaveFile(params, output);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void deleteRepoGui() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        TextView tvTip = new TextView(this);
        tvTip.setText(R.string.set_prefix);
        TextView tvTitle = new TextView(this);
        String tittleText = getString(R.string.heading_select_repo);
        tvTitle.setMovementMethod(LinkMovementMethod.getInstance());
        tvTitle.setText(Html.fromHtml(tittleText, Html.FROM_HTML_MODE_LEGACY));

        TextView labelSelect = new TextView(this);
        labelSelect.setText(R.string.select_repo);
        labelSelect.setPadding(20, 20, 0, 20);
        labelSelect.setTextSize(20f);
        TextView tvPresetsLabel = new TextView(this);
        tvPresetsLabel.setText(R.string.repo_presets);
        final EditText inputPrefix = new EditText(MainActivity.this);
        inputPrefix.setText(strPrefix);
        inputPrefix.setHint(R.string.prefix);
        final Switch switchTimestamp = new Switch(this);
        switchTimestamp.setChecked(bTimestamped);
        switchTimestamp.setText(R.string.include_timestamp);
        final Button loadReposDefault = new Button(this);
        loadReposDefault.setText(R.string.default_repo_list);
        loadReposDefault.setAllCaps(false);
        loadReposDefault.setBackgroundColor(Color.DKGRAY);
        loadReposDefault.setTextColor(Color.WHITE);
        final Button loadReposShort = new Button(this);
        loadReposShort.setText(R.string.short_repo_list);
        loadReposShort.setAllCaps(false);
        loadReposShort.setBackgroundColor(Color.DKGRAY);
        loadReposShort.setTextColor(Color.WHITE);
        final Button loadReposAlt = new Button(this);
        loadReposAlt.setText(R.string.alternative);
        loadReposAlt.setTextColor(Color.WHITE);
        loadReposAlt.setBackgroundColor(Color.DKGRAY);
        loadReposAlt.setAllCaps(false);
        final Button deleteRepo = new Button(this);
        deleteRepo.setText(R.string.delete_selected_repository);
        deleteRepo.setAllCaps(false);
        deleteRepo.setBackgroundColor(Color.DKGRAY);
        deleteRepo.setTextColor(Color.WHITE);

        repos = new String[repositories.length()];
        final int[] length = {repositories.length()};

        for (int i = 0; i < length[0]; i++) {
            JSONObject jo;
            try {
                jo = repositories.getJSONObject(i);
                repos[i] = jo.getString("Repository");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
     //   Log.i("Content ", "SDE bTimestamped=" + bTimestamped
//                + "..strPrefix=" + strPrefix + "..-.." + length[0] + " repos..");
        Spinner spinnerRepoSelect = new Spinner(this);
        ArrayAdapter dataAdapterR = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, repos);
        dataAdapterR.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepoSelect.setAdapter(dataAdapterR);
        spinnerRepoSelect.setPadding(0, 8, 8, 24);

        final int[] selectedRepo = {-1};
        final boolean[] bReset = {false};
        LinearLayout lpset = new LinearLayout(this);
        lpset.setOrientation(LinearLayout.VERTICAL);
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        //tvPresetsLabel.setGravity(1);
        tvPresetsLabel.setTextSize(18f);
        //btnRow.setGravity(1);
        lpset.addView(tvTitle);
        lpset.addView(spinnerRepoSelect);
        lpset.addView(deleteRepo);

        String repoPresetNote = getString(R.string.repository_presets_note); // "<p><br /><hr />Note: Presets overwrite repository list customisations.</p>";
        String infoText = "" + repoPresetNote;
        TextView tvPresetTip = new TextView(this);
        tvPresetTip.setMovementMethod(LinkMovementMethod.getInstance());
        tvPresetTip.setText(Html.fromHtml(infoText, Html.FROM_HTML_MODE_LEGACY));
        tvPresetTip.setTextSize(16f);
        lpset.addView(tvPresetTip);

        lpset.addView(tvPresetsLabel);
        btnRow.addView(loadReposDefault);
        btnRow.addView(loadReposShort);
        btnRow.addView(loadReposAlt);
        lpset.addView(btnRow);

        lpset.setPadding(50, 80, 50, 10);
        alertDialog.setView(lpset);
        spinnerRepoSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRepo[0] = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        deleteRepo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.i("Content ", "Repo Reset");
                String strToast = "";
                JSONObject jsonObj = null;
                try {
                    jsonObj = repositories.getJSONObject(selectedRepo[0]);
                    strToast = jsonObj.getString("Repository");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                deleteRepository(selectedRepo[0]);
                writePreferences();
                spinnerRepoSelect.setAdapter(makeNewRemovalDropDown());
                CharSequence text = getString(R.string.deleted)+ " - " + strToast;
                int duration = 2000;//Toast.LENGTH_SHORT;
                Snackbar snack = Snackbar.make(lpset, text, duration);
                snack.show();
            }
        });

        loadReposDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Log.i("Content ", "Repo Reset");
                bReset[0] = true;
                resetArchons("default");
                writePreferences();
                spinnerRepoSelect.setAdapter(makeNewRemovalDropDown());
                CharSequence text = getString(R.string.repository_default_presets_message);//"Default repository list loaded";
                int duration = 2000;//Toast.LENGTH_SHORT;
                Snackbar snack = Snackbar.make(lpset, text, duration);
                snack.show();
            }
        });

        loadReposShort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Log.i("Content ", "Repo Reset");
                bReset[0] = true;
                resetArchons("short");
                writePreferences();
                spinnerRepoSelect.setAdapter(makeNewRemovalDropDown());
                CharSequence text = getString(R.string.repository_short_presets_message);//"Short repository list loaded";
                int duration = 2000;//Toast.LENGTH_SHORT;
                Snackbar snack = Snackbar.make(lpset, text, duration);
                snack.show();
            }
        });
        loadReposAlt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Log.i("Content ", "Repo Reset");
                bReset[0] = true;
                resetArchons("alternative");
                writePreferences();
                spinnerRepoSelect.setAdapter(makeNewRemovalDropDown());
                CharSequence text = getString(R.string.repository_alternative_presets_message);//"Alternative repository list loaded";
                int duration = 2000;//Toast.LENGTH_SHORT;
                Snackbar snack = Snackbar.make(lpset, text, duration);
                snack.show();
            }
        });
        alertDialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    private ArrayAdapter makeNewRemovalDropDown() {
        repos = new String[repositories.length()];
        final int[] length = {repositories.length()};

        for (int i = 0; i < length[0]; i++) {
            JSONObject jsonObject;
            try {
                jsonObject = repositories.getJSONObject(i);
                repos[i] = jsonObject.getString("Repository");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ArrayAdapter dataAdapterR = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, repos);
        dataAdapterR.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return dataAdapterR;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void showInfo() {
        String str = "";
        for (int i = recentFiles.size() - 1; i >= 0; i--) {
            str += recentFiles.get(i) + "\n";
        }
        String folderStatus = getString(R.string.latest_captures_message) + str; //"Latest captures (Most recent first):\n" + str;
        String strMessage = "";
        strMessage = "<p>" + nCaptureCounter + "</p> ";
        showFolderStatusMessage(strMessage, folderStatus);
    }

    private void setCaptureCounter(int n, String fName) {
       // Log.i("Content ", " Capture counter^^^^^^ " + n);
        if (!fName.equals("")) {
            recentFiles.add(fName);
            if (recentFiles.size() > 5) {
                recentFiles.remove(0);
            }
            recentFileStore = new JSONArray(recentFiles);
        }
        nCaptureCounter = n;
        writePreferences();
      //  Log.i("Content ", " Capture counter^^^^^^ " + nCaptureCounter);
    }

//    private String getRecentCaptureList() {
//        String str = "";
//        for (int i = recentFiles.size() - 1; i >= 0; i--) {
//            str += recentFiles.get(i) + "\n";
//        }
//        return str;
//    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void showFolderStatusMessage(String strMessage, String strReport) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        TextView tvTip = new TextView(this);
        String sLink = getString(R.string.resources_note);//"<h3>Resources</h3>"; //resources_note
//        sLink += "<p><a href=https://blogs.sussex.ac.uk/capturing-the-past/about-us/ >Website</a></p>";
//        sLink += "<p><a href=https://blogs.sussex.ac.uk/capturing-the-past/2021/11/04/hello-world/ >Instructions</a></p>";
        TextView tvLogInfo = new TextView(this);
        String strLogInfo = getString(R.string.log_information);//"<h3>Capture Log</h3><p>A log (called CapturingThePast) of all captures is saved in your Documents folder. " +
                //"Delete the log to reset it, or rename it to preserve it and start a fresh one. " +
                //"</p>"; //log_information
        tvLogInfo.setMovementMethod(LinkMovementMethod.getInstance());
        tvLogInfo.setText(Html.fromHtml(strLogInfo, Html.FROM_HTML_MODE_LEGACY));

        String infoText = "" + sLink;
        tvTip.setMovementMethod(LinkMovementMethod.getInstance());
        tvTip.setText(Html.fromHtml(infoText, Html.FROM_HTML_MODE_LEGACY));
        TextView tvList = new TextView(this);
        tvList.setText(strReport);
        TextView tvCaptureCount = new TextView(this);
        tvCaptureCount.setMovementMethod(LinkMovementMethod.getInstance());
        tvCaptureCount.setText(Html.fromHtml(strMessage, Html.FROM_HTML_MODE_LEGACY));
        String fLink = getString(R.string.footer_link);//"<p><br/>Capturing the Past is a <a href=https://www.sussex.ac.uk/research/centres/sussex-humanities-lab/ >Sussex Humanities Lab</a> project funded by the <a href=https://ahrc.ukri.org/ >Arts and Humanities Research Council</a>.</p>";
        TextView tvHeader = new TextView(this);
        tvHeader.setMovementMethod(LinkMovementMethod.getInstance());
        tvHeader.setText(Html.fromHtml(fLink, Html.FROM_HTML_MODE_LEGACY));
        String fCap = getString(R.string.counter_label);//"<h4>Capture Counter</h4>";
        TextView tvCap = new TextView(this);
        tvCap.setMovementMethod(LinkMovementMethod.getInstance());
        tvCap.setText(Html.fromHtml(fCap, Html.FROM_HTML_MODE_LEGACY));
        final Button btnResetCount = new Button(this);
        btnResetCount.setText(getString(R.string.reset));
        btnResetCount.setAllCaps(false);
        LinearLayout counterReset = new LinearLayout(this);
        counterReset.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout lpset = new LinearLayout(this);
        lpset.setOrientation(LinearLayout.VERTICAL);
        tvTip.setTextSize(16f);
        tvCap.setTextSize(16f);
        lpset.addView(tvTip);
        lpset.addView(tvLogInfo);

        tvCaptureCount.setWidth(150);
        tvCaptureCount.setGravity(1);
        btnResetCount.setBackgroundColor(0000); //setHeight(50)
        btnResetCount.setTextColor(Color.DKGRAY);
        counterReset.addView(tvCap);
        counterReset.addView(tvCaptureCount);
        counterReset.addView(btnResetCount);
        lpset.addView(counterReset);
        lpset.addView(tvList);
        tvHeader.setTextSize(11.0f);
        lpset.addView(tvHeader);
        lpset.setPadding(40, 40, 40, 16);
        alertDialog.setView(lpset);
        alertDialog.setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        btnResetCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Log.i("Content ", "Repo Counter");
                setCaptureCounter(0, "");
                String str = "<p>" + nCaptureCounter + "</p> ";

                tvCaptureCount.setMovementMethod(LinkMovementMethod.getInstance());
                tvCaptureCount.setText(Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY));

            }
        });
        alertDialog.show();
    }
}

