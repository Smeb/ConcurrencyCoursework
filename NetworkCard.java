/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2013)
 */

package physical_network;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * %%%%%%%%%%%%%%%% YOU NEED TO IMPLEMENT THIS %%%%%%%%%%%%%%%%%%
 *
 * Represents a network card that can be attached to a particular wire.
 *
 * It has only two key responsibilities:
 * i) Allow the sending of data frames consisting of arrays of bytes using send() method.
 * ii) If a data frame listener is registered during construction, then any data frames
 *     received across the wire should be sent to this listener.
 *
 * @author K. Bryson
 */
public class NetworkCard extends Thread {

    static final byte SEQUENCE_CHAR = 0x7E;
    static final byte ESCAPE_CHAR = 0x7D;

    // Wire pair that the network card is attached to.
    private final TwistedWirePair wire;

    // Unique device name given to the network card.
    private final String deviceName;

    // A 'data frame listener' to call if a data frame is received.
    private final FrameListener listener;

    // Auxilary data structures to manage scheduling
    private final LinkedBlockingQueue<Double> voltageQueue;
    private final LinkedList<Byte> byteQueue;
    private final ScheduledFrame scheduledFrame;

    // Default values for high, low and mid- voltages on the wire.
    private final static double HIGH_VOLTAGE = 2.5;
    private final static double LOW_VOLTAGE = -2.5;
    private final static double ACC_V_DEVIANCE = 0.5;
    private final AtomicInteger bitCount;
    private CountDownLatch countDownLatch;

    // Default value for a signal pulse width that should be used in milliseconds.
    private final static int PULSE_WIDTH = 200;

    // Default value for sample rate, equal to number of samples taken in the pulse period
    private final static int SAMPLE_RATE = 4;

    // Default value for maximum payload size in bytes.
    private final static int MAX_PAYLOAD_SIZE = 1500;

    // Substitute names for voltage signals used in helper methods
    private final boolean UP = true;
    private final boolean DOWN = false;
    private boolean IN_FRAME;
    private byte lastByte = 0x00;

    /**
     * NetworkCard constructor.
     *
     * @param deviceName This provides the name of this device, i.e. "Network Card A".
     * @param wire       This is the shared wire that this network card is connected to.
     * @param listener   A data frame listener that should be informed when data frames are received.
     *                   (May be set to 'null' if network card should not respond to data frames.)
     */
    public NetworkCard(String deviceName, TwistedWirePair wire, FrameListener listener) {
        this.bitCount = new AtomicInteger(0);
        this.byteQueue = new LinkedList<>();
        this.deviceName = deviceName;
        this.listener = listener;
        this.voltageQueue = new LinkedBlockingQueue<Double>();
        this.wire = wire;
        this.scheduledFrame = new ScheduledFrame();
    }

    /**
     * Tell the network card to send this data frame across the wire.
     * NOTE - THIS METHOD ONLY RETURNS ONCE IT HAS SENT THE DATA FRAME.
     *
     * @param frame Data frame to send across the network.
     */
    public void send(DataFrame frame) throws InterruptedException {
        countDownLatch = new CountDownLatch(1);
        scheduledFrame.addPayload(frame.getPayload());
        countDownLatch.await();
        System.out.println("Woke up");
    }

    /*
     * If the listener is not null, the run method should "listen" to the wire,
     * receive and decode any "data frames" that are transmitted,
     * and inform the listener of any data frames received.
     */

    public void run() {
        if (listener != null) {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

            executor.scheduleAtFixedRate(new ByteEvaluator(), 0, PULSE_WIDTH * 8, TimeUnit.MILLISECONDS);
            executor.scheduleAtFixedRate(new WireMonitor(), 0, PULSE_WIDTH / SAMPLE_RATE, TimeUnit.MILLISECONDS);
        }
    }

    // Sender tasks

    private class ScheduledFrame {
        private final AtomicInteger runCount = new AtomicInteger(0);
        private final ByteManager byteMngr;
        private final ConcurrentLinkedQueue<Byte> payload;
        private final ScheduledExecutorService executor;
        private byte workingByte;

