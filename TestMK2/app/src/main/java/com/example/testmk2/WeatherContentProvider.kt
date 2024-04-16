package com.example.testmk2

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri

class WeatherContentProvider : ContentProvider() {

    private lateinit var dbHelper: WeatherDbHelper

    override fun onCreate(): Boolean {
        dbHelper = WeatherDbHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        return db.query(
            WeatherDataContract.PATH_WEATHER,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        )
    }

    override fun getType(uri: Uri): String? {
        return null // Not used in this example
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id = db.insert(WeatherDataContract.PATH_WEATHER, null, values)
        return ContentUris.withAppendedId(uri, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        return db.delete(WeatherDataContract.PATH_WEATHER, selection, selectionArgs)
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = dbHelper.writableDatabase
        return db.update(WeatherDataContract.PATH_WEATHER, values, selection, selectionArgs)
    }
}




class WeatherDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = "CREATE TABLE ${WeatherDataContract.PATH_WEATHER} (" +
                "${WeatherDataContract.Columns.ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "${WeatherDataContract.Columns.UPDATE_AT} TEXT, " +
                "${WeatherDataContract.Columns.TEMP} TEXT, " +
                "${WeatherDataContract.Columns.TEMP_MIN} TEXT, " +
                "${WeatherDataContract.Columns.TEMP_MAX} TEXT, " +
                "${WeatherDataContract.Columns.PRESSURE} TEXT, " +
                "${WeatherDataContract.Columns.HUMIDITY} TEXT, " +
                "${WeatherDataContract.Columns.FEELS_LIKE} TEXT, " +
                "${WeatherDataContract.Columns.SUNRISE} TEXT, " +
                "${WeatherDataContract.Columns.SUNSET} TEXT, " +
                "${WeatherDataContract.Columns.WIND_SPEED} TEXT, " +
                "${WeatherDataContract.Columns.WEATHER_DESCRIPTION} TEXT, " +
                "${WeatherDataContract.Columns.ADDRESS} TEXT)"
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${WeatherDataContract.PATH_WEATHER}")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "weather.db"
        private const val DATABASE_VERSION = 1
    }
}
