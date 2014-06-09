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

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.stiebelheatpump.protocol.ProtocolConnector;
import org.stiebelheatpump.protocol.CircularByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * connector for serial port communication.
 * 
 * @author Evert van Es (originaly copied from)
 * @author Peter Kreutzer
 */
public class SerialConnector implements ProtocolConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(SerialConnector.class);

	InputStream in = null;
	DataOutputStream out = null;
	SerialPort serialPort = null;
	ByteStreamPipe byteStreamPipe = null;

	private CircularByteBuffer buffer;

	@Override
	public void connect(String device, int baudrate) {
		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier
					.getPortIdentifier(device);

			CommPort commPort = portIdentifier.open(this.getClass().getName(),
					2000);

			serialPort = (SerialPort) commPort;
			setSerialPortParameters(baudrate);

			in = serialPort.getInputStream();
			out = new DataOutputStream(serialPort.getOutputStream());

			out.flush();

			buffer = new CircularByteBuffer(Byte.MAX_VALUE * Byte.MAX_VALUE + 2
					* Byte.MAX_VALUE);
			byteStreamPipe = new ByteStreamPipe(in, buffer);
			byteStreamPipe.startTask();

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					disconnect();
				}
			});

		} catch (Exception e) {
			throw new RuntimeException("Could not init comm port", e);
		}
	}

	@Override
	public void disconnect() {
		logger.debug("Interrupt serial connection");
		byteStreamPipe.stopTask();

		logger.debug("Close serial stream");
		try {
			out.close();
			serialPort.close();
			buffer.stop();
		} catch (IOException e) {
			logger.warn("Could not fully shut down heat pump driver", e);
		}

		logger.debug("Ready");
	}

	@Override
	public byte get() throws Exception {
		return buffer.get();
	}

	@Override
	public short getShort() throws Exception {
		return buffer.getShort();
	}

	@Override
	public void get(byte[] data) throws Exception {
		buffer.get(data);
	}

	@Override
	public void mark() {
		buffer.mark();
	}

	@Override
	public void reset() {
		buffer.reset();
	}

	@Override
	public void write(byte[] data) {
		try {
			logger.debug("Send request message : {}",
					DataParser.bytesToHex(data));
			out.write(data);
			out.flush();

		} catch (IOException e) {
			throw new RuntimeException("Could not write", e);
		}
	}

	@Override
	public void write(byte data) {
		try {
			logger.debug(String.format("Send %02X", data));
			out.write(data);
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException("Could not write", e);
		}
	}

	/**
	 * Sets the serial port parameters to xxxxbps-8N1
	 * 
	 * @param baudrate
	 *            used to initialize the serial connection
	 */
	protected void setSerialPortParameters(int baudrate) throws IOException {

		try {
			// Set serial port to xxxbps-8N1
			serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException ex) {
			throw new IOException(
					"Unsupported serial port parameter for serial port");
		}
	}
}
