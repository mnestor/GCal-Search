/**
 *  Copyright 2016 Mike Nestor
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
/**
 *
 * Updates:
 * 20160411.1 - Change schedule to happen in the child app instead of the device
 * 20160332.2 - Updated date parsing for non-fullday events
 * 20160331.1 - Fix for all day event attempt #2
 * 20160319.1 - Fix for all day events
 * 20160302.1 - Allow for polling of device version number
 * 20160301.1 - GUI fix for white space
 * 20160223.4 - Fix for Dates in UK
 * 20160223.3 - Fix for DateFormat, set the closeTime before we call open() on in progress event to avoid exception
 * 20160223.1 - Error checking - Force check for Device Handler so we can let the user have a more informative error
 *
 */
 
metadata {
	// Automatically generated. Make future change here.
	definition (name: "GCal Event Sensor", namespace: "mnestor", author: "mnestor") {
		capability "Contact Sensor"
		capability "Sensor"
        capability "Refresh"
        capability "Switch"

		command "open"
		command "close"
        
        attribute "eventSummary", "string"
        attribute "openTime", "number"
        attribute "closeTime", "number"
	}

	simulator {
		status "open": "contact:open"
		status "closed": "contact:closed"
	}

	tiles (scale: 2) {
		standardTile("status", "device.contact", width: 2, height: 2) {
			state("closed", label:'', icon:"https://raw.githubusercontent.com/mnestor/GCal-Search/icons/icons/GCal-Off@2x.png", backgroundColor:"#79b821")
			state("open", label:'', icon:"https://raw.githubusercontent.com/mnestor/GCal-Search/icons/icons/GCal-On@2x.png", backgroundColor:"#ffa81e")
		}
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width:4, height: 2) {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
                
        valueTile("summary", "device.eventSummary", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
            state "default", label:'${currentValue}'
        }
        
		main "status"
		details(["status", "refresh", "summary"])
	}
}

def installed() {
	sendEvent(name: "contact", value: "closed")
}

def parse(String description) {}

// refresh status
def refresh() {
	log.trace "refresh()"
    
    parent.refresh()
    poll() //do one now and make sure we schedule
}

def open() {
	log.trace "open()"
	sendEvent(name: "contact", value: "open")
    sendEvent(name: "switch", value: "on")
    
    def closeTime = new Date(device.currentState("closeTime").value)
    log.debug "Setting up Close for: ${closeTime}"
    sendEvent("name":"closeTime", "value":closeTime)
    parent.schedule(closeTime, "close", [overwrite: true])
}

def close() {
	log.trace "close()"
    sendEvent(name: "contact", value: "closed")
    sendEvent(name: "switch", value: "off")
}

void poll() {
    log.trace "poll()"
    def items = parent.getNextEvents()
    log.trace "Items: ${items}"
    try {
    
    def currentState = device.currentState("contact").value
    def isOpen = currentState == "open"
    log.debug "State is: ${currentState}"
    
    if (items && items.items && items.items.size() > 0) {
    	log.debug "we have events!"
        
        //we only handle the first event returned
        def event = items.items[0]
        
        log.debug "Event: ${event}"
        
        def start
        def end
        if (event.start.containsKey('date')) {
        	//this is for full day events
        	//get start and end dates adjusting for timezone
            def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
            sdf.setTimeZone(TimeZone.getTimeZone(items.timeZone))
            start = sdf.parse(event.start.date)
            end = new Date(sdf.parse(event.end.date).time - 60)
        } else {
            def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
            sdf.setTimeZone(TimeZone.getTimeZone(items.timeZone))
            start = sdf.parse(event.start.dateTime)
            end = sdf.parse(event.end.dateTime)
        }
        log.debug "Start: " + start
        log.debug "End: " + end
        
        def eventSummary = "Title: ${event.summary}\n"
        def startHuman = start.format("EEE, d MMM yyyy hh:mm a", location.timeZone)
        eventSummary += "Open: ${startHuman}\n"
        def endHuman = end.format("EEE, d MMM yyyy hh:mm a", location.timeZone)
        eventSummary += "Close: ${endHuman}\n"
        eventSummary += "Calendar: ${event.organizer.displayName}\n"
        eventSummary += event.description ? event.description : ""
        
        sendEvent("name":"eventSummary", "value":eventSummary)
        
        //we need the close time set before we open an event in progress
        //set the closeTime attribute, in the open() call we'll setup the timer for close
        //this way we don't keep timers open in case the start time gets cancelled
        sendEvent("name":"closeTime", "value":end)
        
        //check if we're already in the event
        if (start < new Date()) {
        	if (!isOpen) { open() }
        } else {
        	if (isOpen) { close() }
            log.debug "Setting up Open for: ${start}"
            sendEvent("name":"openTime", "value":start)
        	parent.schedule(start, "open", [overwrite: true])
        }
        
    } else {
    	sendEvent("name":"eventSummary", "value":"No events found")
    	if (isOpen) { close() }
        else { try { unschedule(open) } catch (e) {} }
    }
    
    } catch (e) {
    	log.warn "Failed to do poll: ${e}"
    }
}

def setRefresh(min) {
	log.trace "Setting refresh: ${min}"
	sendEvent("name":"refreshTime", "value":min)
}
def version(){
	def text = "20160411.1"
}
