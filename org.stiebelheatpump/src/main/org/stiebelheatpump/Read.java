package org.stiebelheatpump;

/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */


import java.io.IOException;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stiebelheatpump.internal.ConfigLocator;

import org.stiebelheatpump.protocol.Connection;
import org.stiebelheatpump.protocol.Request;
import org.stiebelheatpump.protocol.SerialConnector;

public class Read {

	private static String configFile;
	private static int baudRate = 9600;
	private static List<Request> heatPumpConfiguration;
	private static SerialConnector connection;

	private static final int INPUT_BUFFER_LENGTH = 1024;
	private static byte[] buffer = new byte[INPUT_BUFFER_LENGTH];
	
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
	
    private static final Logger logger = LoggerFactory.getLogger(SerialConnector.class);

	private static void printUsage() {
		System.out.println("SYNOPSIS\n\torg.stiebelheatpump.Read [-b <baud_rate>] -c <config_file> <serial_port>");
		System.out
				.println("DESCRIPTION\n\tReads the heat pump version connected to the given serial port and prints the received data to stdout. Errors are printed to stderr.");
		System.out.println("OPTIONS");
		System.out
				.println("\t<serial_port>\n\t    The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows)\n");
		System.out
				.println("\t-b <baud_rate>\n\t    Baud rate. Default is 9600.\n");
		System.out
				.println("\t-c <config_file>\n\t    Configuration file containing the request definitions for heatpump version.\n");
	}

	public static void main(String[] args) {
		if (args.length < 1 || args.length > 5) {
			printUsage();
			System.exit(1);
		}

		String serialPortName = "";
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-b")) {
				i++;
				if (i == args.length) {
					printUsage();
					System.exit(1);
				}
				try {
					baudRate = Integer.parseInt(args[i]);
				} catch (NumberFormatException e) {
					printUsage();
					System.exit(1);
				}
			}
			else if (args[i].equals("-c")) {
				i++;
				if (i == args.length) {
					printUsage();
					System.exit(1);
				}
				configFile = args[i];
			}
			else {
				serialPortName = args[i];
			}
		}

		if (!getHeatPumpConfiguration()) {
			System.err.print("Could not load configuration! Can't find heat pump configuration.");
			System.exit(1);
		}
		
		connection = new SerialConnector();
		connection.connect(serialPortName);
		readVersionNew();
		//readVersion(connection);
		//readData(connection);

		connection.disconnect();
		connection = null;
		
	}

	private static void readData(Connection connection) {
		Map<String, String> heatPumpData = new HashMap<String, String>();
		
		Request dataRequest = null;

		for (Request request : heatPumpConfiguration) {
			System.out.println(
					"Request : Name -> " + request.getName() +
					" | Description -> " + request.getDescription() +
					" Requestbyte -> " + 
					DatatypeConverter.printHexBinary(new byte[] { request.getRequestByte() }));
			dataRequest = request;
			System.out.println("Loaded Request : " + dataRequest.getDescription());
		}

		try {
			connection.connect();
			connection.startCommunication();
			heatPumpData = connection.readCurrentValues(dataRequest);
			for (Map.Entry<String, String> entry : heatPumpData.entrySet()) {
				System.out.println("Name : " + entry.getKey() + " Value : "
					+ entry.getValue());
			}

		} catch (IOException e) {
			System.err.println("IOException while trying to read: " + e.getMessage());
			connection.disconnect();
			System.exit(1);
		} catch (TimeoutException e) {
			System.err.print("Read attempt timed out");
			connection.disconnect();
			System.exit(1);
		}finally{
			connection.disconnect();
		}
	}

	private static void readVersion(Connection connection) {
		String version = "";
		try {
			connection.connect();
			version = connection.readVersion();
		} catch (IOException e) {
			System.err.println("IOException while trying to read: " + e.getMessage());
			connection.disconnect();
			System.exit(1);
		} catch (TimeoutException e) {
			System.err.print("Read attempt timed out");
			connection.disconnect();
			System.exit(1);
		}finally{
			connection.disconnect();
		}
		
		// print identification string
		System.out.println("Version of firmware is :  " + version);
	}
	
	private static void readVersionNew() {
		String version = "";

		connection.write(REQUESTVERSION);
		connection.get(buffer);
		if(buffer[1] != STARTCOMMUNICATION){
			logger.info("No data available message...!");
			return;
		}
		
		connection.write(ESCAPE);
		connection.get(buffer);
		
		
		// print identification string
		System.out.println("Version of firmware is :  " + version);
	}

	
	/**
	 * This method looks up all files in resource and List of Request objects
	 * into xml file
	 * 
	 * @return true if heat pump configuration for version could be found and
	 *         loaded
	 */
	private static boolean getHeatPumpConfiguration() {
		ConfigLocator configLocator = new ConfigLocator(configFile);
		heatPumpConfiguration = configLocator.getConfig();

		if (heatPumpConfiguration != null && !heatPumpConfiguration.isEmpty()) {
			System.out.println("Loaded heat pump configuration " + configFile);
			System.out.println("Configuration file contains " + heatPumpConfiguration.size() + " requests.");
			return true;
		}
		System.err.print("Could not load heat pump configuration file for " + configFile);
		return false;
	}

}
