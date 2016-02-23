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
 * 20160223.4 - Fix for duplicating sensors, not having a clostTime at time of open when event is in progress
 * 20160223.2 - Don't make a quick change and forget to test
 * 20160223.1 - Error checking - Force check for Device Handler so we can let the user have a more informative error
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
    
    //force a check to make sure the device handler is available for use
    try {
    	def device = addChildDevice(getNamespace(), getDeviceHandler(), getDeviceID(), null, [label: "GCal:${settings.name}", completedSetup: true])
        deleteChildDevice(device.deviceNetworkId)
    } catch (e) {
    	return dynamicPage(name: "selectCalendars", title: "Missing Device", install: true, uninstall: false) {
        	section ("Error") {
            	paragraph "We can't seem to create a child device, did you install the GCal Event Sensor device handler?"
            }
        }
    }
    
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

def installed() {}

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
        device = addChildDevice(getNamespace(), getDeviceHandler(), getDeviceID(), null, [label: "GCal:${settings.name}", completedSetup: true])
    } else {
        device = getChildDevice(getDeviceID())
    }
    
    device.setRefresh(settings.refresh)
    device.label = "GCal:${settings.name}"
    device.save()
    
    device.refresh()
}

def uninstalled() {
    log.debug "do uninstall in gcal trigger"
	deleteAllChildren()
}

def deleteAllChildren() {
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
    return getChildDevice(getDeviceID())
}

private getDeviceID() {
    return "GCal_${app.id}"
}

def getNextEvents() {
    log.debug "getNextEvents() child"
    def search = (!settings.search) ? "" : settings.search
    return parent.getNextEvents(settings.watchCalendars, search)
}

private getDeviceHandler() { return "GCal Event Sensor" }
private getNamespace() { return "mnestor" }