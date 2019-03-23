package nl.friesoft.solaredgenotifier

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_sites.*

class SitesActivity : AppCompatActivity(), ISolarEdgeListener {
    override fun onSiteFound(site: Site?) {
        if (site != null) {
            siteStorage.add(site)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onError(site: Site?, exception: SolarEdgeException?) {
    }

    override fun onEnergy(site: Site?, result: Energy?) {
    }

    override fun onDetails(site: Site?) {
    }

    val siteStorage = SiteStorage(this)

    private lateinit var adapter: SiteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sites)

        setTitle(R.string.sites)

        var t = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(t)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        adapter = SiteAdapter(this, siteStorage)
        lvSites.adapter = adapter

        lvSites.setOnItemClickListener{ adapterView, view, i, l ->
            var intent: Intent = Intent(this, SiteActivity::class.java)
            intent.putExtra(AlarmReceiver.EXTRA_API_KEY, (adapter.getItem(i) as Site).apikey)
            intent.putExtra(AlarmReceiver.EXTRA_INSTALLATION_ID, (adapter.getItem(i) as Site).id)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sites, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.menuitem_refresh -> refresh()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun refresh() {
        // refreshes the stored list of Sites, using all the stored API keys
        var persistent = Persistent(this)

        siteStorage.delete()
        val apikeys = persistent.getStringSet(PrefFragment.PREF_API_KEY, emptySet())
        for (apikey in apikeys) {
            val solareEdge = SolarEdge(this)
            solareEdge.sites(apikey)
        }
    }
}

class SiteAdapter(ctx: Context, siteStorage: SiteStorage) : BaseAdapter() {
    val mInflater = LayoutInflater.from(ctx)
    val siteStorage = siteStorage

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
        val newView = view ?: mInflater.inflate(R.layout.list_item_site, viewGroup, false)

        val site = siteStorage.get(i)

        newView.findViewById<TextView>(R.id.tvName).setText(site.name)
        newView.findViewById<TextView>(R.id.tvLocation).setText(String.format("%s, %s", site.city, site.country))

        val ivStatus = newView.findViewById<ImageView>(R.id.ivStatus)
        when (site.status) {
            Site.STATUS_BELOWAVG -> ivStatus.setImageResource(R.drawable.outline_wb_cloudy_24)
            Site.STATUS_BELOWFIXED -> ivStatus.setImageResource(R.drawable.baseline_warning_24)
            else -> ivStatus.setImageResource(R.drawable.outline_wb_sunny_24)
        }

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
