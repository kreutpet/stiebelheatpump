/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.stiebelheatpump.internal;

/**
 * Exception for Stiebel heat pump errors.
 * 
 * @author Peter Kreutzer
 * @since 1.4.0
 */
public class StiebelHeatPumpException extends Exception {

	public StiebelHeatPumpException() {
		super();
	}

	public StiebelHeatPumpException(String message) {
		super(message);
	}

	public StiebelHeatPumpException(String message, Throwable cause) {
		super(message, cause);
	}

	public StiebelHeatPumpException(Throwable cause) {
		super(cause);
	}

}
