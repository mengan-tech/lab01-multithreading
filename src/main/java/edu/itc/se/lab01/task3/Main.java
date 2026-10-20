package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;
public class Main {
    public static void main(String[] args) throws InterruptedException {
        BoundedBuffer<Order> b = new BoundedBuffer<>(5);
        AtomicInteger processed = new AtomicInteger(0);

        final int CONSUMERS = 3;
        final int ORDERS_PER_PRODUCER = 50;

        // create 2 producers and 3 consumers
        Producer p1 = new Producer(1,b,ORDERS_PER_PRODUCER,CONSUMERS);
        Producer p2 = new Producer(51,b,ORDERS_PER_PRODUCER,CONSUMERS);

        Consumer c1 = new Consumer(1, b,processed);
        Consumer c2 = new Consumer(2, b, processed);
        Consumer c3 = new Consumer(3, b, processed);
        Long start = System.nanoTime();

        // put it in a thread
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

        // join it
        pt1.join();
        pt2.join();

        // when take poison
        for (int i = 0; i < CONSUMERS; i++) {
            b.put(new Order(-1, "STOP", 0));
        }

        ct1.join();
        ct2.join();
        ct3.join();


        int total = processed.get();
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Total processed: " + total + " (expected " + (2 * ORDERS_PER_PRODUCER) + ") in " + ms + " ms");

        if (total != 2 * ORDERS_PER_PRODUCER) {
            throw new AssertionError("Expected 100 but got " + total);
        }
        System.out.println("OK - 100 orders processed.");
    }
        
}
    

