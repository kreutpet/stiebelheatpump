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
import java.util.Map.Entry;
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
	private static final int MAXRETRIES = 1000;

	private static final int INPUT_BUFFER_LENGTH = 1024;
	private static byte[] buffer = new byte[INPUT_BUFFER_LENGTH];
	private static int WAITING_TIME_BETWEEN_REQUESTS = 1500;

	private static final Logger logger = LoggerFactory
			.getLogger(SerialConnector.class);

	private static void printUsage() {
		System.out
				.println("SYNOPSIS\n\torg.stiebelheatpump.Read [-b <baud_rate>] -c <config_file> <serial_port>");
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
			} else if (args[i].equals("-c")) {
				i++;
				if (i == args.length) {
					printUsage();
					System.exit(1);
				}
				configFile = args[i];
			} else {
				serialPortName = args[i];
			}
		}

		if (!getHeatPumpConfiguration()) {
			logger.error("Could not load configuration! Can't find heat pump configuration.");
			System.exit(1);
		}

		try {
			readData();
		} catch (StiebelHeatPumpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.exit(0);

	}

	private static void readData() throws StiebelHeatPumpException {
		
		connection = new SerialConnector();
		connection.connect(serialPortName);
		
		for (Request request : heatPumpConfiguration) {
			
			try {
				Thread.sleep(WAITING_TIME_BETWEEN_REQUESTS);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			logger.debug(
					"Request : Name -> {}, Description -> {} , RequestByte -> {}",
					request.getName(), request.getDescription(),
					DatatypeConverter.printHexBinary(new byte[] { request
							.getRequestByte() }));

			logger.debug("Sending start communication");
			connection.write(StiebelHeatPumpDataParser.STARTCOMMUNICATION);

			byte response;
			try {
				response = connection.get();
			} catch (Exception e) {
				logger.error("heat pump communication could not be established !");
				return;
			}

			if (response != StiebelHeatPumpDataParser.ESCAPE) {
				logger.warn("heat pump is communicating, but did not received Escape message in inital handshake!");
				return;
			}

			StiebelHeatPumpDataParser parser = new StiebelHeatPumpDataParser();
			Map<String, String> data = new HashMap<String, String>();
			short checkSum;
			byte[] responseAvailable = new byte[]{};
			byte[] requestMessage = new byte[]{
					StiebelHeatPumpDataParser.HEADERSTART,
					StiebelHeatPumpDataParser.GET,
					(byte)0x00,
					request.getRequestByte(),
					StiebelHeatPumpDataParser.ESCAPE,
					StiebelHeatPumpDataParser.END };	
			try {
				// prepare request message	
				checkSum = parser.calculateChecksum(new byte[] { request
						.getRequestByte() });
				requestMessage[2] = parser.shortToByte(checkSum)[0];
			} catch (StiebelHeatPumpException e) {				
			}
			
			boolean validData = false;
			try {
				while (!validData){
					responseAvailable = getData(requestMessage);
					responseAvailable = parser.fixDuplicatedBytes(responseAvailable);
					validData =  parser.headerCheck(responseAvailable);
					
					if (validData){
						data = parser.parseRecords(responseAvailable, request);
					}else{
						logger.debug("Sending start communication");
						connection.write(StiebelHeatPumpDataParser.STARTCOMMUNICATION);
						try {
							response = connection.get();
						} catch (Exception e) {
							logger.error("heat pump communication could not be established !");
							continue;
						}

						if (response != StiebelHeatPumpDataParser.ESCAPE) {
							logger.warn("heat pump is communicating, but did not received Escape message in inital handshake!");
							continue;
						}
					}					
				}
			} catch (StiebelHeatPumpException e) {
			}
			
			for (Entry<String, String> dataEntry : data.entrySet()){
				logger.info("Setting {} has value : {}", dataEntry.getKey(),dataEntry.getValue());
			}
		}
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
			logger.info("Configuration file contains {} requests.",
					heatPumpConfiguration.size());
			return true;
		}
		logger.error("Could not load heat pump configuration file {} ",
				configFile);
		return false;
	}

	private static byte[] getData(byte[] request) {

		buffer = new byte[INPUT_BUFFER_LENGTH];
		byte singleByte;
		int numBytesReadTotal = 0;
		boolean dataAvailable = false;
		int requestRetry = 0;
		int retry = 0;
		while(!dataAvailable & requestRetry < MAXRETRIES/100){
			connection.write(request);
			retry = 0;
			while (!dataAvailable & retry < MAXRETRIES) {
				try {
					singleByte = connection.get();
				} catch (Exception e) {
					retry++;
					continue;
				}

				buffer[numBytesReadTotal] = singleByte;
				numBytesReadTotal++;

				if (buffer[0] == StiebelHeatPumpDataParser.DATAAVAILABLE[0]
						&& buffer[1] == StiebelHeatPumpDataParser.DATAAVAILABLE[1]) {
					// found response from heat pump that data are available
					dataAvailable = true;
					break;
				}
			}
			
			if (!dataAvailable && buffer[0] == StiebelHeatPumpDataParser.DATAAVAILABLE[0]) {
				logger.warn("heat pump not ready to send data ! resend request.");
				requestRetry++;
				connection.disconnect();
				connection.connect(serialPortName);
			}
		}
		

		if (!dataAvailable) {
			logger.warn("heat pump has no data available for request!");
			return new byte[] {};
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
			numBytesReadTotal++;

			if (numBytesReadTotal > 4
					&& buffer[numBytesReadTotal - 2] == StiebelHeatPumpDataParser.ESCAPE
					&& buffer[numBytesReadTotal - 1] == StiebelHeatPumpDataParser.END) {
				// we have reached the end of the response
				endOfMessage = true;
				logger.debug("reached end of response message.");
				break;
			}
		}

		byte[] responseBuffer = new byte[numBytesReadTotal];
		System.arraycopy(buffer, 0, responseBuffer, 0, numBytesReadTotal);
		return responseBuffer;
	}
}
