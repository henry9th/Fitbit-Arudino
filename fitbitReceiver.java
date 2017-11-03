package assignment10.fitbit;

import jssc.SerialPortException;
import sedgewick.StdDraw; 
import studio4.SerialComm;

public class fitbitReceiver {
	final private SerialComm port;
	int byteRead;
	int byte1;
	int byte2;
	int byte3;
	int byte4;
	public static double sumCT;
	long sumTotal;
	static int prevMode; 
	static int maxStep = 0;
	static int maxSleep = 0; 
	public static int sumStep; 
	public static int sumSleep; 
	static double stepRate = 0; 
	int resetTime; 

	Chart Graph;



	//Chart sleepData = new Chart(45, 45);
	public fitbitReceiver(String portname) {
		StdDraw.enableDoubleBuffering();
		port = new SerialComm(portname);
		Graph = new Chart(60,-2,4); 
		Graph.axis("Time (seconds)", "Acceleration");
		Graph.title("FitBit Data");
	}

	enum State
	{
		initial,
		key,
		debug,
		error,
		temperature,
		stepCounter,
		sleepTimer,
		totalTimer,
		xPoint,
		yPoint, 
		zPoint,
		mode,
		switchMode, 
		accelGraph, 
		sleepGraph,
		peak,
		resetTime 

	}

	State readState = State.initial;

