
# Task 2
**Why does making only increment() synchronized but not get() still leave a bug?**

- Synchronized does two things:
    - Only one thread runs at a time and makes sure a thread see a latest value
    - If get() is not, it never takes the lock


# Task 3

**Experiment: change `notifyAll()` to `notify()` and the while to an if. Describe in README.md what breaks and why (hint: run it 20 times in a loop).**

- if instead of while:
    - meaning between the moment of a thread is woken and the moment it gets the lock back, another thread can get in first and change the buffer
    - while fixes both after every wake-up. when conditions is still false it goes back to sleep
    - notify() instead of notifyAll(): it may wake the wrong thread. producer and consumer will will on the same lock.

# Task 4
| Fixed pool | Virtual threads |
|---|---|
| Only 20 tasks run concurrently | Potentially all 500 tasks run concurrently |
| Uses 20 platform threads | Uses lightweight virtual threads |
| Remaining tasks wait in a queue | Many tasks can wait efficiently |
| Approximately 5 seconds | Often close to 200–500 ms |
| Good when limiting concurrency is important | Good for many blocking I/O tasks |
