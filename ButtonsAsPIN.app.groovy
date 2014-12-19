/**
 *  Use Buttons As PIN Input
 *
 *  Assign a multi-button controller (e.g., Aeon Labs Minimote) to be a security 'PIN code' input pad,
 *    which triggers a switch, lock, mode, or Hello Home action.
 *  More details on GitHub: <https://github.com/CosmicPuppy/SmartThings-ButtonsAsPIN>
 *    and on SmartThings Community Forum: <http://community.smartthings.com/t/use-buttons-as-pin-input-e-g-aeon-minimote-button-sequence-as-trigger-to-hello-home-action-lock-unlock-arm-disarm-mode-etc/8378?u=tgauchat>
 *
 *  Filename: ButtonsAsPIN.app.groovy
 *  Version: see myVersion()
 *  Date: 2014-12-21
 *  Status:
 *    - First Beta release to Community for testing, feedback, feature requests.
 *    - Currently hard limited to 3-9 digits from a choice of 1-4.
 *    - Tested only with 4-button Aeon Labs Minimote, button-push only, no support for button-hold.
 *
 *  Summary Changelog (See github for full Release Notes):
 *    - tbd.
 *
 *  Author: Terry Gauchat (CosmicPuppy)
 *  Email: terry@cosmicpuppy.com
 *  SmartThings Community: @tgauchat -- <http://community.smartthings.com/users/tgauchat>
 *  Latest versions on GitHub at: <https://github.com/CosmicPuppy/SmartThings-ButtonsAsPIN>
 *
 *  There is no charge for this software:
 *  Optional "encouragement funding" is accepted to PayPal address: info@CosmicPuppy.com
 *  (Contributions sure help cover endless vet bills for Buddy & Deuce, the official CosmicPuppy beagles.)
 *
 *  ----------------------------------------------------------------------------------------------------------------
 *  Copyright 2014 Terry Gauchat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */


import groovy.json.JsonSlurper

/**
 * Frequently edited options, parameters, constants.
 */
private def myVersion() { return "v0.1.0-beta+004" }
/**
 * Disable specific level of logging by commenting out log.* expressions as desired.
 * NB: Someday SmartThings's live log viewer front-end should provide dynamic filter-by-level, right?
 */
private def myDebug(text) {
    // log.debug myLogFormat(text)
}
private def myTrace(text) {
    log.trace myLogFormat(text)
}
private def myInfo(text) {
    log.info myLogFormat(text)
}
private def myLogFormat(text) {
    return "\"${app.label}\".(\"${app.name}\"): ${text}"
}


/**
 * Definiton
 */
definition(
    name: "Use Buttons As PIN Input",
    namespace: "CosmicPuppy",
    author: "Terry Gauchat",
    description: "Assign a multi-button controller (e.g., Aeon Labs Minimote) to be a security 'PIN code' input pad, " +
        "which triggers a switch, lock, mode, or Hello Home action.",
    category: "Safety & Security",
    iconUrl:     "http://cosmicpuppy.com/SmartThingsAssets/ButtonsAsPIN_icon_ComboLock.jpg",
    iconX2Url:   "http://cosmicpuppy.com/SmartThingsAssets/ButtonsAsPIN_icon_ComboLock.jpg",
    iconX3Url:   "http://cosmicpuppy.com/SmartThingsAssets/ButtonsAsPIN_icon_ComboLock.jpg",
) /* definition */


preferences {
    page(name: "pageSelectButtonDev")
	page(name: "pageSetPinSequence")
    page(name: "pageSelectActions")
} /* preferences */


def pageSelectButtonDev() {
	dynamicPage(name: "pageSelectButtonDev", title: "Select your button device & PIN length...",
    	    nextPage: "pageSetPinSequence", uninstall: true) {
	    section {
            paragraph "App Info: Version ${myVersion()}\nhttps://github.com/CosmicPuppy/SmartThings-ButtonsAsPIN"
	    }
        section([mobileOnly:true]) {
			label title: "Assign a name to this SmartApp instance?", required: false
			mode title: "Activate for specific mode(s)?", required: false
		}
		section {
			input name: "buttonDevice", type: "capability.button", title: "Button Device:", multiple: false, required: true
		}
		section {
			input name: "pinLength", type: "enum", title: "Desired PIN length (3 to 9 digits):", multiple: false,
            	required: true, options:["3","4","5","6","7","8","9"], defaultValue: "4";
		}
	}
} /* pageSelectButtonDev() */


