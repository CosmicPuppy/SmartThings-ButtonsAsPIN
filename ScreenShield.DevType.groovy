metadata {
	// Automatically generated. Make future change here.
	definition (name: "Screen Shield on/off", namespace: "CosmicPuppy", author: "Terry Gauchat") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"
        
        attribute "position", "enum", ["raising", "lowering", "up", "down" ]
        
//        command "stop"
//        attribute "action", "enum", ["stopped", "stopping", "up", "down"]
	}

	// Simulator metadata
	simulator {
		status "on":  "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		status "off": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
        status "stop": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A73746F70"

		reply "raw 0x0 { 00 00 0a 0a 6f 6e }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		reply "raw 0x0 { 00 00 0a 0a 6f 66 66 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
        reply "raw 0x0 { 00 00 0a 0a 73 74 6f 70 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A73746F70"
	}

	// UI tile definitions
	tiles {
		standardTile("switchTile", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: false) {
			state "on",  label: '${name}', action: "switch.off", icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.doors.garage.garage-open", backgroundColor: "#0000ff"
		}
   		standardTile("positionTile", "device.position", width: 1, height: 1) {
			state("down", label:'${name} v03', action:"switch.off", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", nextState:"raising")
			state("up", label:'${name} v03', action:"switch.on", icon:"st.doors.garage.garage-open", backgroundColor:"#0000ff", nextState:"lowering", defaultState: true)
			state("lowering", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffe71e", nextState: "down")
			state("raising", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffe71e", nextState: "up")
		}
//		standardTile("stopTile", "device.action", width: 1, height: 1, canChangeIcon: true, canChangeBackground: true) {
//			state "stopping",  label: '${name}', action: "stop", icon: "st.Weather.weather3", backgroundColor: "#aaaaaa", nextState: "stopped"
//			state "stopped",  label: '${name}', action: "stop", icon: "st.Weather.weather2", backgroundColor: "#bbbbbb"
//        }

        main (["switchTile","positionTile"])
		details ( ["switchTile","positionTile"] )
//        details ( ["positionTile","stopTile"] )
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
    def evt_onoff = []
    def evt_position = []
	def value = zigbee.parse(description)?.text
	def name = value in ["on","off"] ? "switch" : null

//    log.trace "Parse returned ${evt_onoff?.descriptionText}, name: ${name} value: ${value}."
//	  log.debug "Parse argument description: ${description}"

    if ( name != null ) {
        evt_onoff = createEvent(name: name, value: value)
        //sendEvent( evt_onoff )
//        log.trace "Parse returned ${evt_onoff?.descriptionText}, name: ${name} value: ${value}."
        
        switch (value) {
        	case "on":
            	evt_position = createEvent(name: "position", value: "down", displayed: true, isStateChange: true)
                break;

        	case "off":
            	evt_position = createEvent(name: "position", value: "up", displayed: true, isStateChange: true)
                break;
		}
    }    
    
	return [ evt_onoff, evt_position ]
}

// Commands sent to the device
def on() {
   	sendEvent(name: "position", value: "lowering", displayed: true, isStateChange: true)
	zigbee.smartShield(text: "on").format()
}

def off() {
   	sendEvent(name: "position", value: "raising", displayed: true, isStateChange: true)
    zigbee.smartShield(text: "off").format()
}

def stop() {
	zigbee.smartShield(text: "stop").format()
}

/* =========== */
/* End of File */
/* =========== */