package org.stiebelheatpump.protocol;

import java.io.IOException;
import java.io.InputStream;

import org.stiebelheatpump.protocol.CircularByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteStreamPipe implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ByteStreamPipe.class);

    private boolean running = true;
    private InputStream in = null;
    private CircularByteBuffer buffer;

    public ByteStreamPipe(InputStream in, CircularByteBuffer buffer) {
        this.in = in;
        this.buffer = buffer;
    }

    @Override
    public void run() {
        while (running) {
            try {
                byte readByte = (byte) in.read();
                logger.debug(String.format("Received %02X", readByte));
                buffer.put(readByte);
            } catch (Exception e) {
                logger.error("Error while reading from COM port. Stopping.", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        running = false;
        try {
            in.close();
        } catch (IOException e) {
            logger.error("Error while closing COM port.", e);
        }
    }

}
