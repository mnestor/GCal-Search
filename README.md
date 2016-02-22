# GCal-Search

Steps to set this up...

1. Create a Google Project - https://console.developers.google.com
 - Give it any name
 - Enable the Calendar API - https://console.developers.google.com/apis/library
 - Setup new credentials - https://console.developers.google.com/apis/credentials
 - Enable OAuth with a redirect URI of: ```https://graph.api.smartthings.com/oauth/callback```
 - Copy the Client ID and Client Secret, you will need these later
2. Install the 2 SmartApps "[GCal Search](https://raw.githubusercontent.com/mnestor/GCal-Search/master/smartapps/mnestor/gcal-search.src/gcal-search.groovy)" and "[GCal Search Trigger](https://raw.githubusercontent.com/mnestor/GCal-Search/master/smartapps/mnestor/gcal-search-trigger.src/gcal-search-trigger.groovy)"
 - https://graph.api.smartthings.com/ide/app/create
 - Enable OAuth for "GCal Search"
 - Put the ClientID and Client Secret you copied from Step 1 into the Settings for "GCal Search"
 - Publish the Apps
3. Install and Publish the [Device Type](https://raw.githubusercontent.com/mnestor/GCal-Search/master/devicetypes/mnestor/gcal-event-sensor.src/gcal-event-sensor.groovy)
 - https://graph.api.smartthings.com/ide/device/create

Open the ST app on your phone and install the "GCal Search" app.
This will walk you through connecting to Google and selecting a calendar and search terms.
You can create multiple connections, the app will create Contact Sensors that are Open when the event starts and close when the event ends.
