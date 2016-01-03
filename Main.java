/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2013)
 */
package physical_network;

/**
 * 
 * %%%%%%%%%%%%%% DO NOT CHANGE THIS FILE %%%%%%%%%%%%%%%
 * (Unless for testing purposes ... but the system should work
 *  without any modifications to this file ...)
 * 
 * This is a test which joins two network cards together with a wire pair.
 * It then sends a data frame across the network from Network Card A to
 * Network Card B which has a simple listener which prints out the it
 * has received the data frame.
 * 
 * An oscilloscope is also connected to the wire to allow the voltage levels
 * to be monitored over time.
 * 
 * A source for thermal noise can also be connected to the wire which simulates
 * noise on the network to see how robust the transmission process is to noise.
 * 
 * @author kevin-b
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {

    	// Shared twisted pair wire.
        TwistedWirePair wire = new MyTwistedWirePair();

        // Set network card A running connected to the shared wire.
        NetworkCard networkCardA = new NetworkCard("Network Card A", wire, null);
        networkCardA.start();
        
        // Set network card B running with a simple data frame listener registered.
        NetworkCard networkCardB = new NetworkCard("Network Card B", wire, new MyFrameListener());
        networkCardB.start();

        // Currently noise level is set to 0.0 volts on wire - but this can be increased.
        ThermalNoise thermalNoise = new ThermalNoise("Thermal Noise", 0.5, wire);
        thermalNoise.start();

        // Set oscilloscope monitoring the wire voltage.
        Oscilloscope oscilloscope = new Oscilloscope("Oscilloscope", wire);
        oscilloscope.start();

        // Send a data frame across the link from network card A to network card B.
        DataFrame myMessage = new DataFrame("Hello World");
        System.out.println("SENDING DATA FRAME: " + myMessage);
        networkCardA.send(myMessage);
        
    }
}
