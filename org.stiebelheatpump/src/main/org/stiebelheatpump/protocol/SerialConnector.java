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
 * @author Evert van Es
 * @since 1.3.0
 */
public class SerialConnector implements ProtocolConnector {

    private static final Logger logger = LoggerFactory.getLogger(SerialConnector.class);

    InputStream in = null;
    DataOutputStream out = null;
    SerialPort serialPort = null;
    ByteStreamPipe byteStreamPipe = null;

    private CircularByteBuffer buffer;

    public SerialConnector() {
    }

    public void connect(String device, int baudrate) {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(device);

            CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

            serialPort = (SerialPort) commPort;
            setSerialPortParameters(baudrate);

            in = serialPort.getInputStream();
            out = new DataOutputStream(serialPort.getOutputStream());

            out.flush();

            buffer = new CircularByteBuffer(Byte.MAX_VALUE * Byte.MAX_VALUE + 2 * Byte.MAX_VALUE);
            byteStreamPipe = new ByteStreamPipe(in, buffer);
            new Thread(byteStreamPipe).start();

            //Runtime.getRuntime().addShutdownHook(new Thread() {
	        //    @Override
	        //    public void run() {
	        //    	disconnect();
	        //    }
            //});

        } catch (Exception e) {
            throw new RuntimeException("Could not init comm port", e);
        }
    }

	@Override
	public void connect(String device) {
		connect(device,9600);		
	}
    @Override
    public void disconnect() {
        logger.debug("Interrupt serial connection");
        byteStreamPipe.stop();

        logger.debug("Close serial stream");
        try {
            out.close();
            serialPort.close();
            buffer.stop();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
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
    		//for (byte abyte : data){
        	//	out.write(abyte);
    		//}
    		out.write(data);
            out.flush();
            logger.debug("Send request message : {}" , StiebelHeatPumpDataParser.bytesToHex(data));
        } catch (IOException e) {
            throw new RuntimeException("Could not write", e);
        }
    }
    
    @Override
    public void write(byte data) {
        try {
        	logger.debug("Sending byte message : {}" , data);
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
			serialPort.setSerialPortParams(baudrate,
					SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException ex) {
			throw new IOException(
					"Unsupported serial port parameter for serial port");
		}
	}
}
