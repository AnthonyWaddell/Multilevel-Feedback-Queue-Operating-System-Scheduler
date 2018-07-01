
import java.util.*;

public class Scheduler extends Thread {

    // It has three queues, numbered 0 to 2
    private Vector Q0;
    private Vector Q1;
    private Vector Q2;

    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

    // New data added to p161 
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // UNCHANGED
    // A new feature added to p161 
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;

    private void initTid(int maxThreads) {
        tids = new boolean[maxThreads];
        for (int i = 0; i < maxThreads; i++) {
            tids[i] = false;
        }
    }

    // UNCHANGED
    // A new feature added to p161 
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid() {
        for (int i = 0; i < tids.length; i++) {
            int tentative = (nextId + i) % tids.length;
            if (tids[tentative] == false) {
                tids[tentative] = true;
                nextId = (tentative + 1) % tids.length;
                return tentative;
            }
        }
        return -1;
    }

    // UNCHANGED
    // A new feature added to p161 
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid(int tid) {
        if (tid >= 0 && tid < tids.length && tids[tid] == true) {
            tids[tid] = false;
            return true;
        }
        return false;
    }

    // Synchronize each of the three queues
    // A new feature added to p161 
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb() {
        // Retrieve thread from Q0
        Thread myThread = Thread.currentThread(); // Get my thread object
        synchronized (Q0) {
            for (int i = 0; i < Q0.size(); i++) {
                TCB tcb = (TCB) Q0.elementAt(i);
                Thread thread = tcb.getThread();
                if (thread == myThread) // if this is my TCB, return it
                {
                    return tcb;
                }
            }
        }
        // Repeat for Q1
        synchronized (Q1) {
            for (int i = 0; i < Q1.size(); i++) {
                TCB tcb = (TCB) Q1.elementAt(i);
                Thread thread = tcb.getThread();
                if (thread == myThread) // if this is my TCB, return it
                {
                    return tcb;
                }
            }
        }
        // Repeat for Q2
        synchronized (Q2) {
            for (int i = 0; i < Q2.size(); i++) {
                TCB tcb = (TCB) Q2.elementAt(i);
                Thread thread = tcb.getThread();
                if (thread == myThread) // if this is my TCB, return it
                {
                    return tcb;
                }
            }
        }
        return null;
    }

    // UNCHANGED 
    // A new feature added to p161 
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads() {
        return tids.length;
    }

    // Instantiate all three queues
    public Scheduler() {
        timeSlice = DEFAULT_TIME_SLICE;
        Q0 = new Vector();
        Q1 = new Vector();
        Q2 = new Vector();
        initTid(DEFAULT_MAX_THREADS);
    }

    // Instantiat eall three queues
    public Scheduler(int quantum) {
        timeSlice = quantum;
        Q0 = new Vector();
        Q1 = new Vector();
        Q2 = new Vector();
        initTid(DEFAULT_MAX_THREADS);
    }

    // Instantiate all three queues
    // A new feature added to p161 
    // A constructor to receive the max number of threads to be spawned
    public Scheduler(int quantum, int maxThreads) {
        timeSlice = quantum;
        Q0 = new Vector();
        Q1 = new Vector();
        Q2 = new Vector();
        initTid(maxThreads);
    }

    // UNCHANGED
    private void schedulerSleep() {
        try {
            Thread.sleep(timeSlice);
        } catch (InterruptedException e) {
        }
    }

    // A modified addThread of p161 example
    public TCB addThread(Thread t) {
        //  t.setPriority( 2 ); // Not using priorities in this fashion
        TCB parentTcb = getMyTcb(); // get my TCB and find my TID
        int pid = (parentTcb != null) ? parentTcb.getTid() : -1;
        int tid = getNewTid(); // get a new TID
        if (tid == -1) {
            return null;
        }
        TCB tcb = new TCB(t, tid, pid); // create a new TCB
        Q0.add(tcb); // A new threads TCB is always enqueued into Queue sub 0
        return tcb;
    }

    // UNCHANGED
    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread() {
        TCB tcb = getMyTcb();
        if (tcb != null) {
            return tcb.setTerminated();
        } else {
            return false;
        }
    }

