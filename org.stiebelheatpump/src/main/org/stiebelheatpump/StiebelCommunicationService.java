/**
 * Copyright 2014 
 * This file is part of stiebel heat pump reader.
 * It is free software: you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or (at your option) any later version.
 * It is  is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with the project. 
 * If not, see http://www.gnu.org/licenses/.
 */
package org.stiebelheatpump;

import java.util.*;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stiebelheatpump.internal.ConfigLocator;
import org.stiebelheatpump.internal.StiebelHeatPumpException;
import org.stiebelheatpump.protocol.DataParser;
import org.stiebelheatpump.protocol.ProtocolConnector;
import org.stiebelheatpump.protocol.RecordDefinition;
import org.stiebelheatpump.protocol.RecordDefinition.Type;
import org.stiebelheatpump.protocol.Request;
import org.stiebelheatpump.protocol.SerialConnector;

public class StiebelCommunicationService {

	private static ProtocolConnector connector;
	private String serialPortName;
	private static final int MAXRETRIES = 1000;
	private final int INPUT_BUFFER_LENGTH = 1024;
	private byte buffer[] = new byte[INPUT_BUFFER_LENGTH];

	private int WAITING_TIME_BETWEEN_REQUESTS = 1500;

	/** heat pump request definition */
	private List<Request> heatPumpConfiguration = new ArrayList<Request>();
	private List<Request> heatPumpSensorConfiguration = new ArrayList<Request>();
	private List<Request> heatPumpSettingConfiguration = new ArrayList<Request>();
	private List<Request> heatPumpStatusConfiguration = new ArrayList<Request>();
	Request versionRequest;
	DataParser parser = new DataParser();
	private static final Logger logger = LoggerFactory
			.getLogger(StiebelCommunicationService.class);

	public StiebelCommunicationService() {
	}

	public StiebelCommunicationService(String serialPortName, int baudRate,
			String configurationFile) throws StiebelHeatPumpException {
		this.serialPortName = serialPortName;

		if (!getHeatPumpConfiguration(configurationFile)) {
			throw new StiebelHeatPumpException(
					"could not read configuraton file");
		} else {
			connector = getStiebelHeatPumpConnector();
			connector.connect(serialPortName, baudRate);
			return;
		}
	}

	public void finalizer() {
		logger.info("Disconnecting heat pump.");
		connector.disconnect();
		logger.info("Heat pump disconnected.");

	}

	/**
	 * This method looks up all files in resource and List of Request objects
	 * into xml file
	 * 
	 * @return true if heat pump configuration for version could be found and
	 *         loaded
	 */
	public boolean getHeatPumpConfiguration(String version) {
		ConfigLocator configLocator = new ConfigLocator(version);
		heatPumpConfiguration = configLocator.getConfig();

		if (heatPumpConfiguration != null && !heatPumpConfiguration.isEmpty()) {
			logger.info("Loaded heat pump configuration {}.xml .", version);
			logger.info("Configuration file contains {} requests.",
					heatPumpConfiguration.size());

			logger.debug("Loading heat pump configuration ...");

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
					continue;
				}

				for (RecordDefinition record : request.getRecordDefinitions()) {
					if (record.getDataType() == Type.Settings
							&& !heatPumpSettingConfiguration.contains(request)) {
						heatPumpSettingConfiguration.add(request);
					}
					if (record.getDataType() == Type.Status
							&& !heatPumpStatusConfiguration.contains(request)) {
						heatPumpStatusConfiguration.add(request);
					}
					if (record.getDataType() == Type.Sensor
							&& !heatPumpSensorConfiguration.contains(request)) {
						heatPumpSensorConfiguration.add(request);
					}
				}
			}

