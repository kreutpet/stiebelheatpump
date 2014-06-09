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

/**
 * Exception for Stiebel heat pump errors.
 * 
 * @author Peter Kreutzer
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
