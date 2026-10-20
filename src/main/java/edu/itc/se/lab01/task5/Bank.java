package edu.itc.se.lab01.task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Steps 1–3: the deliberately broken bank plus a deadlock watchdog.
 * Run it, it hangs; the watchdog prints the deadlocked threads every second.
 * While it hangs:  jcmd  (or jps)  ->  jstack <pid> > dump.txt  -> copy the
 * "Found one Java-level deadlock" section into deadlock.txt.
 */
public class Bank {
    static final class Account {
        final int id; private long balance;
        Account(int id, long balance) { this.id = id; this.balance = balance; }
        long balance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    // BUG: lock order depends on the argument order → circular wait
    static void transfer(Account from, Account to, long amount) {
        synchronized (from) {
            synchronized (to) {
                if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
            }
        }
    }

    /** Step 3: daemon thread that checks for deadlocks once per second. */
    static Thread startWatchdog() {
        Thread watchdog = new Thread(() -> {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            String lastReported = null;
            while (true) {
                long[] ids = mx.findDeadlockedThreads();   // null when there is no deadlock
                if (ids != null) {
                    String names = Arrays.stream(mx.getThreadInfo(ids))
                            .filter(ti -> ti != null)
                            .map(ThreadInfo::getThreadName)
                            .sorted()
                            .collect(Collectors.joining(", "));
                    if (!names.equals(lastReported)) {      // report each deadlock once, not every second
                        System.out.println("[watchdog] DEADLOCK detected between: " + names);
                        lastReported = names;
                    }
                }
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    return;                                 // asked to stop
                }
            }
        }, "deadlock-watchdog");
        watchdog.setDaemon(true);                           // never keeps the JVM alive
        watchdog.start();
        return watchdog;
    }

    public static void main(String[] args) throws InterruptedException {
        startWatchdog();

        Account[] accounts = new Account[4];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);

        Thread[] workers = new Thread[8];
        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < 100_000; i++) {
                    Account a = accounts[rnd.nextInt(accounts.length)];
                    Account b = accounts[rnd.nextInt(accounts.length)];
                    if (a != b) transfer(a, b, rnd.nextInt(1, 50));
                }
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();
        long total = 0; for (Account a : accounts) total += a.balance();
        System.out.println("total = " + total + " (expected 4000)");   // never printed – deadlock
    }
}