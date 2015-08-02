metadata {
	definition (name: "Screen Shield on/off", namespace: "CosmicPuppy", author: "Terry Gauchat") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"

        command "enable"
        command "disable"
        attribute "override", "enum", ["stopping", "stopped", "enabled", "disabled"]

        command "screenOn"
        command "screenOff"
        attribute "screen", "enum", ["on", "off"]

        command "projecOn"
        command "projecOff"
        attribute "projectorSignal", "enum", ["on", "off"]
	}

	// Simulator metadata
	simulator {
		status "on":  "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		status "off": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
        status "stop": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A73746F70"
        status "projON": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A70726F6A4F4E"
        status "projOFF": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A70726F6A4F4646"

		reply "raw 0x0 { 00 00 0a 0a 6f 6e }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		reply "raw 0x0 { 00 00 0a 0a 6f 66 66 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"

        /* TODO: Fix these replies! */
        reply "raw 0x0 { 00 00 0a 0a 73 74 6f 70 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A73746F70"
        reply "raw 0x0 { 00 00 0a 0a 73 74 6f 70 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A70726F6A4F4E"
        reply "raw 0x0 { 00 00 0a 0a 73 74 6f 70 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A70726F6A4F4646"
	}

	tiles {
		standardTile("screenTile", "device.screen", width: 2, height: 2, canChangeIcon: true, canChangeBackground: false) {
			state("on",  label:'down (${name})', action:"screenOff", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", nextState:"raising")
			state("off", label:'up (${name})', action:"screenOn",  icon:"st.doors.garage.garage-open",   backgroundColor:"#0000ff", nextState:"lowering")
            state("lowering", label:'${name}', action: "screenOff", icon:"st.doors.garage.garage-closing", backgroundColor:"#7fff00")
			state("raising",  label:'${name}', action: "screenOn" , icon:"st.doors.garage.garage-opening", backgroundColor:"#37fdfc")
            state("stopped",  label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#888888")
            state("enabled",  label:'${name}', icon:"st.doors.garage.garage-closed",  backgroundColor:"#555555")
        }

        /* TODO: Perhaps make this tile stop AND disable the on/off/up/down buttons? */
		standardTile("stopTile", "device.override", width: 1, height: 1, canChangeIcon: false, canChangeBackground: true, decoration: flat) {
            state("enabled",  label:'STOP', action:"disable", icon:"st.sonos.stop-btn", backgroundColor:"#cccccc", nextState:"stopping", defaultState:true)
			state("disabled", label:'STOP', action:"enable",  icon:"st.sonos.play-btn", backgroundColor:"#bbbbbb")
            state("stopping",  label:'stopping', icon:"st.sonos.pause-btn", backgroundColor: "#cccccc")
        }

		standardTile("switchTile", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: false) {
			state("on",  label:'${name}', action:"switch.off", icon:"st.Entertainment.entertainment9", backgroundColor:"#79b821", nextState:"powering off")
			state("off", label:'${name}', action:"switch.on",  icon:"st.Entertainment.entertainment9",   backgroundColor:"#0000ff", nextState:"powering on")
            state("powering on", label:'${name}', action:"switch.off", icon:"st.Entertainment.entertainment9", backgroundColor:"#7fff00")
			state("powering off",label:'${name}', action:"switch.on", icon:"st.Entertainment.entertainment9", backgroundColor:"#37fdfc")
        }

		valueTile("blankValueTile", "device.projectorSignal", width: 1, height: 1, canChangeBackground: true, decoration: flat) {
		}

		standardTile("projectorStatus", "device.projectorSignal", width: 1, height: 1, canChangeBackground: true) {
    		state "on",  label:'Projector is: ${currentValue}', icon:"st.Entertainment.entertainment9", backgroundColor:"#79b821"
            state "off", label:'Projector is: ${currentValue}', icon:"st.Entertainment.entertainment9", backgroundColor:"#696969"
		}

        main (["switchTile","screenTile"])
		details (["switchTile","projectorStatus","blankValueTile","screenTile","stopTile","blankValueTile"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
    def evt_onoff = []
	def value = zigbee.parse(description)?.text
	def name = value in ["screenOn","screenOff"] ? "screen" : value in ["projecOn","projecOff"] ? "projectorSignal" : null
    value = value in [ "screenOn" ] ? "on" : value
    value = value in [ "screenOff" ] ? "off" : value
	value = value in [ "projecOn" ] ? "on" : value
    value = value in [ "projecOff" ] ? "off" : value

    if ( name != null ) log.debug "Parse argument description: ${description}"

    log.trace "Parse returned ${evt_onoff?.descriptionText}, name: ${name} value: ${value}."

    if ( name != null ) {
        log.debug "Create Event Name: ${name}, Value: ${value}."
        evt_onoff = createEvent(name: name, value: value, displayed: true, isStateChange: true)

        switch (value) {
			case "disable":
            	evt_onoff = createEvent(name: "screen", value: "stopped", displayed: true, isStateChange: true)
                break;
		}
    }

	return [ evt_onoff ]
}



/**
 * COMMANDS
 */
def on() {
	/* TODO: Change this to Projector, probably. */
	projecOn();
}

def off() {
	/* TODO: Change this to Projector, probably. */
	projecOff();
}

def screenOn() {
	zigbee.smartShield(text: "screenOn").format()
}

def screenOff() {
    zigbee.smartShield(text: "screenOff").format()
}

def projecOn() {
	zigbee.smartShield(text: "projecOn").format()
}

def projecOff() {
    zigbee.smartShield(text: "projecOff").format()
}


def disable() {
 	sendEvent(name: "override", value: "disabled", displayed: true, isStateChange: true)
    zigbee.smartShield(text: "disable").format()
}

def enable() {
 	sendEvent(name: "override", value: "enabled", displayed: true, isStateChange: true)
    /* TODO: We should save the last state (down or up) so we can enable the correct direction. But for now, make it always assume screen was up. */
    sendEvent(name: "switch", value: "off", displayed: true, isStateChange: true)
    zigbee.smartShield(text: "enable").format()
}


/* =========== */
/* End of File */
/* =========== */