def pageSetPinSequence() {
	dynamicPage(name: "pageSetPinSequence", title: "Set PIN (security code sequence)...", nextPage: "pageSelectActions",
    	install: false, uninstall: true) {
        section("PIN Code Buttons in Desired Sequence Order") {
        	L:{ for( i in  1 .. pinLength.toInteger() ) {
               		input name: "comb_${i}", type: "enum", title: "Sequence $i:", mulitple: false, required: true,
                    	options: ["1","2","3","4"];
                }
            }
            href "pageSelectButtonDev", title:"Go Back", description:"Tap to go back."
    	}
    }
} /* pageSetPinSequence() */


def pageSelectActions() {
    def valid = true
    def pageProperties = [
        name: "pageSelectActions",
        title: "Confirm PIN & Select Action(s)...",
        install: true,
        uninstall: true
    ]

    /**
     * TODO: This should be dynamic length loop, but I need to figure out how to dynamically String substitute comb_*,
     *       -- Is that even possible!? Maybe some form of Eval() would work?
     */
    state.pinSeqList = []
    state.pinLength = pinLength.toInteger()
    switch ( state.pinLength ) {
    	case 9:
            state.pinSeqList << comb_9
    	case 8..9:
            state.pinSeqList << comb_8
    	case 7..9:
            state.pinSeqList << comb_7
    	case 6..9:
            state.pinSeqList << comb_6
    	case 5..9:
            state.pinSeqList << comb_5
    	case 4..9:
            state.pinSeqList << comb_4
    	case 3..9:
            state.pinSeqList << comb_3
    	case 2..9:
            state.pinSeqList << comb_2
    	case 1..9:
            state.pinSeqList << comb_1
    }
    state.pinSeqList.reverse(true) // true --> mutate original list instead of a copy.
    myDebug("pinSeqList is $state.pinSeqList")
    myDebug("pinLength is $state.pinLength")

    return dynamicPage(pageProperties) {
        section() {
            paragraph "PIN Code set to: " + "$state.pinSeqList"
            href "pageSetPinSequence", title:"Go Back", description:"Tap to change PIN Code sequence."
        }
        section("Devices, Modes, Actions") {
            input "switches", "capability.switch", title: "Toggle Lights & Switches", multiple: true, required: false
            input "locks", "capability.lock", title: "Toggle Locks", multiple: true, required: false
            input "mode", "mode", title: "Set Mode", required: false
            def phrases = location.helloHome?.getPhrases()*.label
            if (phrases) {
                myDebug("Phrase list found: ${phrases}")
                /* NB: Customary to not allow multiple phrases. Complications due to sequencing, etc. */
                input "phrase", "enum", title: "Trigger Hello Home Action", required: false, options: phrases
            }
        }
    }
} /* pageSelectActions() */


def installed() {
    myTrace("Installed; Version: ${myVersion()}")
	myDebug("Installed; settings: ${settings}") // settings includes the PIN, so we should avoid logging except for Debug.

	initialize()
}

def updated() {
    myTrace("Updated; Version: ${myVersion()}")
	myDebug("Updated; settings: ${settings}") // settings includes the PIN, so we should avoid logging except for Debug.

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(buttonDevice, "button", buttonEvent)
    state.inputDigitsList = []

	myDebug("Initialized - state: ${state}")
}


/**
 * Watch for correct matching PIN input by rolling scan of last "pinLength" presses.
 *
 * TODO: Keep a count of the number of unsucessful sequences so that alarm or other alert action could be called.
 *       Such an alert could also (virtually) "disable" the buttons for a period of time.
 *
 * NB: It would be more secure to require a Start and/or End indicator, but complicates user interface.
 *     One possible improvement is to have a "timeout" on the input buffer.
 *
 * NB: On the Aeon Minimote, pressing the same key twice is "sort of" filtered unless
 *       you wait for the red LED confirmation response.
 *     The two presses are probably detectable by analyzing the buttonDevice.events log, but the stream seems inconsistent.
 *     Therefore the User "MUST" wait for confirmation after each keypress else input digits may be lost (undetected).
 *     NOT waiting will often still work, though, if there are no double presses (duplicate sequential digits in the PIN).
 */
