/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2013)
 */

package physical_network;

/**
 * 
 * %%%%%%%%%%%%%% DO NOT CHANGE THIS FILE %%%%%%%%%%%%%%%
 * 
 * Simple implementation of a data frame listener which simple
 * prints to screen the contents of any data frames received.
 * 
 * @author kevin-b
 *
 */

class MyFrameListener implements FrameListener {
	
	public void receive(DataFrame frame) {
		
		System.out.println("RECEIVED DATA FRAME: \"" + frame + "\"");
		
	}

}
