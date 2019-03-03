package nl.friesoft.solaredgenotifier

interface ApiKeyCallbacks {
    fun onApiKeyDeleted(apikey: String)
    fun onApiKeyAdded(apikey: String)
}