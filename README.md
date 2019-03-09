# solaredge-notifier
An Android app to notify you when something is wrong with your SolarEdge PV system.

HOW IT WORKS
============

The alarm
---------
1. An alarm is set to broadcast an intent "AlarmReceiver"
2. This intent goes to the server and receives solar output of "yesterday"
3. If the server does not respond, try again in 15 minutes
4. If the server responds, try again in 24 hours
5. If there is a response, process it:
    5a. If set to always, notify with output of yesterday
    5b. If set to threshold, check threshold and notify if lower
6. If notification is tapped, open the DiagActivity to show yesterday,
    last week, last month output

The main activity
-----------------
The main activity shows the settings of the Notifier. It also has tools
to manage API keys and a way to show the current state of the Sites.

o Enable/Disable: globally enable or disable the notifier. This can come in
  handy when you temporarily want not to receive messages.
o When to notify / Threshold: configure the way the Notifier works
o Manage API keys: add or remove API keys. API keys can be obtained from the
  installer of the system.
o Sites: opens list of the currently found Sites from the API keys