	public void run() {
		while (true) {
			if(port.available() == true){
				switch (readState) {
				case initial:
					byteRead = port.readByte();
					if (byteRead == 0x21) {
						readState = State.key;
					}	
					break;
				case key:
					byteRead = port.readByte();
					if (byteRead == 0x30) {
						readState = State.debug;
					}
					else if (byteRead == 0x35) {
						readState = State.temperature;
					}
					else if (byteRead == 0x40) {
						readState = State.stepCounter;
					}
					else if (byteRead == 0x41) {
						readState = State.sleepTimer;
					}
					else if (byteRead == 0x42) {
						readState = State.totalTimer;
					}
					else if (byteRead == 0x43) {
						readState = State.xPoint;
					}
					else if (byteRead == 0x44) {
						readState = State.yPoint; 
					}
					else if (byteRead == 0x45) {
						readState = State.zPoint;
					}
					else if (byteRead == 0x46){
						readState = State.mode;
					}
					else if (byteRead == 0x47){
						readState = State.peak; 

					}
					else if (byteRead == 0x48){
						readState = State.resetTime; 
					}
					else {
						readState = State.error;
					}

					break;

				case debug: 
					int a = port.readByte();
					int[] debuggArray = new int[a];
					for (int i = 0; i < a; i++) {
						debuggArray[i] = port.readByte() &0xff;
					}
					//System.out.print("Debugging String:");
					for(int i = 0; i < a; i++) {
						//System.out.print((char)debuggArray[i]);
					}
					//System.out.println();
					readState = State.initial;
					break;
				case error:
					//System.out.println("ERROR!!!");
					readState = State.initial;
					break;
				case temperature:
					byte1 = port.readByte() & 0xff;
					byte2 = port.readByte() & 0xff;
					byte3 = port.readByte() & 0xff;
					byte4 = port.readByte() & 0xff;
					int sign = byte1 >> 7 & 0x01;
					int exp = byte1 << 1;
					exp += byte2 >> 7 & 0x01;
					exp -= 127;
					int mantissa = (byte2 & 0x7f);
					mantissa = mantissa << 16;
					mantissa += (byte3 << 8);
					mantissa += byte4;
					double converted = 1 + ((double)mantissa / 0x7fffff);
					sumCT = Math.pow(2, exp) * converted * ((sign == 1)? -1 : 1);
					sumCT = (Math.round(sumCT * 100.0))/100.0;
					//StdDraw.show();
					//System.out.println("Temperature: " + sumCT + "C");
					readState = State.initial;
					break;	

					case stepCounter:
						byte1 = port.readByte() & 0xff;
						byte2 = port.readByte() & 0xff;
						byte3 = port.readByte() & 0xff;
						byte4 = port.readByte() & 0xff;
						int initSumStep = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
						if (initSumStep == 100000){
							maxStep = sumStep;
							sumStep = 0; 
						}
						else {
							sumStep = initSumStep; 
						}
//						System.out.print("Step Count: ");
						//System.out.println(sumStep);
						readState = State.initial;
						break;	
					case sleepTimer: 
						byte1 = port.readByte() & 0xff;
						byte2 = port.readByte() & 0xff;
						byte3 = port.readByte() & 0xff;
						byte4 = port.readByte() & 0xff;
						int initSleepSum = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
						if (initSleepSum == 100000){
							maxSleep = sumSleep;
							sumSleep = 0; 
						}
						else {
							sumSleep = initSleepSum; 
						}
						readState = State.initial;
						if (prevMode == 1){
							Graph.addPoint(sumTotal/1000.0, sumSleep, StdDraw.BLUE, true);
						}
						break;
					case totalTimer: // 2 byte
						byte1 = port.readByte() & 0xff;
						byte2 = port.readByte() & 0xff;
						byte3 = port.readByte() & 0xff;
						byte4 = port.readByte() & 0xff;
						sumTotal = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
						double hour = (sumTotal-resetTime)/(1000.0*60*60);
						//System.out.println(sumStep);
						stepRate = Math.round(((double)sumStep / hour)*100.0)/100.0;
						//System.out.println("Total Time: " + sumTotal/1000 + " seconds");
						readState = State.initial;
						break;
					case xPoint: 
						//System.out.println(sumTotal/1000.0);
						byte1 = port.readByte() & 0xff;
						byte2 = port.readByte() & 0xff;
						byte3 = port.readByte() & 0xff;
						byte4 = port.readByte() & 0xff;
						double xValue = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
						xValue = xValue/100.0; 
						
						readState = State.initial; 	
						//						System.out.println("X VALUE: " + xValue);
						if (prevMode == 0){
							Graph.addPoint(sumTotal/1000.0, xValue, StdDraw.BLUE, true);
						}
						break; 
					case yPoint: 
						byte1 = port.readByte() & 0xff;
						byte2 = port.readByte() & 0xff;
						byte3 = port.readByte() & 0xff;
						byte4 = port.readByte() & 0xff;
						double yValue = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
						yValue = yValue/100.0; 
						readState = State.initial;
						//System.out.println("Y VALUE: " + yValue);
						if (prevMode == 0){
							//System.out.println(sumTotal/1000.0);
							Graph.addPoint(sumTotal/1000.0, yValue, StdDraw.YELLOW, true);
						}
						break;
					case zPoint: 
						byte1 = port.readByte() & 0xff;
						byte2 = port.readByte() & 0xff;
						byte3 = port.readByte() & 0xff;
						byte4 = port.readByte() & 0xff;
						double zValue = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
						zValue = zValue/100.0; 
						readState = State.initial;
						//System.out.println("Z VALUE: " + zValue);
						if (prevMode == 0){
							Graph.addPoint(sumTotal/1000.0, zValue, StdDraw.RED, true);
						}
						break; 
					case mode: 
						byte1 = port.readByte() & 0xff;
						byte2 = port.readByte() & 0xff;
						byte3 = port.readByte() & 0xff;
						byte4 = port.readByte() & 0xff;
						int modeInt = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
						if (modeInt != prevMode){
							readState = State.switchMode;
							prevMode = modeInt; 
							break;
						}
						readState = State.initial; 
						break; 
					case switchMode:
						if (prevMode == 0){
							Graph.clear();
							Graph = new Chart(60,-2,4); 
							Graph.axis("Time (seconds)", "Accelerometer Data");
							Graph.title("ACCELEROMETER VALUES");
						}
						if (prevMode == 1){
							Graph.clear();
							Graph = new Chart(60, 0, 200);
							Graph.axis("Time (seconds)", "Sleep Time (seconds)");
							Graph.title("SLEEP COUNTER");
						}
						readState = State.initial; 
						break; 
					case peak: 
						Graph.addPoint(sumTotal/1000.0, 0, StdDraw.BLACK, false);
						readState = State.initial; 
						break; 
					case resetTime: 
						byte1 = port.readByte() & 0xff;
						byte2 = port.readByte() & 0xff;
						byte3 = port.readByte() & 0xff;
						byte4 = port.readByte() & 0xff;
						resetTime = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + byte4;
						//System.out.println(resetTime);
						readState = State.initial; 
						break; 
				}

			}
		}

	}

	public static void main(String[] args) {
		fitbitReceiver dataReceiver = new fitbitReceiver("COM5"); 
		dataReceiver.run();

	}
}