package nl.friesoft.solaredgenotifier

data class Site(val apikey: String, val id: Int) {
    companion object {
        val INVALID = Site("", 0)
    }

    lateinit var name: String
}
