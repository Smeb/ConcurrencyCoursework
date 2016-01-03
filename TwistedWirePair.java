/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2013)
 */

package physical_network;

/**
 * 
 * %%%%%%%%%%%%%% DO NOT CHANGE THIS FILE %%%%%%%%%%%%%%%
 * 
 * An interface to a Twisted Wire Pair.
 * 
 * The pair of wires can have a number of voltage sources connected to it -
 * each source can set a particular voltage across the wire pair at its connection.
 * 
 * @author kevin-b
 */
public interface TwistedWirePair {
    
    /**
     * This allows the current 'device thread' to set a particular voltage across the wires.
     * 
     * @param device  Device setting the voltage on the wire.
     * @param voltage Voltage to set across the wires.
     */
    public void setVoltage(String device, double voltage);
    
    /**
     * This returns the current voltage across the wire where the device is connected.
     * How the particular concrete implementation models the voltages is implementation
     * specific and not defined by this interface.
     * 
     * @param device The device testing the voltage on the wire.
     * @return Voltage value across wire pair where this device connects.
     */
    public double getVoltage(String device);

}
