package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * Step 4 – Fix A: global lock ordering.
 * Every thread locks the account with the smaller id first, so no thread can
 * ever hold the "higher" lock while waiting for a "lower" one. That removes
 * the circular-wait condition, so a deadlock is impossible.
 */
public class OrderedBank {

    static void transfer(Bank.Account from, Bank.Account to, long amount) {
        Bank.Account first  = from.id < to.id ? from : to;
        Bank.Account second = from.id < to.id ? to   : from;
        synchronized (first) {
            synchronized (second) {
                if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Bank.startWatchdog();   // stays silent: proves nothing deadlocks

        final int threads = 8;
        final int perThread = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        Bank.Account[] accounts = new Bank.Account[4];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Bank.Account(i, 1_000);

        LongAdder transfers = new LongAdder();
        Thread[] workers = new Thread[threads];
        long start = System.nanoTime();
        for (int w = 0; w < threads; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                int done = 0;
                while (done < perThread) {
                    Bank.Account a = accounts[rnd.nextInt(accounts.length)];
                    Bank.Account b = accounts[rnd.nextInt(accounts.length)];
                    if (a == b) continue;               // only count real transfers
                    transfer(a, b, rnd.nextInt(1, 50));
                    done++;
                }
                transfers.add(done);
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();
        long ms = (System.nanoTime() - start) / 1_000_000;

        long total = 0; for (Bank.Account a : accounts) total += a.balance();
        System.out.printf("transfers=%d total=%d (expected 4000) in %d ms%n",
                transfers.sum(), total, ms);
        if (total != 4_000) throw new AssertionError("money was created or destroyed!");
    }
}