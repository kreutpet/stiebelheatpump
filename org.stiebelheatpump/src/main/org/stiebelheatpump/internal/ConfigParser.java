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
package org.stiebelheatpump.internal;

import java.io.*;
import java.util.List;

import javax.xml.bind.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stiebelheatpump.protocol.Request;
import org.stiebelheatpump.protocol.Requests;

/**
 * Config parser class. This class parses the xml configuration file converts it
 * into a list of requests
 * 
 * @author Peter Kreutzer
 */
public class ConfigParser {

	private static final Logger logger = LoggerFactory
			.getLogger(ConfigParser.class);

	public ConfigParser() {
	}

	/**
	 * This method saves List of Request objects into xml file
	 * 
	 * @param requests
	 *            object to be saved
	 * @param xmlFileLocation
	 *            file object to save the object into
	 */
	@SuppressWarnings("resource")
	public void marshal(List<Request> requests, File xmlFileLocation)
			throws StiebelHeatPumpException {
		JAXBContext context;
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(xmlFileLocation));
		} catch (IOException e) {
			throw new StiebelHeatPumpException(e.toString());
		}
		try {
			context = JAXBContext.newInstance(Requests.class);
		} catch (JAXBException e) {
			throw new StiebelHeatPumpException(e.toString());
		}
		Marshaller m;
		try {
			m = context.createMarshaller();
		} catch (JAXBException e) {
			throw new StiebelHeatPumpException(e.toString());
		}
		try {
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		} catch (PropertyException e) {
			throw new StiebelHeatPumpException(e.toString());
		}
		try {
			m.marshal(new Requests(requests), writer);
		} catch (JAXBException e) {
			throw new StiebelHeatPumpException(e.toString());
		}
		try {
			writer.close();
		} catch (IOException e) {
			throw new StiebelHeatPumpException(e.toString());
		}
	}

	/**
	 * This method loads a List of Request objects from xml file
	 * 
	 * @param importFile
	 *            file object to load the object from
	 * @return List of Requests
	 */
	public List<Request> unmarshal(File importFile)
			throws StiebelHeatPumpException {
		Requests requests = new Requests();

		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Requests.class);
			Unmarshaller um = context.createUnmarshaller();
			requests = (Requests) um.unmarshal(importFile);
		} catch (JAXBException e) {
			new StiebelHeatPumpException(e.toString());
		}

		return requests.getRequests();
	}

	/**
	 * This method loads a List of Request objects from xml file
	 * 
	 * @param fileName
	 *            file object to load the object from
	 * @return List of Requests
	 */
	public List<Request> parseConfig(String fileName) {
		logger.debug("Parsing  heat pump configuration file {}.", fileName);
		try {
			JAXBContext context = JAXBContext.newInstance(Requests.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			InputStream stream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(fileName);
			Requests configuration = (Requests) unmarshaller.unmarshal(stream);
			List<Request> requests = configuration.getRequests();
			return requests;
		} catch (JAXBException e) {
			logger.debug("Parsing  failed {}. " + e.toString(), fileName);
			throw new RuntimeException(e);
		}
	}
}
