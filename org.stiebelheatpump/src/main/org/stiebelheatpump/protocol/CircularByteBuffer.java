package org.stiebelheatpump.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircularByteBuffer {

    private static Logger logger = LoggerFactory.getLogger(CircularByteBuffer.class);

    private static final int WAIT_MS = 10;

    private int readPos = 0;

    private int writePos = 0;

    private int currentSize = 0;

    private int markedPos = 0;

    private byte[] buffer;

    private boolean running = true;

	private int retry = 5;

    public CircularByteBuffer(int size) {
        buffer = new byte[size];
    }

    public byte get() throws Exception {
        if (!waitForData()){
        	throw new Exception ("no data availabel!");
        }
        byte result;
        synchronized (buffer) {
            result = buffer[readPos];
            currentSize--;
            readPos++;
            if (readPos >= buffer.length) {
                readPos = 0;
            }
        }
        return result;
    }
    
    private boolean waitForData() {
    	int timeOut = 0;
        while (isEmpty() && timeOut < retry  &&  running  ) {
            try {
                Thread.sleep(WAIT_MS);
                timeOut ++;
            } catch (Exception e) {
                logger.error("Error while waiting for new data", e);
            }
        }
        
        if (timeOut == retry){
        	return false;
        }
        return true;
    }

    public short getShort() throws Exception {
        
    	ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(get());
        bb.put(get());
        return bb.getShort(0);       
    }

    public void get(byte[] data) throws Exception {
    	
        for (int i = 0; i < data.length; i++) {
            data[i] = get();
        }
    }

    public void put(byte b) {
        synchronized (buffer) {
            buffer[writePos] = b;
            writePos++;
            currentSize++;
            if (writePos >= buffer.length) {
                writePos = 0;
            }
        }
    }

    public void mark() {
        markedPos = readPos;
    }

    public void reset() {
        currentSize += Math.abs(readPos - markedPos);
        readPos = markedPos;
    }

    private boolean isEmpty() {
        return currentSize <= 0;
    }

    public void stop() {
        running = false;
    }

}
