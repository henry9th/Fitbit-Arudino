#include <Wire.h> // Must include Wire library for I2C
#include <SparkFun_MMA8452Q.h> // Includes the SFE_MMA8452Q library
#include <math.h>
MMA8452Q accel;
/*
  Needed Protocols

  0x21 - Magic Number
  0x30 - Debugging String
  0x31 - Error String
  0x35 - converted( unfiltered) temperature reading, 4-byte float, degrees C
  0x40 - Step Counts
  0x41 - Time Spent Asleep
  0x42 - Total Time App has been Running
  0x43 - xData
  0x44 - yData
  0x45 - zData
  0x46 - mode
  0x47 - peakLabel
  0x48 - time since reset 
*/

int stepsLED = 3; //pin connected to LED during Steps mode
int sleepLED = 2; //pin connected to LED during Sleep mode
int switchButton = 4; //pin connected to button that switches state
int resetButton = 5; //pin connected to button that resets sleep or steps
bool switchButtonOn = 1; //boolean for state button on or off in checkButton method
bool resetButtonOn = 1; //boolean for reset button on or off in checkButton method
int previousSwitchButton = 1; //variables used to compare change in button
int previousResetButton = 1;
unsigned long sleepCounter = 0;
unsigned long stepCounter = 0;
int tmp = 0;
float unfilteredTemperature;
unsigned long milliSec; //For counting the total amount of time the app has been used
int interval = 100;  //number of points to check before point for peak
int interval2 = 100; //number of points to check after point for peak
unsigned long lastEndTime; //For delta timing for checkSleep
unsigned long lastEndTime2; ///For delta timing for sendData
const unsigned long checkSleepInterval = 1000;
const unsigned long checkStepsInterval = 0;
const unsigned long sendDataInterval = 0;
unsigned long resetTime; 
float stepThreshold = 1.3;
float sleepThreshold = 0.3; //minimum change in movement when sleep is not counted
float previousX = 0; //variables used to compare change in movement
float previousY = 0;
float previousZ = 0;
int mode = 0; //pedometer = 0, sleep = 1;
bool peak = 0; // for labeling peaks
bool maxSleep = 0;
bool maxStep = 0;
enum State {
  Pedometer,
  Sleep
};

State fitbitState = Pedometer;

State nextState (State state) {
  switch (state) {
    case Pedometer:
      //Serial.println("PEDOMETER");
      mode = 0 ;
      if (resetButtonOn == 0) { //Resets the Step Counter
        //Serial.println("STEP RESET");
        maxStep = 1;
        stepCounter = 0;
        resetTime = milliSec; 
      }
      if (switchButtonOn == 0) { //Switches to Sleep State
        state = Sleep;
        break;
      }
      digitalWrite(sleepLED, LOW);
      digitalWrite(stepsLED, HIGH);
      if (milliSec > lastEndTime) {
        checkPeak();
        lastEndTime = milliSec + checkStepsInterval;
        break;
      }
      break;
    case Sleep:
      mode = 1;
      //Serial.println("SLEEP");
      if (resetButtonOn == 0) { //Resets the Sleep Counter
        maxSleep = 1;
        resetButtonOn = 1; 
      }
      if (switchButtonOn == 0) { //Switches to Pedometer State
        state = Pedometer;
        break;
      }
      digitalWrite(stepsLED, LOW);
      digitalWrite(sleepLED, HIGH);
      if (milliSec > lastEndTime) {
        if (checkSleep()) {
          sleepCounter += 1;
        }
        //        Serial.print("Sleep Time: ");
        //        Serial.print(sleepCounter);
        //        Serial.println(" seconds");
        lastEndTime = milliSec + checkSleepInterval;
        break;
      }
      break;
  }
  return state;
}

void setup() {
  Serial.begin(9600);
  pinMode(stepsLED, OUTPUT);
  pinMode(sleepLED, OUTPUT);
  pinMode(switchButton, INPUT_PULLUP);
  pinMode(resetButton, INPUT_PULLUP);
  analogReference(INTERNAL);
  accel.init();

}

void loop() {
  milliSec = millis();
  checkButton();
  readTemp();
  if (accel.available()) {
    fitbitState = nextState(fitbitState);
    accel.read();
  }
  if (milliSec > lastEndTime2) {
    sendData();
    lastEndTime2 = milliSec + sendDataInterval;
  }

}

void readTemp() {
  int reading = analogRead(tmp);
  float readingConverted = (float) reading * 1.1 / 1023.0;
  unfilteredTemperature = 25 + (readingConverted - 0.75) * 100.0;
  //Serial.println(unfilteredTemperature);
}

