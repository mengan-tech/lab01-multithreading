package edu.itc.se.lab01.task2;

import java.util.concurrent.atomic.LongAdder;

public class LongAdderCounter implements Counter {
    

    LongAdder l = new LongAdder();
    @Override 
    public void increment(){
        l.increment();
    }
    @Override
    public long get(){
        return l.sum();
    }

}