def buttonEvent(evt){
	def allOK = true;
	if(true) {
		def value = evt.value
        def slurper = new JsonSlurper()
        def dataMap = slurper.parseText(evt.data)
        def buttonNumber = dataMap.buttonNumber
		myDebug("buttonEvent Device: [$buttonDevice.name], Name: [$evt.name], Value: [$evt.value], Data: [$evt.data], ButtonNumber: [$dataMap.buttonNumber]")
        if(value == "pushed") {
        	state.inputDigitsList << buttonNumber.toString()
            if(state.inputDigitsList.size > state.pinLength) {
            	state.inputDigitsList.remove(state.inputDigitsList.size - state.pinLength - 1)
            }
            myDebug("Current inputDigitsList: $state.inputDigitsList")
            if(state.inputDigitsList.equals(state.pinSeqList)) {
            	myDebug("PIN Match Detected; found [$state.pinSeqList]. Clearing input digits buffer.")
            	myTrace("PIN Match Detected. Clearing input digits buffer.")
            	state.inputDigitsList.clear();
                executeHandlers()
            } else {
            	myDebug("No PIN match yet: inputDigitsList is $inputDigitsList; looking for $state.pinSeqList")
            	myTrace("No PIN match yet.")
            }
        }

    /**
     * TODO: (Experimental code for reference):
     *   If the above code misses button presses that occur too quickly,
     *   considering scanning back through the event log.
     * The behavior if this is a little confusing though: Repeated keys show up in the recentEvents().
     * Could we limit data entry to 10 or 20 seconds and limit the backscan to the length of the PIN?
     * The only time multiple event backscan seems to apply is for multi-presses of the same key. But then this is essential.
     * Yet eventsSince seems to only be reporting NEW events. Weird. Not critical for this App to work ok, though.
     */
	//	def recentEvents = buttonDevice.eventsSince(new Date(now() - 10000),
    //    	[max:pinLength.toInteger()]).findAll{it.value == evt.value && it.data == evt.data}
	//	myDebug("PIN Found ${recentEvents.size()?:0} events in past 10 seconds"
    //  recentEvents.eachWithIndex { it, i -> myDebug("PIN [$i] Value:$it.value Data:$it.data" }
	}
} /* buttonEvent() */


/**
 * Event handlers.
 * Most code copied from "Button Controller" by SmartThings, + slight modifications.
 */
def executeHandlers() {
	myTrace("executeHandlers; switches/locks toggles, mode set, phrase execute.")

	def switches = findPreferenceSetting('switches')
    myDebug("switches are ${switches}")
	if (switches != null) toggle(switches,'switch')

	def locks = findPreferenceSetting('locks')
    myDebug("locks are ${locks}")
	if (locks != null) toggle(locks,'lock')

	def mode = findPreferenceSetting('mode')
	if (mode != null) changeMode(mode)

	def phrase = findPreferenceSetting('phrase')
    if (phrase != null)	{
        myTrace("helloHome.execute: \"${phrase}\"")
        location.helloHome.execute(phrase)
    }
} /* executeHandlers() */


def findPreferenceSetting(preferenceName) {
	def pref = settings[preferenceName]
	if(pref != null) {
		myDebug("Found Pref Setting: $pref for $preferenceName")
	}
	return pref
}


/**
 * NB: This function only works properly if "devices" list passed are all of same capability.
 *     Shouldn't be a problem, because devices is from a preference setting list filtered by capability.
 * NB: "Toggle" is a misnomer as it sets all switches to off ANY are on (rather than check and toggle each one's state).
 *       (Similarly, all locks are unlocked if any are found locked.)
 *     Is toggling the most intuitive behavior since the resulting set of states is possibly uncertain to the user?
 *     A possible "improvement"?: Require two different PINs, for lock vs for unlock (could just be the last digit: 1 vs 2).
 *       This would also better accomodate two distinct Hello Home phrases (activating security vs deactiviting security).
 *       But: A toggle type action for Hello Home phrase is appropriate if reading and using mode or state is reliable.
 *     The current "Failsafe default" sections are a questionable design decision; Is there a better choice?
 */
def toggle(devices,capabilityType) {
    if (capabilityType == 'switch') {
    	myDebug("toggle switch Values: $devices = ${devices*.currentValue('switch')}")
    	if (devices*.currentValue('switch').contains('on')) {
    	    myTrace("Set devices.off: ${devices}")
    		devices.off()
    	}
    	else if (devices*.currentValue('switch').contains('off')) {
    	    myTrace("Set devices.on: ${devices}")
    		devices.on()
    	}
    	else {
    	    myTrace("Set devices.on (Failsafe default action attempt.): ${devices}")
    		devices.on()
    	}
    }
	else if (capabilityType == 'lock') {
    	myDebug("toggle lock Values: $devices = ${devices*.currentValue('lock')}")
    	if (devices*.currentValue('lock').contains('locked')) {
    	    myTrace("Set devices.unlock: ${devices}")
    		devices.unlock()
    	}
    	else if (devices*.currentValue('lock').contains('unlocked')) {
    	    myTrace("Set devices.lock: ${devices}")
    		devices.lock()
    	}
    	else {
    	    myTrace("Set devices.unlock (Failsafe default action attempt.): ${devices}")
    		devices.unlock()
    	}
	}
} /* toggle() */


def changeMode(mode) {
	myDebug("changeMode: $mode, location.mode = $location.mode, location.modes = $location.modes")

	if (location.mode != mode && location.modes?.find { it.name == mode }) {
	    myTrace("setLocationMode: ${mode}")
		setLocationMode(mode)
	}
}


/* =========== */
/* End of File */
/* =========== */