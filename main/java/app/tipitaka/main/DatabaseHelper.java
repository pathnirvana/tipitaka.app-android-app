package app.tipitaka.main;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    public SQLiteDatabase myDataBase;

    private String vDbName; //the extension may be .sqlite or .db
    private int dbVersion;
    private String inDbAssetsPath;
    private File outDbFile;
    private static Map<String, String> dbVersions = new HashMap<>();

    public DatabaseHelper(Context context, String vDbName, String inDbPath) throws IOException {
        super(context, vDbName, null, 1);
        this.context = context;
        this.vDbName = vDbName;
        this.inDbAssetsPath = inDbPath;

        this.outDbFile = context.getDatabasePath(vDbName);
        if (outDbFile.exists()) {
            Log.d("LOG_TAG", "Database exists " + vDbName);
        } else {
            Log.d("LOG_TAG", "Database doesn't exist " + vDbName);
            // try to delete any older versions before copying the new version
            deleteOldVersions(vDbName);

            // show a simple progress message in case copying large dbs in old phones takes a long time
            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setMessage("Copying the database " + vDbName + ". Please wait.");
            dialog.show();

            this.getReadableDatabase();
            try {
                copyDatabase();
            } catch (IOException e) {
                Log.d("LOG_TAG", "Error copying db " + e.toString());
                dialog.dismiss();
                throw new Error("Error copying database");
            }
            dialog.dismiss();
        }
        myDataBase = SQLiteDatabase.openDatabase(outDbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
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

    private void copyDatabase() throws IOException {
        //Open your local db as the input stream
        InputStream myInput = context.getAssets().open(inDbAssetsPath);

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outDbFile);

        // transfer byte to inputfile to outputfile
        byte[] buffer = new byte[1024 * 512];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    public synchronized void close() {
        if (myDataBase != null) {
            myDataBase.close();
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