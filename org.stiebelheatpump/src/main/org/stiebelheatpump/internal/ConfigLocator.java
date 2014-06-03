/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.stiebelheatpump.internal;

import java.util.List;

import org.stiebelheatpump.protocol.Request;

public class ConfigLocator {

	private String file;
	private ConfigParser configParser = new ConfigParser();

	public ConfigLocator() {
	}

	/**
	 * @param file
	 *            that shall be located in the resources the file shall contain
	 *            the configuration of the specific request to the firmware
	 *            version naming convention shall be "version.xml" , e.g.
	 *            2.06.xml
	 */
	public ConfigLocator(String file) {
		this.file = file;
	}

	/**
	 * Searches for the given files in the class path.
	 * 
	 * @return All found Configurations
	 */
	public List<Request> getConfig() {
		System.out.println("Loading heat pump configuration file for " + file);
		List<Request> config = configParser.parseConfig(file);
		return config;
	}
}
