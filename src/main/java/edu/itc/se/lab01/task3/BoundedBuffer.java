package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;
public class BoundedBuffer<T> implements OrderQueue<T> {
    private final ArrayDeque<T> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
    }
    @Override 
    public synchronized void put(T item) throws InterruptedException {
        while (items.size() == capacity) {   // full -> wait for a consumer to make room
            wait();
        }
        items.addLast(item);
        notifyAll();          // an item is now available
    }

    @Override 
    public synchronized T take() throws InterruptedException {
        while (items.isEmpty()) {             // empty -> wait for a producer
            wait();
        }
        T item = items.removeFirst();
        notifyAll();                          // a slot is now free
        return item;
    }

}
