private def myVersion() { return "v1.0.1-develop+002-unstable" }
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
    page( name: "pageSwitches" )
    page( name: "pagePhrases" )
}

def pageSwitches() {
    myTrace("Version: ${myVersion()}. Running preferences pages.")
    def pageProperties = [
        name: "pageSwitches",
        title: "Select Switches & Dimmers",
        nextPage: "pagePhrases",
        install: true,
        uninstall: true
    ]
    return dynamicPage(pageProperties) {
        section(title: "About This App") {
            paragraph "Version ${myVersion()}"
            href title: "GitHub Link",
                 style: "external",
                 url: "https://github.com/CosmicPuppy/SmartThings-ButtonsAsPIN",
                 description: "https://github.com/CosmicPuppy/SmartThings-ButtonsAsPIN"
                 required: false
        }
        section("Allow Endpoint to Control These Things...") {
            input "switches", "capability.switch", title: "Which Switches?", multiple: true
        }
    }
}

def pagePhrases() {
    def pageProperties = [
        name: "pagePhrases",
        title: "Select Hello Home Phrases",
        install: true,
        uninstall: true
    ]

    return dynamicPage(pageProperties) {
        section("Phrases / Actions, Modes") {
            def phrases = location.helloHome?.getPhrases()*.label
            myDebug("Possible phrase list found: ${phrases}")
            if (phrases) {
                myDebug("Phrase list found: ${phrases}")
                /* NB: Customary to not allow multiple phrases. Complications due to sequencing, etc. */
                input "phrase", "enum", title: "Trigger Hello Home Action", required: false, options: phrases
            }
            input "mode", "mode", title: "Possible modes", required: false
        }
    }
} /* pageSelectPhrases() */


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
    path("/locks") {
        action: [
                GET: "listLocks"
        ]
    }
    path("/locks/:id") {
        action: [
                GET: "showLock"
        ]
    }
    path("/locks/:id/:command") {
        action: [
                GET: "updateLock"
        ]
    }

}

def installed() {}

def updated() {}


//switches
def listSwitches() {
    log.debug "Listing Switches"
    log.debug switches.collect{device(it,"switch")}
    switches.collect{device(it,"switch")}
}

def showSwitch() {
    show(switches, "switch")
    log.debug "Show Switch"
}
void updateSwitch() {
    update(switches)
    log.debug "update Switch"
}

//locks
def listLocks() {
    locks.collect{device(it,"lock")}
}

def showLock() {
    show(locks, "lock")
}

void updateLock() {
    update(locks)
}



def deviceHandler(evt) {}

private void update(devices) {
    log.debug "update, request: params: ${params}, devices: $devices.id"


    //def command = request.JSON?.command
    def command = params.command
    //let's create a toggle option here
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
}

private show(devices, type) {
    log.debug "Echo Endpoint Show Called"
    def device = devices.find { it.id == params.id }
    if (!device) {
        httpError(404, "Device not found")
    }
    else {
        def attributeName = type == "motionSensor" ? "motion" : type
        def s = device.currentState(attributeName)
        [id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
    }
}


private device(it, type) {
    it ? [id: it.id, label: it.label, type: type] : null
}


/* =========== */
/* End of File */
/* =========== */