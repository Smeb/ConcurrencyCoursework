/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2013)
 */

package physical_network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 
 * %%%%%%%%%%%%%%%% YOU MAY NEED TO MODIFY THIS %%%%%%%%%%%%%%%%%%
 * 
 * Concrete implementation of the Twisted Wire Pair.
 *
 * This implementation will simply ADD TOGETHER all current voltages set
 * by different devices attached to the wire.
 * 
 * Thus you may have "Network Card A" device setting voltages to transfer bits
 * across the wire and at the same time a "Thermal Noise" device which
 * is setting random voltages on the wire. These voltages should then
 * be added together so that getVoltage() returns the sum of voltages
 * at any particular time.
 * 
 * Similarly any number of network cards may be attached to the wire and
 * each be setting voltages ... the wire should add all these voltages together.
 * 
 * @author K. Bryson
 */
class MyTwistedWirePair implements TwistedWirePair {

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    final ConcurrentHashMap<String, Double> currentVoltages = new ConcurrentHashMap<String, Double>();

    volatile double voltage = 0.0;


    public void setVoltage(String device, double voltage) {
        currentVoltages.put(device, voltage);
        updateWireVoltage();
    }

    /*
     * Update current voltage on the wire.
     */
    private void updateWireVoltage(){
    	
        double tempVoltage = 0.0;
        for (double currentVoltage: currentVoltages.values()) {
        	tempVoltage += currentVoltage;
        }
        lock.writeLock().lock();
        try {
            voltage = tempVoltage;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getVoltage(String device) {
        lock.readLock().lock();
        try {
            return voltage;
        } finally {
            lock.readLock().unlock();
        }

    }
}
