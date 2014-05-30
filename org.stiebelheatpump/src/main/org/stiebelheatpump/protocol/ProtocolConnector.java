package org.stiebelheatpump.protocol;

public interface ProtocolConnector {

	
    void connect(String device);
        
	void disconnect();

    byte get() throws Exception;

    short getShort() throws Exception;

    void get(byte[] data) throws Exception;

    void mark();

    void reset();

    void write(byte[] data);

	void write(byte data);

}