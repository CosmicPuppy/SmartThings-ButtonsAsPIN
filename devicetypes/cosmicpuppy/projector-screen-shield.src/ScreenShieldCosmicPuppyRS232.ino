/**
 * Notes:
 *   - Screen can get out of sync with GUI because of assumed "Next State".
 *        - Projector has a verification (the trigger signal), so it is better.
 *        - But with no verification, might as well assume the screen is in the last requested spot.
 */

#define MYVERSION "v1.0.2-beta+002"
/*****************************************************************************
 * @file
 * @brief
 * 2015/8/02 - A few modifications by CosmicPuppy to send responses for every command (on, off).
 */
#include <RCSwitch.h>
#include <SoftwareSerial.h>   //TODO need to set due to some weird wire language linker, should we absorb this whole library into smartthings
#include <SmartThings.h>

#define PIN_THING_RX    3
#define PIN_THING_TX    2

SmartThingsCallout_t messageCallout;    // call out function forward decalaration
SmartThings smartthing(PIN_THING_RX, PIN_THING_TX, messageCallout, "", false);  // constructor

/*
 * NB: Can't use SoftwareSerial reliably because it conflicts with ThingShield running at 2400 BAUD.
 *     Perhaps we could get everything running at 2400 BAUD or 9600 BAUD, but I think speed of Shield and Projector aren't configurable.
 */
//SoftwareSerial projecSerial(4, 5); // RX, TX
#define projecSerial Serial

int ledPin = 13;
bool isDebugEnabled = true;    // enable or disable debug in this example

#define down  12
#define up    11
#define pause 10
#define RCPIN 10

int holdDuration = 200; // duration of the relay on "press"
int waitDuration = 100; // duration of the relay between "presses".

int inputPin = 7; // 12vDC mono trigger signal input from Projector
bool projectorOn = false;
// bool projectorUpdate = false; /* Let status of Projector be unsent until we see it change? */
int inputPinSum = 0; // We will sum the value of input pin over several samples to make sure it is really ON or really OFF.
int loopCount = 0;

/* TODO: These could be made on/off configuration options from the SmartThings Device Tiles! */
/*       or change them to #define */
bool screenAutoOn  = true;  // Localized automatic screen down/on when Projector ON  if true.
bool screenAutoOff = false; // Localized automatic screen up/off  when Projector OFF if true.

bool screenIsOn = false; // Attempt to avoid extra Screen On attempts in SOME automatic calls.

RCSwitch myRCSwitch = RCSwitch(); /* Remote Control RF Radio */


/**
 * setup
 */
void setup()
{
  /* Remote Control RF Radio Setup */
  myRCSwitch.enableTransmit(RCPIN);
  
  /* Input trigger from Projector */
  analogReference(DEFAULT);
  pinMode(inputPin, INPUT);
  digitalWrite(inputPin, LOW); // Must be LOW else always get 1. Is this Pulldown enabled?
  
  /* Set mode for Hardware Pins */
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, HIGH);

  pinMode(down, OUTPUT);
  digitalWrite(down, LOW);

  pinMode(up, OUTPUT);
  digitalWrite(up, LOW);

  // projecSerial.begin(9600);  // This line breaks everything. Must conflict with ThingShield Software Serial.

  updateProjector();

  if (isDebugEnabled)
  { // setup debug serial port
    Serial.begin(9600);
    Serial.print("Version"); Serial.println(MYVERSION);
    Serial.println("Setup.");
  }
} /* setup() */


/**
 * loop
 */
