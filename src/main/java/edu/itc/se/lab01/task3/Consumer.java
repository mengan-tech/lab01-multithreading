package edu.itc.se.lab01.task3;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


public class Consumer implements Runnable {
    private final int id;                       // consumer number, used in the printout
    private final OrderQueue<Order> queue;      // the shared buffer
    private final AtomicInteger processed;      // shared counter of processed orders

    public Consumer(int id, OrderQueue<Order> queue, AtomicInteger processed) {
        this.id = id;
        this.queue = queue;
        this.processed = processed;
    }

    @Override
    public void run() {
        Random r = new Random();
        try {
            while (true) {
                Order order = queue.take();     // waits here if the buffer is empty

                if (order.id() == -1) { 
                    System.out.println("Consumer-" + id + "received poison pill, stopping");        // poison pill -> stop
                    break;
                }

                // "process" the order: pretend to work for 5-30 ms
                Thread.sleep(r.nextInt(5, 31));

                processed.incrementAndGet();    // +1, thread-safe
                System.out.println("consumer-" + id + " processed " + order);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupt flag and exit
        }
    }
}