void sendData() {
  if (maxStep == 1) {
    stepCounter = 100000;
    maxStep = 0 ;
  }
  byte numSteps[6];
  numSteps[0] = 0x21;
  numSteps[1] = 0x40;
  numSteps[2] = stepCounter >> 24;
  numSteps[3] = stepCounter >> 16;
  numSteps[4] = stepCounter >> 8;
  numSteps[5] = stepCounter;
  for (int i = 0; i < 6; i ++) {
    Serial.write(numSteps[i]);
  }
  if (stepCounter == 100000) {
    stepCounter = 0;
  }

  if (maxSleep == 1) {
    sleepCounter = 100000;
    maxSleep = 0;
  }
  byte sleepSeconds[6]; //send in seconds (convert to something more readable on receiving side)
  sleepSeconds[0] = 0x21;
  sleepSeconds[1] = 0x41;
  sleepSeconds[2] = sleepCounter >> 24;
  sleepSeconds[3] = sleepCounter >> 16;
  sleepSeconds[4] = sleepCounter >> 8;
  sleepSeconds[5] = sleepCounter;
  for (int i = 0; i < 6; i ++) {
    Serial.write(sleepSeconds[i]);
  }
  if (sleepCounter == 100000) {
    sleepCounter = 0;
  }

  byte timeReset[6]; 
  timeReset[0] = 0x21; 
  timeReset[1] = 0x48; 
  timeReset[2] = resetTime >> 24; 
  timeReset[3] = resetTime >> 16; 
  timeReset[4] = resetTime >> 8; 
  timeReset[5] = resetTime; 
  for (int i = 0; i < 6; i ++ ){
    Serial.write(timeReset[i]); 
  }

  byte secondsRan[6]; //send in seconds (convert to something more readable on receiving side)
  secondsRan[0] = 0x21;
  secondsRan[1] = 0x42;
  secondsRan[2] = milliSec >> 24;
  secondsRan[3] = milliSec >> 16;
  secondsRan[4] = milliSec >> 8;
  secondsRan[5] = milliSec;
  for (int i = 0; i < 6; i ++) {
    Serial.write(secondsRan[i]);
  }

  if (peak == 1) {
    Serial.println("FUCK");
    byte peakLabel[6];
    peakLabel[0] = 0x21;
    peakLabel[1] = 0x47;
    peakLabel[2] = 0x00;
    peakLabel[3] = 0x01;
    for (int i = 0; i < 4; i++) {
      Serial.write(peakLabel[i]);
    }
    peak = 0;
  }

  byte temperature[6];
  unsigned long unfilteredTmp = *(unsigned long*) & unfilteredTemperature;
  temperature[0] = 0x21;
  temperature[1] = 0x35;
  temperature[2] = unfilteredTmp >> 24;
  temperature[3] = unfilteredTmp >> 16;
  temperature[4] = unfilteredTmp >> 8;
  temperature[5] = unfilteredTmp;
  for (int i = 0; i < 6; i++) {
    Serial.write(temperature[i]);
  }

  sendDebug("debug!!!");

  byte errorTest[6];
  errorTest[0] = 0x21;
  errorTest[1] = 0x99;
  errorTest[2] = 0x69;
  errorTest[3] = 0x69;
  for (int i = 0; i < 4; i++) {
    Serial.write(errorTest[i]);
  }


  accel.read();
  double x = accel.cx;
  double y = accel.cy;
  double z = accel.cz;
  long xData = x * 100;
  long yData = y * 100;
  long zData = z * 100;
  byte xPoint[6];
  xPoint[0] = 0x21;
  xPoint[1] = 0x43;
  xPoint[2] = xData >> 24;
  xPoint[3] = xData >> 16;
  xPoint[4] = xData >> 8;
  xPoint[5] = xData;
  for (int i = 0; i < 6; i++) {
    Serial.write(xPoint[i]);
  }

  byte yPoint[6];
  yPoint[0] = 0x21;
  yPoint[1] = 0x44;
  yPoint[2] = yData >> 24;
  yPoint[3] = yData >> 16;
  yPoint[4] = yData >> 8;
  yPoint[5] = yData;
  for (int i = 0; i < 6; i++) {
    Serial.write(yPoint[i]);
  }

  byte zPoint[6];
  zPoint[0] = 0x21;
  zPoint[1] = 0x45;
  zPoint[2] = zData >> 24;
  zPoint[3] = zData >> 16;
  zPoint[4] = zData >> 8;
  zPoint[5] = zData;
  for (int i = 0; i < 6; i++) {
    Serial.write(zPoint[i]);
  }


  byte graphMode[6];
  graphMode[0] = 0x21;
  graphMode[1] = 0x46;
  graphMode[2] = mode >> 24;
  graphMode[3] = mode >> 16;
  graphMode[4] = mode >> 8;
  graphMode[5] = mode;
  for (int i = 0; i < 6; i++) {
    Serial.write(graphMode[i]);
  }
  //  Serial.println("");
  //  Serial.println("******************************************************");
  //  Serial.print("Temperature: ");
  //  Serial.println(unfilteredTemperature);
  //  Serial.print("Sleep Seconds: ");
  //  Serial.println(sleepCounter);
  //  Serial.print("Steps: ");
  //  Serial.println(stepCounter);
  //  Serial.print("Total Seconds: ");
  //  Serial.println(seconds);
  //  Serial.println("******************************************************");


}


