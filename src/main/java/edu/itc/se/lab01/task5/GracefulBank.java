package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Step 6 – graceful shutdown with cooperative cancellation.
 *
 * Workers loop while (!Thread.currentThread().isInterrupted()). After 2 s main
 * interrupts every worker, joins each with a 1 s timeout, and prints how many
 * transfers each one completed plus the final total balance.
 *
 * Reuses TryLockBank.Account (one ReentrantLock per account). Locks are taken
 * in ascending id order (Fix A, so no deadlock) with lockInterruptibly(), a
 * blocking call that throws InterruptedException. When it does, the interrupt
 * flag has been CLEARED, so the worker must restore it, otherwise the
 * while-condition would never see the interrupt and the worker would never stop.
 */
public class GracefulBank {

    static void transfer(TryLockBank.Account from, TryLockBank.Account to, long amount)
            throws InterruptedException {
        TryLockBank.Account first  = from.id < to.id ? from : to;
        TryLockBank.Account second = from.id < to.id ? to   : from;
        first.lock.lockInterruptibly();
        try {
            second.lock.lockInterruptibly();
            try {
                if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        TryLockBank.Account[] accounts = new TryLockBank.Account[4];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new TryLockBank.Account(i, 1_000);

        Thread[] workers = new Thread[8];
        AtomicLongArray counts = new AtomicLongArray(workers.length);
        for (int w = 0; w < workers.length; w++) {
            final int idx = w;
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                long done = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    TryLockBank.Account a = accounts[rnd.nextInt(accounts.length)];
                    TryLockBank.Account b = accounts[rnd.nextInt(accounts.length)];
                    if (a == b) continue;
                    try {
                        transfer(a, b, rnd.nextInt(1, 50));
                        done++;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();   // restore the flag so the loop exits
                    }
                }
                counts.set(idx, done);
            }, "transfer-" + w);
            workers[w].start();
        }

        Thread.sleep(2_000);
        System.out.println("[main] interrupting " + workers.length + " workers after 2000 ms");
        for (Thread t : workers) t.interrupt();

        boolean allStopped = true;
        for (int w = 0; w < workers.length; w++) {
            Thread t = workers[w];
            t.join(1_000);                                     // wait at most 1 s per worker
            if (t.isAlive()) {
                allStopped = false;
                System.out.println(t.getName() + " did not stop in time!");
            } else {
                System.out.println(t.getName() + " stopped after " + counts.get(w) + " transfers");
            }
        }

        long total = 0; for (TryLockBank.Account a : accounts) total += a.balance();
        System.out.printf("all workers stopped=%b total=%d (expected 4000)%n", allStopped, total);
    }
}