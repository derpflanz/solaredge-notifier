package nl.friesoft.solaredgenotifier

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

public class SiteStorage(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val add_city = "ALTER TABLE $TBL_SITES ADD COLUMN $COL_CITY text"
        val add_country = "ALTER TABLE $TBL_SITES ADD COLUMN $COL_COUNTRY text"
        val add_status = "ALTER TABLE $TBL_SITES ADD COLUMN $COL_STATUS integer"

        /* How to handle database upgrades?
           1. Define the update that goes with a certain_version
           2. Place the ALTER commands in a if (oldVersion < certain_version) block
           3. Update the onCreate to have the final last super version
         */

        if (oldVersion < 2) {
            db?.execSQL(add_city)
            db?.execSQL(add_country)
        }

        if (oldVersion < 3) {
            db?.execSQL(add_status)
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE = "CREATE TABLE $TBL_SITES ($COL_APIKEY text, $COL_ID integer, $COL_NAME text, " +
                "$COL_COUNTRY text, $COL_CITY text, $COL_STATUS integer, PRIMARY KEY ($COL_APIKEY, $COL_ID))"
        db?.execSQL(CREATE_TABLE)
    }

    public companion object {
        const val DB_NAME = "solaredge_notifier"
        const val DB_VERSION = 3
        const val TBL_SITES = "sites"

        // v1
        const val COL_ID = "id"
        const val COL_APIKEY = "apikey"
        const val COL_NAME = "name"

        // v2
        const val COL_CITY = "city"
        const val COL_COUNTRY = "country"

        // v3
        const val COL_STATUS = "status"
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

        // v1
        values.put(COL_APIKEY, s.apikey)
        values.put(COL_ID, s.id)
        values.put(COL_NAME, s.name)

        // v2
        values.put(COL_CITY, s.city)
        values.put(COL_COUNTRY, s.country)

        // v3
        values.put(COL_STATUS, s.status)

        db.insert(TBL_SITES, null, values)
        db.close()
    }

    fun update(s: Site, cv: ContentValues) {
        val db = this.writableDatabase
        db.update(TBL_SITES, cv, "$COL_APIKEY=? AND $COL_ID=?", arrayOf(s.apikey, s.id.toString()))
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
        val cursor = db.query(TBL_SITES, arrayOf(COL_APIKEY, COL_ID, COL_NAME, COL_CITY, COL_COUNTRY, COL_STATUS), null, null,
                null, null, "$COL_NAME", "$idx, 1")

        cursor.moveToFirst()

        val site : Site
        if (cursor != null) {
            site = Site(cursor.getString(0), cursor.getInt(1))
            site.name = cursor.getString(2)

            site.city = cursor.getString(3)?:""
            site.country = cursor.getString(4)?:""
            site.status = cursor.getInt(5)
        } else {
            site = Site.INVALID
        }
        return site
    }
}