			if (versionRequest == null) {
				logger.debug("version request could not be found in configuration");
				return false;
			}
			return true;
		}
		logger.warn("Could not load heat pump configuration file for {}!",
				version);
		return false;
	}

	public String getversion() throws StiebelHeatPumpException {
		String version = "";
		try {
			Map<String, String> data = readData(versionRequest);
			version = (String) data.get("Version");
			Thread.sleep(WAITING_TIME_BETWEEN_REQUESTS);
		} catch (InterruptedException e) {
			throw new StiebelHeatPumpException(e.toString());
		}
		return version;
	}

	public Map<String, String> getSettings() throws StiebelHeatPumpException {
		Map<String, String> data = new HashMap<String, String>();
		for (Request request : heatPumpSettingConfiguration) {
			logger.debug("Loading data for request {} ...", request.getName());
			try {
				Map<String, String> newData = readData(request);
				data.putAll(newData);
				Thread.sleep(WAITING_TIME_BETWEEN_REQUESTS);
			} catch (InterruptedException e) {
				throw new StiebelHeatPumpException(e.toString());
			}
		}
		return data;
	}

	public Map<String, String> getSensors() throws StiebelHeatPumpException {
		Map<String, String> data = new HashMap<String, String>();
		for (Request request : heatPumpSensorConfiguration) {
			logger.debug("Loading data for request {} ...", request.getName());
			try {
				Map<String, String> newData = readData(request);
				data.putAll(newData);
				Thread.sleep(WAITING_TIME_BETWEEN_REQUESTS);
			} catch (InterruptedException e) {
				throw new StiebelHeatPumpException(e.toString());
			}
		}
		return data;
	}

	public Map<String, String> getStatus() throws StiebelHeatPumpException {
		Map<String, String> data = new HashMap<String, String>();
		for (Request request : heatPumpStatusConfiguration) {
			logger.debug("Loading data for request {} ...", request.getName());
			try {
				Map<String, String> newData = readData(request);
				data.putAll(newData);
				Thread.sleep(WAITING_TIME_BETWEEN_REQUESTS);
			} catch (InterruptedException e) {
				throw new StiebelHeatPumpException(e.toString());
			}
		}
		return data;
	}

	public Map<String, String> readData(Request request)
			throws StiebelHeatPumpException {
		Map<String, String> data = new HashMap<String, String>();
		logger.debug(
				"Request : Name -> {}, Description -> {} , RequestByte -> {}",
				request.getName(), request.getDescription(),
				DatatypeConverter.printHexBinary(new byte[] { request
						.getRequestByte() }));
		logger.debug("Sending start communication");
		byte response;
		try {
			connector.write(DataParser.STARTCOMMUNICATION);
			response = connector.get();
		} catch (Exception e) {
			logger.error("heat pump communication could not be established !");
			throw new StiebelHeatPumpException(
					"heat pump communication could not be established !");
		}
		if (response != DataParser.ESCAPE) {
			logger.warn("heat pump is communicating, but did not received Escape message in inital handshake!");
			throw new StiebelHeatPumpException(
					"heat pump is communicating, but did not received Escape message in inital handshake!");
		}
		byte responseAvailable[] = new byte[0];
		byte requestMessage[] = createRequestMessage(request);
		boolean validData = false;
		try {
			while (!validData) {
				responseAvailable = getData(requestMessage);
				responseAvailable = parser
						.fixDuplicatedBytes(responseAvailable);
				validData = parser.headerCheck(responseAvailable);
				if (validData) {
					data = parser.parseRecords(responseAvailable, request);
					continue;
				}
				logger.debug("Sending start communication");
				try {
					connector.write(DataParser.STARTCOMMUNICATION);
					response = connector.get();
				} catch (Exception e) {
					logger.error("heat pump communication could not be established !");
					continue;
				}
				if (response != DataParser.ESCAPE)
					logger.warn("heat pump is communicating, but did not received Escape message in inital handshake!");
			}
		} catch (StiebelHeatPumpException e) {
			logger.error("Error reading data : {}", e.toString());
		}
		return data;
	}

	private byte[] getData(byte request[]) {
		buffer = new byte[INPUT_BUFFER_LENGTH];
		int numBytesReadTotal = 0;
		boolean dataAvailable = false;
		int requestRetry = 0;
		int retry = 0;
		try {
			while ((!dataAvailable) & (requestRetry < 10)) {
				connector.write(request);
				retry = 0;
				byte singleByte;
				while ((!dataAvailable) & (retry < MAXRETRIES)) {
					try {
						singleByte = connector.get();
					} catch (Exception e) {
						retry++;
						continue;
					}
					buffer[numBytesReadTotal] = singleByte;
					numBytesReadTotal++;
					if (buffer[0] != DataParser.DATAAVAILABLE[0]
							|| buffer[1] != DataParser.DATAAVAILABLE[1]) {
						continue;
					}
					dataAvailable = true;
					break;
				}
			}
			if (!dataAvailable) {
				logger.warn("heat pump has no data available for request!");
				return new byte[0];
			}
			connector.write(DataParser.ESCAPE);
		} catch (Exception e1) {
			logger.error("Could not get data from heat pump! {}", e1.toString());
			return buffer;
		}
		buffer = new byte[INPUT_BUFFER_LENGTH];
		boolean endOfMessage = false;
		numBytesReadTotal = 0;
		while ((!endOfMessage) & (retry < MAXRETRIES)) {
			byte singleByte;
			try {
				singleByte = connector.get();
			} catch (Exception e) {
				retry++;
				continue;
			}
			buffer[numBytesReadTotal] = singleByte;
			if (++numBytesReadTotal <= 4
					|| buffer[numBytesReadTotal - 2] != DataParser.ESCAPE
					|| buffer[numBytesReadTotal - 1] != DataParser.END)
				continue;
			endOfMessage = true;
			logger.debug("reached end of response message.");
			break;
		}
		byte responseBuffer[] = new byte[numBytesReadTotal];
		System.arraycopy(buffer, 0, responseBuffer, 0, numBytesReadTotal);
		return responseBuffer;
	}

	private byte[] createRequestMessage(Request request) {
		byte requestMessage[] = { DataParser.HEADERSTART, DataParser.GET, 0,
				request.getRequestByte(), DataParser.ESCAPE, DataParser.END };
		try {
			short checkSum = parser.calculateChecksum(requestMessage);
			requestMessage[2] = parser.shortToByte(checkSum)[0];
			requestMessage = parser.addDuplicatedBytes(requestMessage);
		} catch (StiebelHeatPumpException stiebelheatpumpexception) {
		}
		return requestMessage;
	}

	private ProtocolConnector getStiebelHeatPumpConnector() {
		if (connector != null)
			return connector;
		if (serialPortName != null)
			connector = new SerialConnector();
		return connector;
	}
}
