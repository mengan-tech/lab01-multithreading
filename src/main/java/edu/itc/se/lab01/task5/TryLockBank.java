package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Step 5 – Fix B: timed tryLock with back-off.
 * Locks are still taken in argument order (deadlock-prone on purpose), but no
 * thread ever waits forever: if it cannot get both locks within 10 ms it
 * releases what it holds, sleeps a random moment and retries. That breaks the
 * "hold and wait" condition.
 */
public class TryLockBank {

    static final class Account {
        final int id; private long balance;
        final ReentrantLock lock = new ReentrantLock();
        Account(int id, long balance) { this.id = id; this.balance = balance; }
        long balance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    static final LongAdder RETRIES = new LongAdder();

    /** @throws InterruptedException if interrupted while waiting for a lock or backing off */
    static void transfer(Account from, Account to, long amount) throws InterruptedException {
        var rnd = ThreadLocalRandom.current();
        while (true) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
                            return;                                   // success
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();                               // always release the first lock
                }
            }
            // failed to get both locks: count it, back off 1–5 ms at random so the two
            // threads do not collide again straight away, then try again
            RETRIES.increment();
            TimeUnit.MICROSECONDS.sleep(rnd.nextInt(1_000, 5_000));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final int threads = 8;
        final int perThread = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        Account[] accounts = new Account[4];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

        LongAdder transfers = new LongAdder();
        Thread[] workers = new Thread[threads];
        long start = System.nanoTime();
        for (int w = 0; w < threads; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                int done = 0;
                try {
                    while (done < perThread) {
                        Account a = accounts[rnd.nextInt(accounts.length)];
                        Account b = accounts[rnd.nextInt(accounts.length)];
                        if (a == b) continue;
                        transfer(a, b, rnd.nextInt(1, 50));
                        done++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    transfers.add(done);
                }
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();
        long ms = (System.nanoTime() - start) / 1_000_000;

        long total = 0; for (Account a : accounts) total += a.balance();
        System.out.printf("transfers=%d retries=%d total=%d (expected 4000) in %d ms%n",
                transfers.sum(), RETRIES.sum(), total, ms);
        if (total != 4_000) throw new AssertionError("money was created or destroyed!");
    }
}