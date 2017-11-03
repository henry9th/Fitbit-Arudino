package studio4;

import jssc.SerialPort;
import jssc.SerialPortException;


public class SerialComm {
	static boolean debug = true; 
	static SerialPort port; 

	public SerialComm(String name){
		port = new SerialPort(name);
		try {
			port.openPort();
			port.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		} 
		catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean available(){
		try {
			int byteRead = port.getInputBufferBytesCount();
			if (byteRead > 0){
				return true;
			}
		} catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false; 
	}

	
	public static byte readByte() {
		byte[] bytes= new byte[1];
		debug = false; 
		String c = "";
		try {
			bytes = port.readBytes(1);
		} catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (debug)
		{
			c = String.format("%02x", bytes[0]);
			System.out.println("[0x" + c + "]");
			debug = false; 
		}
		return bytes[0];		
	}
	
	public static byte writeBytes(Byte b) { 
		debug = false; 
		String c = "";
		 try {
			port.writeByte(b);
		} 
		 catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (debug) {
			c = String.format("%02x", b);
			System.out.println("[0x" + c + "]");
			debug = false; 
		}
		return b; 
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new SerialComm("COM4");
		while(true){
			if (available()){  
					System.out.print((char) readByte());
				}		
		}

	}
}
