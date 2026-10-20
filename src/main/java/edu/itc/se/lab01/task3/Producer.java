package edu.itc.se.lab01.task3;

import java.util.Random;

public class Producer implements Runnable {
    private final OrderQueue<Order> queue;
    private final int count;
    private final int consumerCount;
    private final int startId;
    

    public Producer(int startId, OrderQueue<Order> queue, int count, int consumerCount) {
        this.startId = startId;
        this.queue = queue;
        this.count = count;
        this.consumerCount = consumerCount;
    }

    @Override
    public void run() {
        Random r = new Random();
        
        try {
            for (int i = 0; i < this.count; i++) {
                int id = startId + i;
                queue.put(new Order(id, "item-" + id, r.nextInt(1, 6))); // random from 1 to 5
                
                try {
                    Thread.sleep(r.nextLong(0,21));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // restore the flag and stop
        }
    }
}
