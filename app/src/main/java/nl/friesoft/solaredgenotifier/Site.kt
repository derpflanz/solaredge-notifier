package nl.friesoft.solaredgenotifier

data class Site(val apikey: String, val id: Int) {
    companion object {
        val INVALID = Site("", 0)

        const val STATUS_OK = 0
        const val STATUS_BELOWFIXED = 1
        const val STATUS_BELOWAVG = 2
    }

    lateinit var name: String
    lateinit var city: String
    lateinit var country: String
    var status: Int = 0
}
