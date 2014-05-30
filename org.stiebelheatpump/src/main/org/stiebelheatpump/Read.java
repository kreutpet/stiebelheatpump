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
import org.stiebelheatpump.internal.StiebelHeatPumpException;
import org.stiebelheatpump.protocol.Request;
import org.stiebelheatpump.protocol.SerialConnector;
import org.stiebelheatpump.protocol.StiebelHeatPumpDataParser;

public class Read {

	private static String configFile;
	private static int baudRate = 9600;
	private static List<Request> heatPumpConfiguration;
	private static SerialConnector connection;
 
    private static final int WAIT_MS = 100;
	private static String serialPortName = "";	
	private static final int MAXRETRIES = 2;
	
	private static final int INPUT_BUFFER_LENGTH = 16;
	private static byte[] buffer = new byte[INPUT_BUFFER_LENGTH];
		
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
			logger.error("Could not load configuration! Can't find heat pump configuration.");
			System.exit(1);
		}
		
		readVersionNew();

		System.exit(0);
		
	}

	private static void readVersionNew() {
		Request versionRequest = null;

		for (Request request : heatPumpConfiguration) {
			logger.debug(
					"Request : Name -> {}, Description -> {} , RequestByte -> {}",
					request.getName(), request.getDescription(),
					DatatypeConverter.printHexBinary(new byte[] { request
							.getRequestByte() }));
			if (request.getName().equalsIgnoreCase("Version")) {
				versionRequest = request;
				logger.debug("Loaded Request : "
						+ versionRequest.getDescription());
			}
		}

		connection = new SerialConnector();
		connection.connect(serialPortName);

		String version = "";

		connection.write(StiebelHeatPumpDataParser.STARTCOMMUNICATION);
		logger.debug("Sending start communication");
		
		byte response;
		try {
			response = connection.get();
		} catch (Exception e) {
			logger.error("heat pump communication could not be established !");
			return;
		}
		
		if(response != StiebelHeatPumpDataParser.ESCAPE){
			logger.warn("heat pump is communicating, but did not received Escape message in inital handshake!");
			return;
		}
		
		StiebelHeatPumpDataParser parser = new StiebelHeatPumpDataParser();
		Map<String, String> data = new HashMap<String, String>();
		// prepare request message 
		short checkSum;
		try {
			checkSum = parser.calculateChecksum(
					new byte[] { versionRequest.getRequestByte()});
			byte[] requestMessage = { 
					StiebelHeatPumpDataParser.HEADERSTART,
					StiebelHeatPumpDataParser.GET,
					parser.shortToByte(checkSum)[0], 
					versionRequest.getRequestByte(),
					StiebelHeatPumpDataParser.ESCAPE,
					StiebelHeatPumpDataParser.END };

			byte[] responseAvailable =  getData(requestMessage);
			
		} catch (StiebelHeatPumpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// print identification string
		logger.info("Version of firmware is : {}" , version);
		
		connection.disconnect();
		connection = null;

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
			logger.info("Loaded heat pump configuration {} ", configFile);
			logger.info("Configuration file contains {} requests.",  heatPumpConfiguration.size());
			return true;
		}
		logger.error("Could not load heat pump configuration file {} ", configFile);
		return false;
	}

    private static byte[] getData(byte[] request) {
    	
    	buffer = new byte[INPUT_BUFFER_LENGTH];
    	byte singleByte ;
    	int retry = 0;
		int numBytesReadTotal = 0;
		boolean dataAvailable = false;

		connection.write(request);
		
    	while (!dataAvailable & retry < MAXRETRIES ) {	
    		try {
				singleByte = connection.get();
			} catch (Exception e) {
				// reconnect and try again to send request
				//connection.disconnect();
				//connection.connect(serialPortName,baudRate);
				//connection.write(request);
				
				retry++;
				continue;
			}
			
    		buffer[numBytesReadTotal] = singleByte;
			numBytesReadTotal ++;
    		
			if (buffer[0] == StiebelHeatPumpDataParser.DATAAVAILABLE[0] &&
					buffer[1] == StiebelHeatPumpDataParser.DATAAVAILABLE[1]){				
				// found response from heat pump that data are available
				dataAvailable = true;
				break;
			}
    	}   	
    	
    	if(!dataAvailable){
    		logger.warn("heat pump has no data available for request!");
    		return new byte[]{};
    	}
    	
    	// Acknowledge sending data
    	connection.write(StiebelHeatPumpDataParser.ESCAPE);
    	
    	// receive data
    	buffer = new byte[INPUT_BUFFER_LENGTH];
    	retry = 0;
		numBytesReadTotal = 0;
    	boolean endOfMessage = false;
    	 
		while (!endOfMessage & retry < MAXRETRIES) {		
    		try {
				singleByte = connection.get();
			} catch (Exception e) {
				// reconnect and try again to send request
				retry++;
				continue;
			}
			
    		buffer[numBytesReadTotal] = singleByte;
			numBytesReadTotal ++;
			
			if (numBytesReadTotal > 4 && 
				buffer[numBytesReadTotal-2] == StiebelHeatPumpDataParser.ESCAPE && 
				buffer[numBytesReadTotal-1] == StiebelHeatPumpDataParser.END) {
				// we have reached the end of the response
				endOfMessage = true;
				break;
			}
		}
		
		byte[] responseBuffer = new byte[numBytesReadTotal];
		System.arraycopy(buffer, 0, responseBuffer, 0, numBytesReadTotal);
        return responseBuffer;
    }
}
