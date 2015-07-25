private def myVersion() { return "v1.0.2-develop+001-unstable" }
// Version Numbering: vMajor.Minor.VisibleFix[-branch]+BuildNo[-State]. For master, branch=beta or null.
// In non-release branches, version number is pre-incremented (i.e., branch version always > base released version).
/**
 *  Echo Hue Emulator Endpoint
 *
 *  By Terry R. Gauchat
 *  Based upon Ronald Gouldner
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
 * Frequently edited options, parameters, constants.
 */
/**
 * Disable specific level of logging by commenting out log.* expressions as desired.
 * NB: Someday SmartThings's live log viewer front-end should provide dynamic filter-by-level, right?
 */
private def myDebug(text) {
    log.debug myLogFormat(text) // NB: Debug level messages including the PIN number! Keep debug off mostly.
}
private def myTrace(text) {
    log.trace myLogFormat(text) // NB: Trace messages are farely minimal. Still helpful even if debug on.
}
private def myInfo(text) {
    log.info myLogFormat(text)  // NB: No usages in this program. TODO: Should some Trace be Info?
}
private def myLogFormat(text) {
    return "\"${app.label}\".(\"${app.name}\"): ${text}"
}


definition(
    name: "Echo Hue Emulator Endpoint",
    namespace: "CosmicPuppy",
    author: "Terry Gauchat",
    description: "This is an Endpoint authorization to Amazon Echo via Hue Emulator",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true
) /* definition */


preferences {
        section("Allow Endpoint to Control These Things...") {
            input "switches", "capability.switch", title: "Which Switches?", multiple: true
        }
}


mappings {

    path("/switches") {
        action: [
                GET: "listSwitches"
        ]
    }
    path("/switches/:id") {
        action: [
                GET: "showSwitch"
        ]
    }
    path("/switches/:id/:command") {
        action: [
                GET: "updateSwitch"
        ]
    }

} /* mappings */

def installed() {}

def updated() {}


/**
 * Switches
 */
def listSwitches() {
    state.phrases = location.helloHome?.getPhrases()*.label
    def list = []
    myDebug("listSwitches")
    list = list + switches.collect{device(it,"switch")}
    myDebug("List so far: ${list}")
    list = list + location.modes.collect{device(it,"mode")}
    myDebug("List so far: ${list}")
    list = list + state.phrases.collect{device(it,"phrase")}

    myInfo("Combined: " + list )
    list
}

/**
 * TODO: Does Echo call Show? Why?
 * For now, this will fail on Phrases.
 */
def showSwitch() {
    myDebug("showSwitch")
    show(switches, "switch")
}

/**
 * TODO: Phrase manipulation should be a subroutine that looks up original label from a state Map.
 */
void updateSwitch() {
    myTrace("updateSwitch Endpoint: request: params: ${params}")
    def String id = params.id

    def String type = id.substring( 0, 7 )
    myTrace("ID: ${id}, TYPE: ${type}.")
    switch ( type ) {
    	case "Mode___":
            def String calledMode = (id.substring( 7 )).replaceAll("-"," ")
            myInfo("Found a Mode! ${calledMode}")
        	executeMode(calledMode)
            break

    	case "Phrase_":
            def String calledPhrase = (id.substring( 7 )).replaceAll("-"," ")
            myInfo("Found a Phrase! ${calledPhrase}")
        	executePhrase(calledPhrase)
            break

        default:
            update(switches)
    }
}


def deviceHandler(evt) {}


/**
 * Handle Phrases
 */
private void executePhrase(myPhrase) {
    /* TODO: Check to make sure Phrase is in Settings.phrases first. May have to expand it. */
	if (myPhrase != null)	{
        myTrace("helloHome.execute: \"${myPhrase}\"")
        location.helloHome.execute(myPhrase)
    }
}


/**
 * Handle Modes
 */
private void executeMode(myMode) {
    /* TODO: Check to make sure Mode is in valid and not already active first. May have to expand it. */
	if (myMode != null)	{
        myTrace("setLocationMode: \"${myMode}\"")
        setLocationMode(myMode)
    }
}


private void update(devices) {
    myDebug("update, request: params: ${params}, devices: $devices.id")

    //def command = request.JSON?.command
    def command = params.command
    if (command)
    {
        def device = devices.find { it.id == params.id }
        if (!device) {
            httpError(404, "Device not found")
        } else {
            if(command == "toggle")
            {
                if(device.currentValue('switch') == "on")
                    device.off();
                else
                    device.on();
            }
            else
            {
                device."$command"()
            }
        }
    }
} /* update(devices) */


private show(devices, type) {
    myDebug("Echo Endpoint Show Called")
    def device = devices.find { it.id == params.id }
    if (!device) {
        httpError(404, "Device not found")
    }
    else {
        def attributeName = type == "motionSensor" ? "motion" : type
        def s = device.currentState(attributeName)
        [id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
    }
} /* show() */


/**
 * TODO: Phrase manipulation should be a subroutine that sets up original label from a state Map.
 */
private device(it, type) {
    myDebug("Device( Type: ${type} Value: ${it} )")
    switch ( type ) {
    	case "switch":
            it ? [id: it.id, label: it.label, type: type] : null
            break
        case "mode":
            def String modeName = it
            it ? [id: "Mode___" + modeName.replaceAll(" ","-"), label: "Mode " + modeName, type: type] : null
            break
        case "phrase":
            it ? [id: "Phrase_" + it.replaceAll(" ","-"), label: "Activity " + it, type: type] : null
            break
     }
} /* device() */


/* =========== */
/* End of File */
/* =========== */