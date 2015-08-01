/*****************************************************************************
 * @file
 * @brief
 *   Arduino SmartThings Shield LED Example
 * 2015/5/13 - A few modifications by CosmicPuppy to send responses for every command (on, off, and Hello - h0, h1).
 */
#include <SoftwareSerial.h>   //TODO need to set due to some weird wire language linker, should we absorb this whole library into smartthings
#include <SmartThings.h>

#define PIN_THING_RX    3
#define PIN_THING_TX    2

SmartThingsCallout_t messageCallout;    // call out function forward decalaration
SmartThings smartthing(PIN_THING_RX, PIN_THING_TX, messageCallout, "", false);  // constructor


int ledPin = 13;
bool isDebugEnabled = true;    // enable or disable debug in this example

int downRelayPin = 12;
int upRelayPin = 11;
int holdDuration = 300; // duration of the relay on "press"
int waitDuration = 100; // duration of the relay between "presses".

int inputPin = 7; // 12vDC mono trigger signal input from Projector
bool projectorOn = false;
bool projectorUpdate = false; /* Let status of Projector be unsent until we see it change? */
int inputPinSum = 0; // We will sum the value of input pin over several samples to make sure it is really ON or really OFF.
int loopCount = 0;

/* TODO: These could be made on/off configuration options from the SmartThings Device Tiles! */
/*       or change them to #define */
bool screenAutoOn = true;   // Localized automatic screen down/on when Projector ON  if true.
bool screenAutoOff = false; // Localized automatic screen up/off  when Projector OFF if true.


/**
 * setup
 */
void setup()
{
  /* Input trigger from Projector */
  analogReference(DEFAULT);
  pinMode(inputPin, INPUT);
  digitalWrite(inputPin, LOW); // Must be LOW else always get 1. Is this Pulldown enabled?

  /* Set mode for Hardware Pins */
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, HIGH);

  pinMode(downRelayPin, OUTPUT);
  digitalWrite(downRelayPin, LOW);

  pinMode(upRelayPin, OUTPUT);
  digitalWrite(upRelayPin, LOW);

  if (isDebugEnabled)
  { // setup debug serial port
    Serial.begin(9600);         // setup serial with a baud rate of 9600
    Serial.println("setup..");  // print out 'setup..' on start
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
    if ( !projectorOn ) {
      projectorOn = true;
      projectorUpdate = true; /* Let status of Projector be unsent until we see it change? */
      messageCallout( "" );
      if ( screenAutoOn ) on();
    }
    loopCount = 0;
    inputPinSum = 0;
  } else if ( loopCount >= 50 && inputPinSum < 5 ) {
    if ( projectorOn ) {
      projectorOn = false;
      projectorUpdate = true; /* Let status of Projector be unsent until we see it change? */
      messageCallout( "" );
      if ( screenAutoOff ) off();
    }
    loopCount = 0;
    inputPinSum = 0;
  }
  if ( loopCount > 50 ) { loopCount = 0; inputPinSum = 0; }
  loopCount ++;

/*
  Serial.print("loopCount: ");
  Serial.print(loopCount);
  Serial.print(" inputPinSum: ");
  Serial.print(inputPinSum);
  Serial.print(" ProjectorOn Value: ");
  Serial.print(projectorOn);
  Serial.println(".");
*/

  smartthing.run();

} /* loop() */


/**
 * doublePress
 */
void doublePress( int whichPin )
{
  singlePress( whichPin );
  singlePress( whichPin );
}


/**
 * singlePress
 */
void singlePress( int whichPin )
{
  digitalWrite(whichPin, HIGH);
  delay(holdDuration);
  digitalWrite(whichPin, LOW);
  delay(waitDuration);
}


/**
 * on() - Screen Down
 */
void on()
{
  smartthing.shieldSetLED(0, 1, 0); // green

  digitalWrite( upRelayPin, LOW);  // Be certain to lift the other button.
  doublePress( downRelayPin );

  smartthing.send("on");        // send message to cloud
  Serial.println("Sent: on");
  // Serial.println( smartthing.shieldGetLastNetworkState() );
}


/**
 * off() - Screen Up
 */
void off()
{
  smartthing.shieldSetLED(0, 0, 1); // blue

  digitalWrite( downRelayPin, LOW);  // Be certain to lift the other button.
  doublePress( upRelayPin );

  smartthing.send("off");       // send message to cloud
  Serial.println("Sent: off");
  // Serial.println( smartthing.shieldGetLastNetworkState() );
}


/**
 * disable() - Stop Screen (TODO: could also disable SmartThings control).
 */
void disable()
{
  smartthing.shieldSetLED(1, 0, 0); // red

  digitalWrite( upRelayPin, LOW);  // Be certain to lift the other button.
  singlePress( downRelayPin );
  singlePress( upRelayPin );

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

  digitalWrite( upRelayPin, LOW);  // Be certain to lift the other button.
  singlePress( downRelayPin );
  singlePress( upRelayPin );

  smartthing.send("enable");       // send message to cloud
  Serial.println("Sent: enable");
  // Serial.println( smartthing.shieldGetLastNetworkState() );
}


/**
 * messageCallout
 * TODO: Move projectorUpdate outside this function as well as callers in loop().
 */
void messageCallout(String message)
{
  // if debug is enabled print out the received message
  if (isDebugEnabled)
  {
    Serial.print("Received message: '");
    Serial.print(message);
    Serial.println("' ");
  }

  if (message.equals("on"))
  {
    on();
  }

  if (message.equals("off"))
  {
    off();
  }

  if (message.equals("disable"))
  {
    disable();
  }

  if (message.equals("enable"))
  {
    enable();
  }

  /* TODO: This should be own function, not here in messageCallout(). */
  /*       But ... if we add remote projON / projOFF (RS232), then we move things around anyway. */
  if (projectorUpdate)
  {
    projectorUpdate = false;
    if (projectorOn) {
      Serial.println( "Sending: projON" );
      smartthing.send("projON");
    } else {
      Serial.println( "Sending: projOFF" );
      smartthing.send("projOFF");
    }
  }

} /* messageCallout() */


/* =========== */
/* End of File */
/* =========== */