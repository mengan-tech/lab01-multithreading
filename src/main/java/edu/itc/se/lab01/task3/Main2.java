package edu.itc.se.lab01.task3;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** Step 5: same test as Main, but with Java's ArrayBlockingQueue instead of our BoundedBuffer. */
public class Main2 {
    public static void main(String[] args) throws InterruptedException {
        final int CONSUMERS = 3;
        final int ORDERS_PER_PRODUCER = 50;

        // The ONLY real difference from Main: which buffer we plug in
        OrderQueue<Order> b = new BlockingQueueAdapter<>(new ArrayBlockingQueue<>(5));
        AtomicInteger processed = new AtomicInteger(0);

        // create 2 producers and 3 consumers (same classes as Main)
        Producer p1 = new Producer(1,b, ORDERS_PER_PRODUCER, CONSUMERS);
        Producer p2 = new Producer(51,b, ORDERS_PER_PRODUCER, CONSUMERS);

        Consumer c1 = new Consumer(1, b, processed);
        Consumer c2 = new Consumer(2, b, processed);
        Consumer c3 = new Consumer(3, b, processed);

        // put them in threads
        Thread pt1 = new Thread(p1);
        Thread pt2 = new Thread(p2);
        Thread ct1 = new Thread(c1);
        Thread ct2 = new Thread(c2);
        Thread ct3 = new Thread(c3);

        // start everything
        ct1.start();
        ct2.start();
        ct3.start();
        pt1.start();
        pt2.start();

        // wait for producers to hand in all orders
        pt1.join();
        pt2.join();

        // one poison pill per consumer
        for (int i = 0; i < CONSUMERS; i++) {
            b.put(new Order(-1, "STOP", 0));
        }

        // wait for consumers to finish
        ct1.join();
        ct2.join();
        ct3.join();

        // check the total
        int total = processed.get();
        System.out.println("Total processed: " + total);

        if (total != 2 * ORDERS_PER_PRODUCER) {
            throw new AssertionError("Expected 100 but got " + total);
        }
        System.out.println("OK - 100 orders processed (ArrayBlockingQueue).");
    }
}