void sendAccelData(double x, double y, double z) {
  long xData = x * 100;
  long yData = y * 100;
  long zData = z * 100;
  byte xPoint[6];
  xPoint[0] = 0x21;
  xPoint[1] = 0x43;
  xPoint[2] = xData >> 24;
  xPoint[3] = xData >> 16;
  xPoint[4] = xData >> 8;
  xPoint[5] = xData;
  for (int i = 0; i < 6; i++) {
    Serial.write(xPoint[i]);
  }

  byte yPoint[6];
  yPoint[0] = 0x21;
  yPoint[1] = 0x44;
  yPoint[2] = yData >> 24;
  yPoint[3] = yData >> 16;
  yPoint[4] = yData >> 8;
  yPoint[5] = yData;
  for (int i = 0; i < 6; i++) {
    Serial.write(yPoint[i]);
  }

  byte zPoint[6];
  zPoint[0] = 0x21;
  zPoint[1] = 0x45;
  zPoint[2] = zData >> 24;
  zPoint[3] = zData >> 16;
  zPoint[4] = zData >> 8;
  zPoint[5] = zData;
  for (int i = 0; i < 6; i++) {
    Serial.write(zPoint[i]);
  }
}

bool checkSleep() {
  //Find different in movement as a vector and then find magnitude and compare to threshold
  //Returns true if magnitude is less than threshold
  accel.read();
  float xVector = accel.cx - previousX;
  float yVector = accel.cy - previousY;
  float zVector = accel.cz = previousZ;
  previousX = accel.cx;
  previousY = accel.cy;
  previousZ = accel.cz;
  float vectorMag = sqrt(sq(xVector) + sq(yVector) + sq(zVector)); //compute magnitude of vector
  if (vectorMag >= sleepThreshold) {
    return false;
  }
  else {
    return true;
  }
}

