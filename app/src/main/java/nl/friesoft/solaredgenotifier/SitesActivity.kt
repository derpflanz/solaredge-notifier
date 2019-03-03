package nl.friesoft.solaredgenotifier

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SitesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sites)

        setTitle(R.string.sites)

        var t = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(t)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val lvSites = findViewById<ListView>(R.id.lvSites)
        lvSites.adapter = SiteAdapter(this)
    }
}

class SiteAdapter(val ctx: Context) : BaseAdapter() {
    val siteStorage = SiteStorage(ctx)
    val mInflater = LayoutInflater.from(ctx)

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
        val newView = view ?: mInflater.inflate(android.R.layout.simple_list_item_2, viewGroup, false)

        val site = siteStorage.get(i)

        newView.findViewById<TextView>(android.R.id.text1).setText(site.name)
        newView.findViewById<TextView>(android.R.id.text2).setText(site.id.toString())

        return newView
    }

    override fun getItem(i: Int): Any {
        return siteStorage.get(i)
    }

    override fun getItemId(i: Int): Long {
        return siteStorage.get(i).id.toLong()
    }

    override fun getCount(): Int {
        return siteStorage.count().toInt()
    }
}
