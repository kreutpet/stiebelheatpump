/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.stiebelheatpump.protocol;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ReadVersion {

	private static void printUsage() {
		System.out.println("SYNOPSIS\n\torg.stiebelheatpump.ReadVersion [-b <baud_rate>] <serial_port>");
		System.out
				.println("DESCRIPTION\n\tReads the heat pump version connected to the given serial port and prints the received data to stdout. Errors are printed to stderr.");
		System.out.println("OPTIONS");
		System.out
				.println("\t<serial_port>\n\t    The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows)\n");
		System.out
				.println("\t-b <baud_rate>\n\t    Baud rate. Default is 9600.\n");
	}

	public static void main(String[] args) {
		if (args.length < 1 || args.length > 4) {
			printUsage();
			System.exit(1);
		}

		String serialPortName = "";
		int baudRate = 9600;
		
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
			else {
				serialPortName = args[i];
			}
		}

		Connection connection = new Connection(serialPortName, baudRate);

		try {
			connection.connect();
		} catch (IOException e) {
			System.err.println("Failed to open serial port: " + e.getMessage());
			System.exit(1);
		}

		String version = "";
		try {
			version = connection.readVersion();
		} catch (IOException e) {
			System.err.println("IOException while trying to read: " + e.getMessage());
			connection.disconnect();
			System.exit(1);
		} catch (TimeoutException e) {
			System.err.print("Read attempt timed out");
			connection.disconnect();
			System.exit(1);
		}

		// print identification string
		System.out.println("Version of firmware is :  " + version);

		connection.disconnect();

	}

}
