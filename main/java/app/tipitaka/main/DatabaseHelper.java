package app.tipitaka.main;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipInputStream;

import static java.lang.Thread.sleep;

public class DatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    private SQLiteDatabase db = null;

    private String vDbName; //the extension may be .sqlite or .db
    private int dbVersion;
    private String inDbAssetsPath;
    private File outDbFile;

    public DatabaseHelper(Context context, String vDbName, String inDbPath) {
        super(context, vDbName, null, 1);
        this.context = context;
        this.vDbName = vDbName;
        this.inDbAssetsPath = inDbPath;

        this.outDbFile = context.getDatabasePath(vDbName);
        if (outDbFile.exists()) {
            Log.d("LOG_TAG", "Database exists " + vDbName);
            openDatabase();
        } else {
            Log.d("LOG_TAG", "Database doesn't exist " + vDbName);
            // try to delete any older versions before copying the new version
            deleteOldVersions(vDbName);

            // show a simple progress message in case copying large dbs in old phones takes a long time
            final ProgressDialog dialog = new ProgressDialog(context);
            dialog.setMessage("Copying the database " + vDbName + ". Please wait few minutes.");
            dialog.setCancelable(false);
            dialog.show();

            // dialog will not show up unless a new thread is created
            Thread mThread = new Thread() {
                @Override public void run() {
                    getReadableDatabase();
                    try {
                        int copiedSize;
                        // when building apk for playstore uncomment the condition below
                        if (inDbAssetsPath.equals("static/db/dict-all.db")) {
                            copiedSize = copyFromURL("https://tipitaka.lk/library/674");
                        } else {
                            copiedSize = copyFromAssets();
                        }
                        Log.d("LOG_TAG", "Copied db " + inDbAssetsPath + " of size " + copiedSize);
                    } catch (IOException e) {
                        Log.e("LOG_TAG", "Error copying db " + e.toString());
                        dialog.dismiss();
                    }
                    openDatabase();
                    dialog.dismiss();
                }
            };
            mThread.start();
        }
    }

    private void openDatabase() {
        db = SQLiteDatabase.openDatabase(outDbFile.getPath(), null,
                SQLiteDatabase.OPEN_READONLY);
    }

    public Cursor runQuery(String sql, String[] params) {
        if (db == null) { // wait until the db copy is finished and db opened
            throw new Error("Please wait until db copy finished and search again.");
        }
        return db.rawQuery(sql, params);
    }

    private void deleteOldVersions(String vDbName) {
        String[] parts = vDbName.split("@");
        int dbVersion = Integer.parseInt(parts[0]);
        while (dbVersion > 0) {
            dbVersion--;
            File oldDbFile = context.getDatabasePath("" + dbVersion + "@" + parts[1]);
            if (oldDbFile.exists()) {
                Log.d("LOG_TAG", "Deleting old db " + oldDbFile.getPath());
                SQLiteDatabase.deleteDatabase(oldDbFile);
            }
        }
    }

    private int copyFromAssets() throws IOException {
        //Open your local db as the input stream
        InputStream assetsIn = context.getAssets().open(inDbAssetsPath);

        //Open the empty db as the output stream
        OutputStream databaseOut = new FileOutputStream(outDbFile);

        // transfer byte to inputfile to outputfile
        byte[] buffer = new byte[1024 * 128];
        int length, copiedSize = 0;
        while ((length = assetsIn.read(buffer)) > 0) {
            databaseOut.write(buffer, 0, length);
            copiedSize += length;
        }

        //Close the streams
        databaseOut.flush();
        databaseOut.close();
        assetsIn.close();
        return copiedSize;
    }

    private int copyFromURL(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(url.openStream()));
        OutputStream databaseOut = new FileOutputStream(outDbFile);
        int length, copiedSize = 0;
        byte[] buffer = new byte[8192];
        try {
            zis.getNextEntry(); // not sure if this is necessary
            while ((length = zis.read(buffer)) != -1) {
                databaseOut.write(buffer, 0, length);
                copiedSize += length;
            }
        } finally {
            zis.close();
            databaseOut.flush();
            databaseOut.close();
        }
        return copiedSize;
    }

    public synchronized void close() {
        if (db != null) {
            db.close();
        }
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
}