void loop()
{
  
  /* We will allow a few "missed" signal values 5 out of 50 and still count as ON. */
  inputPinSum += digitalRead(inputPin);
  if ( loopCount <= 50 && inputPinSum > 45 ) {
    // Serial.println( "Signal still ON." );
    if ( !projectorOn ) {
      projectorOn = true;
      // projectorUpdate = true;
      updateProjector();
      if ( screenAutoOn && !screenIsOn ) screenOn();
      screenIsOn = false; // Reset local Screen Status to be sure it will turn on next *automated* attempt.
    }
    loopCount = 0;
    inputPinSum = 0;
  } else if ( loopCount >= 50 && inputPinSum < 5 ) {
    // Serial.println( "Signal is OFF." );
    if ( projectorOn ) {
      projectorOn = false;
      //projectorUpdate = true;
      updateProjector();
      if ( screenAutoOff && screenOff ) screenOff();
    }
    loopCount = 0;
    inputPinSum = 0;
  }
  if ( loopCount > 50 ) { loopCount = 0; inputPinSum = 0; }
  loopCount ++;

  smartthing.run();
} /* loop() */


/**
 * doublePress
 */
void doublePress( int whichButton )
{
  singlePress( whichButton );
  singlePress( whichButton );
}


/**
 * singlePress
 */
void singlePress( int whichButton )
{
//  digitalWrite(whichButton, HIGH);
//  delay(holdDuration);
//  digitalWrite(whichButton, LOW);
//  delay(waitDuration);
  switch (whichButton) {
    case up:
      myRCSwitch.send(4750754, 24);
      break;
    case down:
      myRCSwitch.send(4750756, 24);
      break;  
    case pause:
      myRCSwitch.send(4750760, 24);
      break;  
  }
  delay(waitDuration);
}


/**
 * screenOn() - Screen Down
 */
void screenOn()
{
  smartthing.shieldSetLED(0, 1, 0); // green

  digitalWrite( up, LOW);  // Be certain to lift the other button.
  doublePress( down );

  smartthing.send("screenOn");
  Serial.println("Sent: screenOn");
  // Serial.println( smartthing.shieldGetLastNetworkState() ); // Extra debugging if necessary.
}


/**
 * screenOff() - Screen Up
 */
void screenOff()
{
  smartthing.shieldSetLED(0, 0, 1); // blue

  digitalWrite( down, LOW);  // Be certain to lift the other button.  
  doublePress( up );

  smartthing.send("screenOff");
  Serial.println("Sent: screenOff");
  // Serial.println( smartthing.shieldGetLastNetworkState() ); // Extra debugging if necessary.
}



/**
 * projecSendMsg() - Send message to Projector.
 * Currently hard coded for multiple tries since this doesn't hurt.
 */
void projecSendMsg( char* message )
{
  byte inByte;
  
  Serial.println( "Attempting message: " ); Serial.println( message );
  /* Empty the input buffer (mostly ERR messages from the Projector, probably). */
  while( projecSerial.available() ) { inByte = Serial.read(); };
  delay(300);
  projecSerial.print( message ); projecSerial.print( "\r" );
  projecSerial.println( "" );
  /* Do it a second time for good measure. */
  delay(300);
  while( projecSerial.available() ) { inByte = Serial.read(); };
  projecSerial.println( message );
  delay(300);
  while( projecSerial.available() ) { inByte = Serial.read(); };
  delay(300);
  projecSerial.print( message ); projecSerial.print( "\r" );
  projecSerial.println( "" );
  while( projecSerial.available() ) { inByte = Serial.read(); };
  /* TODO: We could confirm that no ":ERR" response received. */
}

/**
 * projecOn() - Projector On
 */
void projecOn()
{
  int inByte = 0;
  smartthing.shieldSetLED(0, 2, 1); // green

  /* Watching for Projector reponses may or may not be useful. I guess useful in case remote control is used in a separate function. */
  /* TODO: We could first read "PWR?" to get result ":PWR=00" or ":PWR=01 or 02" or ":ERR" */

  projecSendMsg( "PWR ON" );

  /* We do this here to let the screen start movement ASAP. */
  if ( screenAutoOn && !screenIsOn ) {
    screenOn();
    screenIsOn = true; // Assume success in order to avoid next automated attempt.
  }

  /* TODO: we don't do next two lines unless we check projecSerial output. Instead we check Signal Trigger. */
  // smartthing.send("projecOn");
  // Serial.println("Sent: projecOn");
  // Serial.println( smartthing.shieldGetLastNetworkState() ); // Extra debugging if necessary.
}