        public ScheduledFrame() {
            byteMngr = new ByteManager();
            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(byteMngr, 0, PULSE_WIDTH, TimeUnit.MILLISECONDS);
            this.payload = new ConcurrentLinkedQueue<Byte>();
        }

        // This doesn't actually need to be synchronized because of the requirement that the send
        // method of NetworkCard returns only after the DataFrame has been sent.

        synchronized void addPayload(byte[] payload){
            for(int i = 0; i < 5; i++){
                this.payload.add((byte)0x00);
            }
            this.payload.add(SEQUENCE_CHAR);
            for (byte b : payload) {
                if (b == SEQUENCE_CHAR || b == ESCAPE_CHAR) {
                    this.payload.add(ESCAPE_CHAR);
                }
                this.payload.add(b);
            }
            this.payload.add(SEQUENCE_CHAR);
        }

        private class ByteManager implements Runnable {
            public void run() {
                if(!payload.isEmpty()) {
                    if (runCount.get() == 0) {
                        workingByte = payload.element();
                    }
                    if ((workingByte & 0x80) == 0x80) {
                        wire.setVoltage(deviceName, HIGH_VOLTAGE);
                    } else {
                        wire.setVoltage(deviceName, LOW_VOLTAGE);
                    }
                    workingByte <<= 1;
                    runCount.incrementAndGet();
                    if (runCount.get() == 8) {
                        runCount.set(0);
                        payload.remove();
                    }
                }
                else{
                    if(countDownLatch != null){
                        wire.setVoltage(deviceName, 0);
                        countDownLatch.countDown();
                    }
                }
            }
        }
    }

    // Receiver tasks

    private class WireMonitor implements Runnable {
        public void run() {
            try {
                voltageQueue.put(wire.getVoltage(deviceName));
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private class ByteEvaluator implements Runnable {
        private boolean byteStuffFlag = false;

        public void run() {
            int signalDirection;
            int i = 0;
            try {
                if (!IN_FRAME) {
                    for (; i < 8; i++) {
                        signalDirection = sampleSignal();
                        processSignal(signalDirection);
                        if (lastByte == 0x7E) {
                            System.out.println("Found frame");
                            IN_FRAME = true;
                            lastByte = 0x00;
                            bitCount.set(0);
                            break;
                        }
                    }
                }
                if (IN_FRAME) {
                    for (; i < 8; i++) {
                        signalDirection = sampleSignal();
                        if (bitCount.get() == 7) {
                            processSignal(signalDirection);
                            if(lastByte == 0x7E && !byteStuffFlag){
                                IN_FRAME = false;
                                lastByte = 0x00;
                                listener.receive(new DataFrame(unpackBytes(byteQueue)));
                                byteQueue.clear();
                            }
                            else if(lastByte == 0x7D && !byteStuffFlag){
                                byteStuffFlag = true;
                            }
                            else{
                                byteQueue.add(lastByte);
                            }
                            bitCount.set(0);
                        } else {
                            processSignal(signalDirection);
                            bitCount.incrementAndGet();
                        }
                    }
                }
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void processSignal(int signal) {
        lastByte <<= 1;
        if (signal >= SAMPLE_RATE / 2) {
            lastByte |= 1;
        }
    }

    private boolean signalTypeUp(Double d) {
        if (d > (HIGH_VOLTAGE - ACC_V_DEVIANCE) && d < (HIGH_VOLTAGE + ACC_V_DEVIANCE)) {
            return UP;
        } else if (d < (LOW_VOLTAGE + ACC_V_DEVIANCE) && d > (LOW_VOLTAGE - ACC_V_DEVIANCE)) {
            return DOWN;
        }
        return DOWN;
    }

    private int sampleSignal() throws InterruptedException {
        int signalDirection = 0;
        for (int j = 0; j < SAMPLE_RATE; j++) {
            if (signalTypeUp(voltageQueue.take())) {
                signalDirection += 1;
            } else {
                signalDirection -= 1;
            }
        }
        return signalDirection;
    }

    private byte[] unpackBytes(LinkedList<Byte> byteQueue){
        int i = 0;
        byte[] resultArray = new byte[byteQueue.size()];
        for(Byte b : byteQueue){
            resultArray[i++] = b;
        }
        return resultArray;
    }
}