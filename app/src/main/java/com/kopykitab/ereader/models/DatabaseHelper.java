package com.kopykitab.ereader.models;

import com.kopykitab.ereader.settings.DatabaseConstants.BookEntry;
import com.kopykitab.ereader.settings.DatabaseConstants.StoreBannersEntry;
import com.kopykitab.ereader.settings.DatabaseConstants.RecommendationsEntry;
import com.kopykitab.ereader.settings.DatabaseConstants.StoreCategoryEntry;
import com.kopykitab.ereader.settings.DatabaseConstants.SearchSuggestionsEntry;
import com.kopykitab.ereader.settings.DatabaseConstants.DatabaseEntry;
import com.kopykitab.ereader.settings.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = DatabaseEntry.DATABSE_NAME;
    private static DatabaseHelper mDatabaseInstance = null;

    // Create & Define table queries
    public static final String SEARCH_SUGGESTIONS_TABLE_CREATE_QUERY = "CREATE TABLE " + SearchSuggestionsEntry.TABLE_NAME
            + "(" + SearchSuggestionsEntry.COLUMN_ID + " INTEGER PRIMARY KEY,"
            + SearchSuggestionsEntry.COLUMN_KEYWORD + " TEXT,"
            + SearchSuggestionsEntry.COLUMN_RESULTS + " TEXT,"
            + SearchSuggestionsEntry.COLUMN_DATE + " TEXT"
            + ")";
    public static final String STORE_CATEGORY_TABLE_CREATE_QUERY = "CREATE TABLE " + StoreCategoryEntry.TABLE_NAME
            + "(" + StoreCategoryEntry.COLUMN_ID + " INTEGER PRIMARY KEY,"
            + StoreCategoryEntry.COLUMN_NAME + " TEXT,"
            + StoreCategoryEntry.COLUMN_PARENT_NAME + " TEXT,"
            + StoreCategoryEntry.COLUMN_URL + " TEXT,"
            + StoreCategoryEntry.COLUMN_LEVEL + " INTEGER"
            + ")";

    public static final String RECOMMENDATIONS_TABLE_CREATE_QUERY = "CREATE TABLE " + RecommendationsEntry.TABLE_NAME
            + "(" + RecommendationsEntry.COLUMN_PRODUCT_ID + " INTEGER PRIMARY KEY,"
            + RecommendationsEntry.COLUMN_IMAGE + " TEXT,"
            + RecommendationsEntry.COLUMN_NAME + " TEXT,"
            + RecommendationsEntry.COLUMN_DESCRIPTION + " TEXT,"
            + RecommendationsEntry.COLUMN_RENTAL_PERIOD + " TEXT,"
            + RecommendationsEntry.COLUMN_PRICE_1 + " TEXT,"
            + RecommendationsEntry.COLUMN_PRICE_2 + " TEXT,"
            + RecommendationsEntry.COLUMN_FREE_PRODUCT + " TEXT,"
            + RecommendationsEntry.COLUMN_HREF + " TEXT,"
            + RecommendationsEntry.COLUMN_PRODUCT_TYPE + " TEXT"
            + ")";

    public static final String STORE_BANNERS_TABLE_CREATE_QUERY = "CREATE TABLE " + StoreBannersEntry.TABLE_NAME
            + "(" + StoreBannersEntry.COLUMN_ID + " INTEGER PRIMARY KEY,"
            + StoreBannersEntry.COLUMN_IMAGE_URL + " TEXT,"
            + StoreBannersEntry.COLUMN_DESCRIPTION + " TEXT,"
            + StoreBannersEntry.COLUMN_HREF + " TEXT"
            + ")";

    public static final String DOWNLOADED_BOOKS_TABLE_CREATE_QUERY = "CREATE TABLE " + DatabaseEntry.DOWNLOADED_BOOKS_TABLE_NAME
            + "(" + BookEntry.COLUMN_ID + " INTEGER PRIMARY KEY,"
            + BookEntry.COLUMN_CUSTOMER_ID + " TEXT,"
            + BookEntry.COLUMN_PRODUCT_ID + " TEXT,"
            + BookEntry.COLUMN_ORDER_PRODUCT_ID + " TEXT,"
            + BookEntry.COLUMN_CIDD + " TEXT,"
            + BookEntry.COLUMN_NAME + " TEXT,"
            + BookEntry.COLUMN_IMAGE_URL + " TEXT,"
            + BookEntry.COLUMN_DESCRIPTION + " TEXT,"
            + BookEntry.COLUMN_PRICE + " TEXT,"
            + BookEntry.COLUMN_LEFT_DAYS + " TEXT,"
            + BookEntry.COLUMN_PRODUCT_TYPE + " TEXT,"
            + BookEntry.COLUMN_PDF_DOWNLOADED_DATE + " TEXT,"
            + BookEntry.COLUMN_LICENCE_PERIOD + " TEXT,"
            + BookEntry.COLUMN_PRODUCT_LINK + " TEXT"
            + ")";


    // Drop table queries
    private static final String SEARCH_SUGGESTIONS_TABLE_DELETE_QUERY = "DROP TABLE IF EXISTS " + SearchSuggestionsEntry.TABLE_NAME;
    private static final String STORE_CATEGORY_TABLE_DELETE_QUERY = "DROP TABLE IF EXISTS " + StoreCategoryEntry.TABLE_NAME;
    private static final String RECOMMENDATIONS_TABLE_DELETE_QUERY = "DROP TABLE IF EXISTS " + RecommendationsEntry.TABLE_NAME;
    private static final String STORE_BANNERS_TABLE_DELETE_QUERY = "DROP TABLE IF EXISTS " + StoreBannersEntry.TABLE_NAME;
    private static final String DOWNLOADED_BOOKS_TABLE_DELETE_QUERY = "DROP TABLE IF EXISTS " + DatabaseEntry.DOWNLOADED_BOOKS_TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, Utils.getDirectory(context) + DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        //To access database, instantiate subclass of SQLiteOpenHelper
        if (mDatabaseInstance == null) {
            mDatabaseInstance = new DatabaseHelper(context);
        }
        return mDatabaseInstance;
    }

    public static void resetDatabaseInstance() {
        mDatabaseInstance = null;
    }

    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(SEARCH_SUGGESTIONS_TABLE_CREATE_QUERY);
        db.execSQL(STORE_CATEGORY_TABLE_CREATE_QUERY);
        db.execSQL(RECOMMENDATIONS_TABLE_CREATE_QUERY);
        db.execSQL(STORE_BANNERS_TABLE_CREATE_QUERY);
        db.execSQL(DOWNLOADED_BOOKS_TABLE_CREATE_QUERY);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL(SEARCH_SUGGESTIONS_TABLE_DELETE_QUERY);
        db.execSQL(STORE_CATEGORY_TABLE_DELETE_QUERY);
        db.execSQL(RECOMMENDATIONS_TABLE_DELETE_QUERY);
        db.execSQL(STORE_BANNERS_TABLE_DELETE_QUERY);
        db.execSQL(DOWNLOADED_BOOKS_TABLE_DELETE_QUERY);

        // create new tables
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Check if the database exist and can be read.
     *
     * @return true if it exists and can be read, false if it doesn't
     */
    public static boolean checkDatabase(Context mContext) {
        boolean isDatabase = false;
        try {
            File dbFile = mContext.getDatabasePath(Utils.getDirectory(mContext) + DATABASE_NAME);
            isDatabase = dbFile.exists();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isDatabase;
    }

    public static boolean validateDatabaseAndTableHasData(Context mContext, String tableName) {
        boolean isTableEmpty = false;
        SQLiteDatabase checkDB = null;
        try {
            checkDB = SQLiteDatabase.openDatabase(Utils.getDirectory(mContext) + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            if (checkDB != null) {
                Cursor cursor = checkDB.rawQuery("SELECT * FROM " + tableName, null);
                isTableEmpty = cursor.moveToFirst();
                checkDB.close();
            }
        } catch (SQLiteException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isTableEmpty;
    }

    public static boolean isDatabaseVersionConflict(Context mContext) {
        boolean isConflict = false;
        SQLiteDatabase database = null;
        try {
            database = SQLiteDatabase.openDatabase(Utils.getDirectory(mContext) + DATABASE_NAME, null, 0);
            int databaseVersion = database.getVersion();
            database.close();
            if (databaseVersion != DATABASE_VERSION) {
                isConflict = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (database != null) {
                database.close();
            }
        }

        return isConflict;
    }

    public static void handleDatabaseVersionConflict(Context mContext) {
        SQLiteDatabase database = null;
        try {
            database = SQLiteDatabase.openDatabase(Utils.getDirectory(mContext) + DATABASE_NAME, null, 0);
            DATABASE_VERSION = database.getVersion();
            mDatabaseInstance = new DatabaseHelper(mContext);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    public void deleteData(String query) {
        SQLiteDatabase databaseWritable = null;

        try {
            // Gets the data repository in write mode
            databaseWritable = mDatabaseInstance.getWritableDatabase();

            //start transactions
            databaseWritable.beginTransaction();

            // truncate table
            databaseWritable.execSQL(query);

            //commit transactions
            databaseWritable.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (databaseWritable != null) {
                databaseWritable.endTransaction();
                databaseWritable.close();
            }
        }
    }

    public long insertSingleData(String tableName, ContentValues values) {
        SQLiteDatabase databaseWritable = null;
        long newRowId = -1;

        try {
            // Gets the data repository in write mode
            databaseWritable = mDatabaseInstance.getWritableDatabase();

            //start transactions
            databaseWritable.beginTransaction();

            // Insert the new row, returning the primary key value of the new row or -1 if an error occurred
            newRowId = databaseWritable.insert(tableName, null, values);

            //commit transactions
            databaseWritable.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (databaseWritable != null) {
                databaseWritable.endTransaction();
                databaseWritable.close();
            }
        }

        return newRowId;
    }

    public long insertOrUpdateData(String tableName, ContentValues insertValues, ContentValues updateValues, String selection, String[] selectionArgs) {
        SQLiteDatabase databaseWritable = null;
        long newRowId = -1;

        try {
            // Gets the data repository in write mode
            databaseWritable = mDatabaseInstance.getWritableDatabase();

            //start transactions
            databaseWritable.beginTransaction();

            // Insert the new row or Update existing row, returning the primary key value of the new row or existing row or -1 if an error occurred
            newRowId = databaseWritable.update(tableName, updateValues, selection, selectionArgs);
            if (newRowId == 0) {
                newRowId = (int) databaseWritable.insertWithOnConflict(tableName, null, insertValues, SQLiteDatabase.CONFLICT_REPLACE);
            }

            //commit transactions
            databaseWritable.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (databaseWritable != null) {
                databaseWritable.endTransaction();
                databaseWritable.close();
            }
        }

        return newRowId;
    }

    public long insertMultipleData(String tableName, LinkedList<ContentValues> valuesList) {
        SQLiteDatabase databaseWritable = null;
        long newRowId = -1;

        try {
            // Gets the data repository in write mode
            databaseWritable = mDatabaseInstance.getWritableDatabase();

            //start transactions
            databaseWritable.beginTransaction();

            // Insert the new row, returning the primary key value of the new row or -1 if an error occurred
            for (ContentValues values : valuesList) {
                newRowId = databaseWritable.insert(tableName, null, values);
            }

            //commit transactions
            databaseWritable.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (databaseWritable != null) {
                databaseWritable.endTransaction();
                databaseWritable.close();
            }
        }

        return newRowId;
    }

    public List<HashMap<String, String>> getDetails(String tableName, String whereCaluse, String sortColumn, int limit) {
        LinkedList<HashMap<String, String>> allDetails = new LinkedList<HashMap<String, String>>();
        SQLiteDatabase databaseReadable = null;
        Cursor cursor = null;

        try {
            String selectQuery = "SELECT  * FROM " + tableName;

            if (whereCaluse != null && whereCaluse.length() > 0) {
                selectQuery += " " + whereCaluse;
            }

            if (sortColumn != null && sortColumn.length() > 0) {
                selectQuery += " ORDER BY " + sortColumn + " DESC";
            }

            if (limit != -1) {
                selectQuery += " LIMIT " + limit;
            }


            // Gets the data repository in Read mode
            databaseReadable = mDatabaseInstance.getReadableDatabase();
            cursor = databaseReadable.rawQuery(selectQuery, null);

            while (cursor.moveToNext()) {
                int i = 0;
                HashMap<String, String> data = new HashMap<String, String>();
                while (cursor.getColumnCount() != i) {
                    data.put(cursor.getColumnName(i), cursor.getString(i));
                    i++;
                }

                allDetails.add(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }

            if (databaseReadable != null) {
                databaseReadable.close();
            }
        }

        return allDetails;
    }
}