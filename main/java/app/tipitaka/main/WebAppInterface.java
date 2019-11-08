package app.tipitaka.main;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.database.Cursor.FIELD_TYPE_FLOAT;
import static android.database.Cursor.FIELD_TYPE_INTEGER;
import static android.database.Cursor.FIELD_TYPE_STRING;
import static java.lang.Thread.sleep;

public class WebAppInterface {
    Context mContext;
    Map<String, DatabaseHelper> openedDbs;
    JSONObject dbVersions;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        openedDbs = new HashMap<>();
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void initDbVersions(String jsonString) {
        Log.d("LOG_TAG", "db versions initialized string " + jsonString);
        try {
            dbVersions = new JSONObject(jsonString);
        } catch (JSONException ex) {
            Log.e("LOG_TAG", "malformed version string " + jsonString);
        }
    }

    @JavascriptInterface
    public String openDb(String dbPath){
        String vDbName = getVersionedDbName(dbPath);
        Log.d("LOG_TAG", "opendb called " + dbPath + " versioned " + vDbName);

        if (!openedDbs.containsKey(vDbName)) {
            File fDbPath = new File(dbPath);
            openedDbs.put(vDbName,
                    new DatabaseHelper(mContext, vDbName, dbPath));
        }
        return vDbName;
    }

    @JavascriptInterface
    public String all(String vDbName, String sql, String[] params) {
        DatabaseHelper dbHelper = openedDbs.get(vDbName);
        return cursorToJson(dbHelper.runQuery(sql, params));
    }

    private String getVersionedDbName(String dbPath) {
        File fDbPath = new File(dbPath);
        int dbVersion = 0;
        try {
            String dbName = fDbPath.getName().split("\\.")[0];
            dbVersion = dbVersions.getInt(dbName);
        } catch (JSONException ex) {}
        return "" + dbVersion + "@" + fDbPath.getName();
    }

    private static String cursorToJson(Cursor cursor) {
        JSONArray resultSet = new JSONArray();
        JSONObject returnObj = new JSONObject();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();

            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        switch(cursor.getType(i)) {
                            case FIELD_TYPE_INTEGER:
                                rowObject.put(cursor.getColumnName(i), cursor.getInt(i));
                                break;
                            case FIELD_TYPE_FLOAT:
                                rowObject.put(cursor.getColumnName(i), cursor.getDouble(i));
                                break;
                            case FIELD_TYPE_STRING:
                                rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                                break;
                            default: //NULL or BLOB put empty
                                rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch (Exception e) {
                        Log.d("LOG_TAG", e.getMessage());
                    }
                }
            }

            resultSet.put(rowObject);
            cursor.moveToNext();
        }

        cursor.close();
        Log.d("LOG_TAG", "resultSet size " + resultSet.length());
        try {
            return resultSet.toString(2);
        } catch (JSONException ex) {
            return ex.toString();
        }
    }
}
