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
package org.stiebelheatpump.protocol;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Record definition class for Stiebel heat pump requests.
 * 
 * @author Peter Kreutzer
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class RecordDefinition {

	public static enum Type {
		Sensor, Status, Settings;
	}

	@XmlAttribute(required = true)
	private String name;

	@XmlAttribute(required = true)
	private int position;

	@XmlAttribute(required = true)
	private int length;

	@XmlAttribute(required = true)
	private double scale;

	@XmlAttribute(required = true)
	private Type dataType;

	@XmlAttribute(required = false)
	private int min;

	@XmlAttribute(required = false)
	private int max;

	@XmlAttribute(required = false)
	private double step;

	@XmlAttribute(required = false)
	private String unit;

	@XmlAttribute(required = false)
	private int bitPosition;

	public RecordDefinition() {
	}

	/**
	 * Constructor of record definition used for status and sensor values
	 * 
	 * @param name
	 *            of record
	 * @param position
	 *            of the value in the byte array
	 * @param lenght
	 *            of byte representing the value
	 * @param scale
	 *            to apply to the byte value
	 * @param dataType
	 *            of the record, see enums
	 * @param unit
	 *            of the value
	 */
	public RecordDefinition(String name, int position, int lenght,
			double scale, Type dataType, String unit) {
		this.name = name;
		this.position = position;
		this.length = lenght;
		this.scale = scale;
		this.dataType = dataType;
		this.unit = unit;
	}

	/**
	 * Constructor of record definition used for setting programs with week days
	 * encoding
	 * 
	 * @param name
	 *            of record
	 * @param position
	 *            of the value in the byte array
	 * @param lenght
	 *            of byte representing the value
	 * @param scale
	 *            to apply to the byte value
	 * @param dataType
	 *            of the record, see enums
	 * @param min
	 *            values for a setting
	 * @param max
	 *            values for a setting
	 * @param step
	 *            in which setting can be changed
	 * @param bitPosition
	 *            of the bit in the byte representing the value
	 * @param unit
	 *            of the value
	 */
	public RecordDefinition(String name, int position, int lenght,
			double scale, Type dataType, int min, int max, double step,
			int bitPosition, String unit) {
		this.name = name;
		this.position = position;
		this.length = lenght;
		this.scale = scale;
		this.dataType = dataType;
		this.min = min;
		this.max = max;
		this.step = step;
		this.bitPosition = bitPosition;
		this.unit = unit;
	}

	/**
	 * Constructor of record definition used for settings that can be changed
	 * 
	 * @param name
	 *            of record
	 * @param position
	 *            of the value in the byte array
	 * @param lenght
	 *            of byte representing the value
	 * @param scale
	 *            to apply to the byte value
	 * @param dataType
	 *            of the record, see enums
	 * @param min
	 *            values for a setting
	 * @param max
	 *            values for a setting
	 * @param step
	 *            in which setting can be changed
	 * @param unit
	 *            of the value
	 */
	public RecordDefinition(String name, int position, int lenght,
			double scale, Type dataType, int min, int max, double step,
			String unit) {
		this.name = name;
		this.position = position;
		this.length = lenght;
		this.scale = scale;
		this.dataType = dataType;
		this.min = min;
		this.max = max;
		this.step = step;
		this.unit = unit;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public Type getDataType() {
		return dataType;
	}

	public void setDataType(Type dataType) {
		this.dataType = dataType;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public double getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public int getBitPosition() {
		return bitPosition;
	}

	public void setBitPosition(int bitPosition) {
		this.bitPosition = bitPosition;
	}
}
