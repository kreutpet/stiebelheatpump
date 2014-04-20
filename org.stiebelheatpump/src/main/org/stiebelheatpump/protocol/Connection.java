/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.stiebelheatpump.protocol;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.stiebelheatpump.internal.StiebelHeatPumpException;

public class Connection {

	private final String serialPortName;
	private SerialPort serialPort;
	/** baud rate of serial port */
	private int baudRate;
	
	private int timeout = 5000;

	//private DataOutputStream os;
	//private DataInputStream is;

	private OutputStream os;
	private InputStream is;
    ByteStreamPipe byteStreamPipe = null;
	
	private static final int INPUT_BUFFER_LENGTH = 1024;
	private byte[] buffer = new byte[INPUT_BUFFER_LENGTH];

	public static byte ESCAPE = (byte) 0x10;
	public static byte HEADERSTART = (byte) 0x01;
	public static byte END = (byte) 0x03;
	public static byte GET = (byte) 0x00;
	public static byte SET = (byte) 0x80;
	public static byte STARTCOMMUNICATION = (byte) 0x02;
	public static byte[] FOOTER = { ESCAPE, END };
	public static byte[] DATAAVAILABLE = { ESCAPE, STARTCOMMUNICATION };
	
	public static byte VERSIONREQUEST = (byte) 0xfd;
	public static byte VERSIONCHECKSUM = (byte) 0xfe;
	public static byte[] REQUESTVERSION = { HEADERSTART,GET,VERSIONCHECKSUM,VERSIONREQUEST,ESCAPE,END };
	public static byte VALUECHECKSUM = (byte) 0xfc;
	public static byte VALUEREQUEST = (byte) 0xfb;
	public static byte[] REQUESTVALUES = { HEADERSTART,GET,VALUECHECKSUM,VALUEREQUEST,ESCAPE,END };
	
	private static final Charset charset = Charset.forName("US-ASCII");

	private static final int SLEEP_INTERVAL = 100;

	/**
	 * Creates a Connection object. You must call <code>connect()</code> before calling <code>read()</code> in order to
	 * read data. The timeout is set by default to 5s.
	 * 
	 * @param serialPort
	 *            examples for serial port identifiers are on Linux "/dev/ttyS0" or "/dev/ttyUSB0" and on Windows "COM1"
	 */
	public Connection(String serialPort) {
		if (serialPort == null) {
			throw new IllegalArgumentException("serialPort may not be NULL");
		}

		serialPortName = serialPort;
	}
	
	/**
	 * Creates a Connection object. You must call <code>connect()</code> before calling <code>read()</code> in order to
	 * read data. The timeout is set by default to 5s.
	 * 
	 * @param serialPort
	 *            examples for serial port identifiers are on Linux "/dev/ttyS0" or "/dev/ttyUSB0" and on Windows "COM1"
	 */
	public Connection(String serialPort, int baudrate) {
		if (serialPort == null) {
			throw new IllegalArgumentException("serialPort may not be NULL");
		}

		this.serialPortName = serialPort;
		this.baudRate = baudrate;
	}

