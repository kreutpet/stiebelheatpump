/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.stiebelheatpump.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.stiebelheatpump.protocol.Request;
import org.stiebelheatpump.protocol.Requests;

public class ConfigParser {
	
	public ConfigParser() {
	}

	/**
	 * This method saves List of Request objects into xml file
	 * 
	 * @param requests
	 *            object to be saved
	 * @param xmlFileLocation
	 *            file object to save the object into
	 * @throws IOException 
	 * @throws JAXBException 
	 */
	public void marshal(List<Request> requests, File xmlFileLocation) throws IOException, JAXBException {
		JAXBContext context;
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(xmlFileLocation));
			context = JAXBContext.newInstance(Requests.class);
			Marshaller m;
			m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(new Requests(requests), writer);
			writer.close();
		} catch (IOException e) {
			throw new IOException(e.getMessage());
		} catch (JAXBException e) {
			throw new JAXBException(e.getMessage());
		}
	}

	/**
	 * This method loads a List of Request objects from xml file
	 * 
	 * @param importFile
	 *            file object to load the object from
	 * @return List of Requests
	 */
	public List<Request> unmarshal(File importFile) {
		Requests requests = new Requests();

		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Requests.class);
			Unmarshaller um = context.createUnmarshaller();
			requests = (Requests) um.unmarshal(importFile);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
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
		System.out.println("Parsing  heat pump configuration file " + fileName);
        try {
        	JAXBContext context = JAXBContext.newInstance(Requests.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            Requests configuration = (Requests) unmarshaller.unmarshal(stream);
            List<Request> requests = configuration.getRequests();
            return requests;
        } catch (JAXBException e) {
        	System.err.println("Parsing  failed " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
