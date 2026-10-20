package edu.itc.se.lab01.task3;

import java.util.concurrent.BlockingQueue;


public class BlockingQueueAdapter<T> implements OrderQueue<T> {
    private final BlockingQueue<T> queue;

    public BlockingQueueAdapter(BlockingQueue<T> queue) {
        this.queue = queue;
    }

    @Override
    public void put(T order) throws InterruptedException {
        queue.put(order);       // blocks when full (same as BoundedBuffer)
    }

    @Override
    public T take() throws InterruptedException {
        return queue.take();    // blocks when empty (same as BoundedBuffer)
    }
}