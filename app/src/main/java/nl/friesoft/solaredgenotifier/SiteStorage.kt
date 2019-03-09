package nl.friesoft.solaredgenotifier

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SiteStorage(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val add_city = "ALTER TABLE $TBL_SITES ADD COLUMN $COL_CITY text"
        val add_country = "ALTER TABLE $TBL_SITES ADD COLUMN $COL_COUNTRY text"

        if (oldVersion < 2) {
            db?.execSQL(add_city)
            db?.execSQL(add_country)
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE = "CREATE TABLE $TBL_SITES ($COL_APIKEY text, $COL_ID integer, $COL_NAME text, " +
                "PRIMARY KEY ($COL_APIKEY, $COL_ID))"
        db?.execSQL(CREATE_TABLE)
    }

    companion object {
        private val DB_NAME = "solaredge_notifier"
        private val DB_VERSION = 2
        private val TBL_SITES = "sites"

        // v1
        private val COL_ID = "id"
        private val COL_APIKEY = "apikey"
        private val COL_NAME = "name"

        // v2
        private val COL_CITY = "city"
        private val COL_COUNTRY = "country"
    }

    fun delete(apikey: String = "") {
        val db = this.writableDatabase
        if ("".equals(apikey)) {
            // empty api key, delete all
            db.delete(TBL_SITES, "", null)
        } else {
            db.delete(TBL_SITES, "$COL_APIKEY=?", arrayOf(apikey))
        }
        db.close()
    }

    fun add(s: Site) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_APIKEY, s.apikey)
        values.put(COL_ID, s.id)
        values.put(COL_NAME, s.name)

        values.put(COL_CITY, s.city)
        values.put(COL_COUNTRY, s.country)

        db.insert(TBL_SITES, null, values)
        db.close()
    }

    fun count(): Long {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TBL_SITES", null)

        cursor.moveToFirst()
        val r = cursor.getLong(0)

        cursor.close()
        db.close()

        return r
    }

    fun get(idx: Int): Site {
        val db = this.readableDatabase
        val cursor = db.query(TBL_SITES, arrayOf(COL_APIKEY, COL_ID, COL_NAME, COL_CITY, COL_COUNTRY), null, null,
                null, null, "$COL_NAME", "$idx, 1")

        cursor.moveToFirst()

        val site : Site
        if (cursor != null) {
            site = Site(cursor.getString(0), cursor.getInt(1))
            site.name = cursor.getString(2)

            site.city = cursor.getString(3)?:""
            site.country = cursor.getString(4)?:""
        } else {
            site = Site.INVALID
        }
        return site
    }
}
