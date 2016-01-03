/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2013)
 */

package physical_network;

/**
 * 
 * %%%%%%%%%%%%%% DO NOT CHANGE THIS FILE %%%%%%%%%%%%%%%
 * 
 * Encapsulates the data for a network 'data frame'.
 * At the moment this just includes a payload byte array.
 * 
 * @author kevin-b
 *
 */

public class DataFrame {
	
	public final byte[] payload;
	
	public DataFrame(byte[] payload) {
		this.payload = payload;
	}
	
	public DataFrame(String payload) {
		this.payload = payload.getBytes();
	}
	
	
	public byte[] getPayload() {
		return payload;
	}
	
	public String toString() {
		return new String(payload);		
	}
}

