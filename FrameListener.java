/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2013)
 */

package physical_network;

/**
 * 
 * %%%%%%%%%%%%%% DO NOT CHANGE THIS FILE %%%%%%%%%%%%%%%
 * 
 * Interface supported by a listener object which responds
 * to data frames being received by a network card.
 * 
 * @author kevin-b
 *
 */

public interface FrameListener {
	
	public void receive(DataFrame frame);

}