    // UNCHANGED
    public void sleepThread(int milliseconds) {
        try {
            sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }

    // A modified run of p161
    public void run() {
        Thread current = null;

        //this.setPriority( 6 ); bollocks
        while (true) {
            while (true) {
                while (true) {
                    try {
                        // Start with highest priority queue: Q0
                        if (processQ0()) {

                            TCB currentTCB = (TCB) Q0.firstElement();
                            if (currentTCB.getTerminated() == true) {
                                Q0.remove(currentTCB);
                                returnTid(currentTCB.getTid());
                                continue;
                            }
                            current = currentTCB.getThread();
                            if (current != null) {
                                if (current.isAlive()) {
                                    current.resume(); //cur.setPriority(4); bollocks
                                } else {
                                    current.start();
                                }
                            }

                            // Execute all threads in Queue sub 0 whose time quantum is 500 ms
                            sleepThread(timeSlice / 2);

                            synchronized (Q0) {
                                if (current != null && current.isAlive()) {
                                    current.suspend(); //current.setPriority(2); bollocks
                                }
                                Q0.remove(currentTCB); // Pass TCB to Q1
                                Q1.add(currentTCB);
                            }
                            // Move to second priority queue: Q1
                        } else if (processQ1()) {
                            TCB currentTCB = (TCB) Q1.firstElement();
                            if (currentTCB.getTerminated() == true) {
                                Q1.remove(currentTCB);
                                returnTid(currentTCB.getTid());
                                continue;
                            }
                            current = currentTCB.getThread();
                            if (current != null) {
                                if (current.isAlive()) {
                                    current.resume();
                                } else {
                                    current.start();
                                }
                            }

                            // Your MFQS scheduler should execute a thread in 
                            // Queue 1 for only Q0=500ms one-half the Queue1 time
                            // quantum and then check if Queue0 has new TCBs pending
                            for (int x = 0; x < 2; x++) {
                                sleepThread(timeSlice / 2);
                                // See if Q0 has anything
                                if (processQ0()) {
                                    current.suspend();
                                    continue;
                                }
                            }
                            // If a thread in Queue1 does not complete its 
                            // execution and was given a full Queue1's time 
                            // quantum, (i.e., Q1 ), the scheduler then moves 
                            // the TCB to Queue2 
                            synchronized (Q1) {
                                if (current != null && current.isAlive()) {
                                    current.suspend();
                                }
                                Q1.remove(currentTCB); // Pass TCB to Q2
                                Q2.add(currentTCB);
                            }
                            /// Move to lowest priority queue: Q2
                        } else if (processQ2()) {
                            TCB currentTCB = (TCB) Q2.firstElement();
                            if (currentTCB.getTerminated() == true) {
                                Q2.remove(currentTCB);
                                returnTid(currentTCB.getTid());
                                continue;
                            }
                            current = currentTCB.getThread();
                            if (current != null) {
                                if (current.isAlive()) {
                                    current.resume();
                                } else {
                                    current.start();
                                }
                            }

                            // Your scheduler should execute a thread in Queue2 
                            // for Q0 increments and check if Queue0 and Queue1 
                            // have new TCBs
                            for (int x = 0; x < 4; x++) {
                                sleepThread(timeSlice / 2);
                                // See if Q0 has anything
                                if (processQ0()) {
                                    current.suspend();
                                    continue;
                                }
                                if (processQ1()) {
                                    current.suspend();
                                    continue;
                                }
                            }
                            synchronized (Q2) {
                                if (current != null && current.isAlive()) {
                                    current.suspend();
                                }
                                // If a thread in Queue2 does not complete its 
                                // execution for Queue2's time slice, (i.e., Q2)
                                // the scheduler puts it back to the tail of 
                                // Queue2.
                                Q2.remove(currentTCB);
                                Q2.add(currentTCB);
                            }
                        }
                    } catch (NullPointerException e3) {
                    }
                }
            }
        }
    }

// Utility/readbility functions
    public boolean processQ0() {
        return !(Q0.isEmpty());
    }

    public boolean processQ1() {
        return !(Q1.isEmpty());
    }

    public boolean processQ2() {
        return !(Q2.isEmpty());
    }
}