/**
 * projecOff() - Projector Off
 */
void projecOff()
{
  int inByte = 0;
  smartthing.shieldSetLED(0, 1, 2); // blue

  /* Watching for Projector reponses may or may not be useful. I guess useful in case remote control is used in a separate function. */
  /* TODO: We could first read "PWR?" to get result ":PWR=00" or ":PWR=01 or 02" or ":ERR" */
  projecSendMsg( "PWR OFF" );

  /* We do this here to let the screen start movement ASAP. */
  if ( screenAutoOff && screenIsOn ) { 
    screenOff();
    screenIsOn = false; // Assume success in order to avoid next automated attempt.
  }

  /* TODO: we don't do next two lines unless we check projecSerial output. Instead we check Signal Trigger */
  // smartthing.send("projecOff");
  // Serial.println("Sent: projecOff");
  // Serial.println( smartthing.shieldGetLastNetworkState() ); // Extra debugging if necessary.
}


/**
 * disable() - Stop Screen (TODO: could also disable SmartThings control).
 */
void disable()
{
  smartthing.shieldSetLED(1, 0, 0); // red

//  digitalWrite( up, LOW);  // Be certain to lift the other button.  
//  doublePress( down );
//  singlePress( up );

  /* TODO: For pause (stop), singlePress() should be sufficient! */
  doublePress( pause );
  
  smartthing.send("disable");       // send message to cloud
  Serial.println("Sent: disable");
  // Serial.println( smartthing.shieldGetLastNetworkState() );
}


/**
 * enable() - Enable SmartThings (TODO: Just resets from disable for now.).
 */
void enable()
{
  smartthing.shieldSetLED(1, 2, 1); // 

//  digitalWrite( up, LOW);  // Be certain to lift the other button.  
//  doublePress( down );
//  singlePress( up );

  /* TODO: For pause (stop), singlePress() should be sufficient! */
  doublePress( pause );

  smartthing.send("enable");       // send message to cloud
  Serial.println("Sent: enable");
  // Serial.println( smartthing.shieldGetLastNetworkState() );
}


/**
 * updateProjector State in SmartThings
 * TODO: Perhaps name SmartThings outgoing messages with "state" or something.
 */
void updateProjector() {
  if (projectorOn) {
    Serial.println( "Sending: projecOn" );
    smartthing.send("projecOn");
  } else {
    Serial.println( "Sending: projecOff" );
    smartthing.send("projecOff");
  }
} /* updateProjector */


/**
 * messageCallout
 * TODO: Move projectorUpdate outside this function as well as callers in loop().
 */
void messageCallout(String message)
{
  Serial.print("In messageCallout.");
  // if debug is enabled print out the received message
  if (isDebugEnabled)
  {
    Serial.print("Received message: '");
    Serial.print(message);
    Serial.println("' ");
  }

  if (message.equals("screenOn"))
  {
    screenOn();
  }
  
  if (message.equals("screenOff"))
  {
    screenOff();
  }

  if (message.equals("projecOn"))
  {
    projecOn();
  }
  
  if (message.equals("projecOff"))
  {
    projecOff();
  }
  
  if (message.equals("disable"))
  {
    disable();
  }
  
  if (message.equals("enable"))
  {
    enable();
  }

  if ((message.substring(0,10)).equals("projecMsg:")) {
    String projecMsg = message.substring(10);
    char* projecMsgC = new char[projecMsg.length()+1];
    projecMsg.toCharArray(projecMsgC,projecMsg.length()+1);
    //Serial.print( "Received request to send message: " );
    //Serial.println( projecMsgC );
    projecSendMsg( projecMsgC );
  }
  
} /* messageCallout() */


/* =========== */
/* End of File */
/* =========== */