	/**
	 * Sets the maximum time in ms to wait for new data from the remote device. A timeout of zero is interpreted as an
	 * infinite timeout.
	 * 
	 * @param timeout
	 *            the maximum time in ms to wait for new data.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Returns the timeout in ms.
	 * 
	 * @return the timeout in ms.
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Opens the serial port associated with this connection.
	 * 
	 * @throws IOException
	 *             if any kind of error occurs opening the serial port.
	 */
	public void connect() throws IOException {

		CommPortIdentifier portIdentifier;
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);

		} catch (NoSuchPortException e) {
			throw new IOException("Serial port with given name does not exist", e);
		}

		if (portIdentifier.isCurrentlyOwned()) {
			throw new IOException("Serial port is currently in use.");
		}

		RXTXPort commPort;
		try {
			commPort = portIdentifier.open(this.getClass().getName(), 2000);
		} catch (PortInUseException e) {
			throw new IOException("Serial port is currently in use.", e);
		}

		if (!(commPort instanceof SerialPort)) {
			commPort.close();
			throw new IOException("The specified CommPort is not a serial port");
		}

		serialPort = (RXTXPort) commPort;
		// Set the parameters of the connection.
		setSerialPortParameters(baudRate);

		try {
			//os  = serialPort.getOutputStream();
			os = new DataOutputStream(serialPort.getOutputStream());
			//is = serialPort.getInputStream();
			is = new DataInputStream(serialPort.getInputStream());
		} catch (IOException e) {
			serialPort.close();
			serialPort = null;
			throw new IOException("Error getting input or output or input stream from serial port", e);
		}
	}

	/**
	 * Closes the serial port.
	 */
	public void disconnect() {
		if (serialPort != null) {
			try {
				// close the i/o streams.
				os.close();
				is.close();
			} catch (IOException ex) {
				// don't care
			}
			// Close the port.
			serialPort.close();
			serialPort = null;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Requests the firmware version from the stiebel heat pump using serial connection.
	 * 
	 * @return A version string.
	 * 
	 * @throws IOException
	 *             if any kind of error other than timeout occurs while trying to read the remote device. Note that the
	 *             connection is not closed when an IOException is thrown.
	 * @throws TimeoutException
	 *             if no response at all (not even a single byte) was received from the meter within the timeout span.
	 */
	public String readVersion() throws IOException, TimeoutException {

		if (serialPort == null) {
			throw new IllegalStateException("Connection is not open.");
		}

		if(!startCommunication()){
			return "No Communication";
		}
		
		short myNumber = getVersionInfo();

		return String.valueOf(myNumber);
	}

	/**
	 * Requests the data from the stiebel heat pump using serial connection.
	 * 
	 * @return map of values
	 * 
	 * @throws IOException
	 *             if any kind of error other than timeout occurs while trying to read the remote device. Note that the
	 *             connection is not closed when an IOException is thrown.
	 * @throws TimeoutException
	 *             if no response at all (not even a single byte) was received from the meter within the timeout span.
	 */
	public Map<String, String> readCurrentValues(Request request) throws IOException, TimeoutException {

		if (serialPort == null) {
			throw new IllegalStateException("Connection is not open.");
		}
		
		return getDataByRequest(request);
	}
	
	private short getVersionInfo() throws IOException {
		// send version request
		for (byte abyte : REQUESTVERSION){
			os.write(abyte);
		}		
		os.flush();		
		
		boolean readSuccessful = false;
		int numBytesReadTotal = 0;
		int timeval = 0;
		
		// receive data are available "0x10 0x02"
		while (timeout == 0 || timeval < timeout) {
			if (is.available() > 0) {
				int numBytesRead = is.read(buffer, numBytesReadTotal, INPUT_BUFFER_LENGTH - numBytesReadTotal);
				numBytesReadTotal += numBytesRead;

				if (numBytesRead > 0) {
					timeval = 0;
				}

				if (numBytesReadTotal > 1 && buffer[numBytesReadTotal-1] == STARTCOMMUNICATION) {
					readSuccessful = true;
					break;
				}
			}

			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
			}

			timeval += SLEEP_INTERVAL;
		}

		if (!readSuccessful) {
			throw new IOException("Did not receive any data available message !");
		}
		
		if (numBytesReadTotal != 2) {
			throw new IOException("Data available message does not have length of 2!");
		}
		
		// send acknowledgment 
		os.write(ESCAPE);
		os.flush();
		
		readSuccessful = false;
		numBytesReadTotal = 0;
		timeval = 0;
		buffer = new byte[INPUT_BUFFER_LENGTH];
		// receive version information
		while (timeout == 0 || timeval < timeout) {
			if (is.available() > 0) {
				int numBytesRead = is.read(buffer, numBytesReadTotal, INPUT_BUFFER_LENGTH - numBytesReadTotal);
				numBytesReadTotal += numBytesRead;

				if (numBytesRead > 0) {
					timeval = 0;
				}

				if (numBytesReadTotal == 8 && buffer[numBytesReadTotal-1] == END) {
					readSuccessful = true;
					break;
				}
			}

			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
			}

			timeval += SLEEP_INTERVAL;
		}

		if (!readSuccessful) {
			throw new IOException("Did not receive version info !");
		}
		
		if (numBytesReadTotal != 8) {
			throw new IOException("Data available message does not have length of 8!");
		}
		
		
		if (buffer[numBytesReadTotal - 1] != END && buffer[numBytesReadTotal - 2] != ESCAPE) {
			throw new IOException("Data message does not have footer!");
		}

		ByteBuffer versionBytes = ByteBuffer.wrap(buffer);
		short myNumber = (short) versionBytes.getShort(4);
		return myNumber;
	}

	private Map<String, String> getDataByRequest(Request request) throws IOException {
		
		short checkSum = calculateChecksum(
				new byte[] { request.getRequestByte()});
		byte[] requestMessage = { 
				HEADERSTART,GET, 
				shortToByte(checkSum)[0], request.getRequestByte(),
				ESCAPE,	END };
		
		// send version request
		for (byte abyte : requestMessage){
			os.write(abyte);
		}
		try {
			Thread.sleep(SLEEP_INTERVAL);
		} catch (InterruptedException e) {
		}
		os.flush();		
		
		boolean readSuccessful = false;
		int numBytesReadTotal = 0;
		int timeval = 0;
		
		// receive data are available "0x10 0x02"
		while (timeout == 0 || timeval < timeout) {
			if (is.available() > 0) {
				int numBytesRead = is.read(buffer, numBytesReadTotal, INPUT_BUFFER_LENGTH - numBytesReadTotal);
				numBytesReadTotal += numBytesRead;

				if (numBytesRead > 0) {
					timeval = 0;
				}

				if (numBytesReadTotal > 1 && buffer[numBytesReadTotal-1] == STARTCOMMUNICATION) {
					readSuccessful = true;
					break;
				}
			}

			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
			}

			timeval += SLEEP_INTERVAL;
		}

		if (!readSuccessful) {
			throw new IOException("Did not receive any data available message !");
		}
		
		if (numBytesReadTotal != 2) {
			throw new IOException("Data available message does not have length of 2!");
		}
		
		// send acknowledgment 
		os.write(ESCAPE);
		os.flush();
		
		readSuccessful = false;
		numBytesReadTotal = 0;
		timeval = 0;
		buffer = new byte[INPUT_BUFFER_LENGTH];
		// receive version information
		while (timeout == 0 || timeval < timeout) {
			if (is.available() > 0) {
				int numBytesRead = is.read(buffer, numBytesReadTotal, INPUT_BUFFER_LENGTH - numBytesReadTotal);
				numBytesReadTotal += numBytesRead;

				if (numBytesRead > 0) {
					timeval = 0;
				}

				if (numBytesReadTotal == 8 && buffer[numBytesReadTotal-1] == END) {
					readSuccessful = true;
					break;
				}
			}

			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
			}

			timeval += SLEEP_INTERVAL;
		}

		if (!readSuccessful) {
			throw new IOException("Did not receive version info !");
		}
		
		if (numBytesReadTotal != 8) {
			throw new IOException("Data available message does not have length of 8!");
		}
		
		
		if (buffer[numBytesReadTotal - 1] != END && buffer[numBytesReadTotal - 2] != ESCAPE) {
			throw new IOException("Data message does not have footer!");
		}

		byte[] responseBuffer = new byte[numBytesReadTotal];
		System.arraycopy(buffer, 0, responseBuffer, 0, numBytesReadTotal);
		;
		Map<String, String> data = new HashMap<String, String>();
		// get data from heat pump
		StiebelHeatPumpDataParser parser = new StiebelHeatPumpDataParser();
		try {
			data.putAll(parser.parseRecords(responseBuffer, request));
		} catch (StiebelHeatPumpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return data;	
	}
		
	public boolean startCommunication() throws IOException, TimeoutException {
		
		boolean readSuccessful;
		int numBytesReadTotal;
		
		os.write(STARTCOMMUNICATION);
		os.flush();
		
		readSuccessful = false;
		int timeval = 0;
		numBytesReadTotal = 0;

		while (timeout == 0 || timeval < timeout) {
			if (is.available() > 0) {

				int numBytesRead = is.read(buffer, numBytesReadTotal, INPUT_BUFFER_LENGTH - numBytesReadTotal);
				numBytesReadTotal += numBytesRead;

				if (numBytesRead > 0) {
					timeval = 0;
				}

				if (buffer[0] == ESCAPE ) {
					readSuccessful = true;
					break;
				}
			}

			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
			}

			timeval += SLEEP_INTERVAL;
		}
		
		int offset = 0;

		if (numBytesReadTotal == offset) {
			throw new TimeoutException();
		}

		if (!readSuccessful) {
			throw new IOException("Timeout while listening for Identification Message");
		}

		if (buffer[0] == ESCAPE){
			System.out.println("Stiebel heatpump serial port ready for request.");
		}else{
			System.out.println("Stiebel heatpump serial port could not be connected with start communication request!");				
		}
		return readSuccessful;
	}
	
	/**
	 * Sets the serial port parameters to xxxxbps-8N1
	 * 
	 * @param baudrate
	 *            used to initialize the serial connection
	 */
	protected void setSerialPortParameters(int baudrate) throws IOException {

		try {
			// Set serial port to xxxbps-8N1
			serialPort.setSerialPortParams(baudRate,
					SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException ex) {
			throw new IOException(
					"Unsupported serial port parameter for serial port");
		}
	}

	/**
	 * calculates the checksum of a byte data array
	 * 
	 * @param data
	 *            to calculate the checksum for
	 * @param withReplace
	 *            to set if the byte array shall be corrected by special replace
	 *            method
	 * @return calculated checksum as short
	 */
	private byte calculateChecksum(byte[] data) {
		
		byte[] dataWithoutHeaderFooter = data;
		if (data.length>4){
			dataWithoutHeaderFooter = Arrays.copyOfRange(data, 3,
					data.length - 2);
		}
		
		short checkSum = 1, i = 0;
		for (i = 0; i < dataWithoutHeaderFooter.length; i++) {
			checkSum += (short) (dataWithoutHeaderFooter[i] & 0xFF);
		}

		return shortToByte(checkSum)[0];
	}
	
	/**
	 * converts short to byte
	 * 
	 * @return array of bytes
	 */
	private byte[] shortToByte(short value) {
		byte[] returnByteArray = new byte[2];
		returnByteArray[0] = (byte) (value & 0xff);
		returnByteArray[1] = (byte) ((value >> 8) & 0xff);

		return returnByteArray;
	}


}
