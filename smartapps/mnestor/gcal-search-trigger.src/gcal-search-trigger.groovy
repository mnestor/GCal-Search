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

definition(
    name: "GCal Search Trigger",
    namespace: "mnestor",
    author: "Mike Nestor",
    description: "Integrates SmartThings with Google Calendar to trigger events based on calendar items.",
    category: "My Apps",
    parent: "mnestor:GCal Search",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/calendar_contact-accelerometer.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/calendar_contact-accelerometer@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/calendar_contact-accelerometer@2x.png",
) {}

preferences {
	page(name: "selectCalendars")
}

def selectCalendars() {
	log.debug "selectCalendars()"
    
    def calendars = parent.getCalendarList()
        
    return dynamicPage(name: "selectCalendars", title: "Create new calendar search", install: true, uninstall: childCreated()) {
            section("Required") {
                input name: "name", type: "text", title: "Assign a Name", required: true, multiple: false
                
                //we can't do multiple calendars because the api doesn't support it and it could potentially cause a lot of traffic to happen
                input name: "watchCalendars", title:"", type: "enum", required:true, multiple:false, description: "Which calendars do you want to search?", metadata:[values:calendars]
            }
            section("Free-Form Search") {
                paragraph "Leave search blank to match every event on the selected calendar(s)"
                paragraph "Searches for entries that have all terms\nTo search for an exact phrase, enclose the phrase in quotation marks: \"exact phrase\"\nTo exclude entries that match a given term, use the form -term\nExamples:\nHoliday (anything with Holiday)\n\"#Holiday\" (anything with #Holiday)\n#Holiday (anything with Holiday, ignores the #)"
                input name: "search", type: "text", title: "Search String", required: false
            }
            section("Optional") {
            	input name: "refresh", type: "number", title: "Time in minutes to refresh device", required: false, defaultValue: 15, range: "5..86400"
            }
        }
}

def installed() {
    log.debug "installed()"
}


def updated() {
	log.debug "Updated with settings: ${settings}"

	//we have nothing to subscribe to yet
    //leave this just in case something crazy happens though
	unsubscribe()
    
	initialize()
}

def initialize() {
    log.debug "initialize()"
    app.updateLabel(settings.name)
    def device
    if (!childCreated()) {
        log.debug "appinfo: $settings.name"
        log.debug "Need to create child $settings.name"
        device = addChildDevice("mnestor", "GCal Event Sensor", getDeviceID(), null, [label: "GCal:${settings.name}", completedSetup: true])
        //make sure it's created before we do anything else
        device.save()
    } else {
        log.debug "Need to change child $settings.name"
        device = getChildDevice(getDeviceID())
    }
    
    device.setRefresh(settings.refresh)
    device.label = "GCal:${settings.name}"
    device.save()
    
    device.refresh()
}

def uninstalled() {
    log.debug "do uninstall in gcal trigger"
    
    getChildDevices().each {
    	log.debug "Delete $it.deviceNetworkId"
        try {
            deleteChildDevice(it.deviceNetworkId)
        } catch (Exception e) {
            log.debug "Fatal exception? $e"
        }
    }
}

private childCreated() {
    if (getChildDevice(getDeviceID())) {
        return true
    } else {
        return false
    }
}

private getDeviceID() {
    log.debug "we got called to get the device ID!"
    return "GCal_${app.id}"
}

def eventUpdater(evt) {
    log.trace "eventUpdater()"
}

def getNextEvents() {
    log.debug "getNextEvents() child"
    def search = settings.search
    if (!search) { search = "" }
    log.debug "getNextEvents() watchCalendars: ${watchCalendars}, search: ${search}"
    return parent.getNextEvents(settings.watchCalendars, search)
}