void checkPeak() {
  int counter = 0;
  float mean1 = 0;
  float mean2 = 0;
  byte pl = accel.readPL();
  float pointPUp = 0.0;
  float pointPDown = 0.0;
  float pointLR = 0.0;
  float pointLL = 0.0;
  float pointFUp = 0.0;
  float pointFDown = 0.0;
  switch (pl)
  {
    case PORTRAIT_U:
      while (counter != interval)
      { //takes the first interval of points before main point to compare
        accel.read();
        mean1 += (accel.cy * (-1));
        counter += 1;
      }
      mean1 = mean1 / interval;
      counter = 0;
      accel.read();
      pointPUp = (accel.cy * (-1)); //assign the point between the two intervals being compared
      while (counter != interval2) { //takes the second interval of points after main point to compare
        accel.read();
        mean2 += (accel.cy * (-1));
        counter += 1;
      }
      mean2 = mean2 / interval2;
      if (pointPUp > mean1 && pointPUp > mean2 && pointPUp > stepThreshold) { //if point is greater than first interval but less than second interval and pass threshold, then it's a peak
        stepCounter += 1;
        peak = 1;
      }
      //Serial.print("Portrait Up");
      break;
    case PORTRAIT_D:
      while (counter != interval)
      { //takes the first interval of points before main point to compare
        accel.read();
        mean1 += (accel.cy);
        counter += 1;
      }
      mean1 = mean1 / interval;
      counter = 0;
      accel.read();
      pointPDown = (accel.cy); //assign the point between the two intervals being compared
      while (counter != interval2) { //takes the second interval of points after main point to compare
        accel.read();
        mean2 += (accel.cy);
        counter += 1;
      }
      mean2 = mean2 / interval2;
      if (pointPDown > mean1 && pointPDown > mean2 && pointPDown > stepThreshold) { //if point is greater than first interval but less than second interval and pass threshold, then it's a peak
        stepCounter += 1;
        peak = 1;
      }
      //Serial.print("Portrait Down");
      break;
    case LANDSCAPE_R:
      while (counter != interval)
      { //takes the first interval of points before main point to compare
        accel.read();
        mean1 += (accel.cx);
        counter += 1;
      }
      mean1 = mean1 / interval;
      counter = 0;
      accel.read();
      pointLR = (accel.cx); //assign the point between the two intervals being compared
      while (counter != interval2) { //takes the second interval of points after main point to compare
        accel.read();
        mean2 += (accel.cx);
        counter += 1;
      }
      mean2 = mean2 / interval2;
      if (pointLR > mean1 && pointLR > mean2 && pointLR > stepThreshold) { //if point is greater than first interval but less than second interval and pass threshold, then it's a peak
        stepCounter += 1;
        peak = 1;
      }
      //Serial.print("Landscape Right");
      break;
    case LANDSCAPE_L:
      while (counter != interval)
      { //takes the first interval of points before main point to compare
        accel.read();
        mean1 += (accel.cx * (-1));
        counter += 1;
      }
      mean1 = mean1 / interval;
      counter = 0;
      accel.read();
      pointLL = (accel.cx * (-1)); //assign the point between the two intervals being compared
      while (counter != interval2) { //takes the second interval of points after main point to compare
        accel.read();
        mean2 += (accel.cx * (-1));
        counter += 1;
      }
      mean2 = mean2 / interval2;
      if (pointLL > mean1 && pointLL > mean2 && pointLL > stepThreshold) { //if point is greater than first interval but less than second interval and pass threshold, then it's a peak
        stepCounter += 1;
        peak = 1;
      }
      //Serial.print("Landscape Left");
      break;
    case LOCKOUT:
      if (accel.cz < 0)
      {
        while (counter != interval)
        { //takes the first interval of points before main point to compare
          accel.read();
          mean1 += (accel.cz * (-1));
          counter += 1;
        }
        mean1 = mean1 / interval;
        counter = 0;
        accel.read();
        pointFUp = (accel.cz * (-1)); //assign the point between the two intervals being compared
        while (counter != interval2) { //takes the second interval of points after main point to compare
          accel.read();
          mean2 += (accel.cz * (-1));
          counter += 1;
        }
        mean2 = mean2 / interval2;
        if  (pointFUp > mean1 && pointFUp > mean2 && pointFUp > stepThreshold) { //if point is greater than first interval but less than second interval and pass threshold, then it's a peak
          stepCounter += 1;
          peak = 1;
        }
      }
      else
      {
        while (counter != interval)
        { //takes the first interval of points before main point to compare
          accel.read();
          mean1 += (accel.cz);
          counter += 1;
        }
        mean1 = mean1 / interval;
        counter = 0;
        accel.read();
        pointFDown = (accel.cz); //assign the point between the two intervals being compared
        while (counter != interval2) { //takes the second interval of points after main point to compare
          accel.read();
          mean2 += (accel.cz);
          counter += 1;
        }
        mean2 = mean2 / interval2;
        if  (pointFDown > mean1 && pointFDown > mean2 && pointFDown > stepThreshold) { //if point is greater than first interval but less than second interval and pass threshold, then it's a peak
          stepCounter += 1;
          peak = 1;
        }
        break;
      }

  }

}

void checkButton() { //checks button and also doesn't allow holding button down
  switchButtonOn = 1;
  if (switchButtonOn == 1) {
    if (digitalRead(switchButton) == 0 && digitalRead(switchButton) != previousSwitchButton) {
      switchButtonOn = 0;
    }
  }
  else if (switchButtonOn == 0) {
    if (digitalRead(switchButton) == 1) {
      switchButtonOn = 1;
    }
  }
  previousSwitchButton = digitalRead(switchButton);

  if (resetButtonOn == 1) {
    if (digitalRead(resetButton) == 0 && digitalRead(resetButton) != previousResetButton) {
      resetButtonOn = 0;
    }
  }
  if (resetButtonOn == 0) {
    if (digitalRead(resetButton) == 1) {
      resetButtonOn = 1;
    }
  }
  previousResetButton = digitalRead(resetButton);
}

void sendDebug(char * s) {
  Serial.write(0x21);
  Serial.write(0x30);
  Serial.write(8);
  Serial.write(s);
} 
