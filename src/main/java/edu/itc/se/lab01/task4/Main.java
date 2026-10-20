package edu.itc.se.lab01.task4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class Main {

    static final long LIMIT = 4_000_000, CHUNKS = 16;
    static final int DOWNLOADS = 500;
    static final int PLATFORM_POOL_SIZE = 20;
    static final int FAIL_EVERY = 50;          // step 3: every 50th download (#50, #100, ... #500) throws
    static final int SLOW_TASK = 123;          // step 4: this download sleeps 5 s
    static final long SLOW_DELAY_MS = 5_000;
    static final long TIMEOUT_SEC = 1;         // step 4: per-future get() timeout

    public static void main(String[] args) throws Exception {
        // Step 1
        cpuBound();

        // Steps 2-4: same 500 downloads (with failures and one slow task) on both executors.
        // Step 5: both executors are closed by try-with-resources inside runDownloads().
        runDownloads("fixed(" + PLATFORM_POOL_SIZE + ") platform threads",
                () -> Executors.newFixedThreadPool(PLATFORM_POOL_SIZE));
        runDownloads("virtual threads",
                Executors::newVirtualThreadPerTaskExecutor);

        // Step 6 (bonus): java ... Main --bonus
        if (Arrays.asList(args).contains("--bonus")) completableFutureBonus();

        // Step 5: no System.exit. Every executor has been closed, so no non-daemon
        // pool threads remain and the JVM exits on its own when main returns.
    }

    // ------------------------------------------------------------------ Step 1: CPU-bound

    static void cpuBound() throws InterruptedException {
        long t0 = System.nanoTime();
        long single = PrimeCounter.countPrimes(0, LIMIT);
        System.out.printf("single-threaded: %d primes in %d ms%n", single, ms(t0));

        int threads = Runtime.getRuntime().availableProcessors();
        t0 = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<Long>> futures = new ArrayList<>();
            long chunk = LIMIT / CHUNKS;
            for (int i = 0; i < CHUNKS; i++) {
                long from = i * chunk;
                long to = (i == CHUNKS - 1) ? LIMIT : from + chunk;   // last chunk absorbs any remainder
                futures.add(pool.submit(new PrimeCounter(from, to)));
            }

            long total = 0;
            for (Future<Long> f : futures) {
                try {
                    total += f.get();
                } catch (ExecutionException e) {
                    throw new IllegalStateException("prime chunk failed", e.getCause());
                }
            }
            System.out.printf("pool (%d threads): %d primes in %d ms%n", threads, total, ms(t0));
        }
    }

    // ------------------------------------------------------------------ Steps 2-5: I/O-bound

    static List<FakeDownload> downloadTasks() {
        List<FakeDownload> tasks = new ArrayList<>(DOWNLOADS);
        for (int i = 1; i <= DOWNLOADS; i++) {
            long delay = (i == SLOW_TASK) ? SLOW_DELAY_MS : FakeDownload.DEFAULT_DELAY_MS;
            tasks.add(new FakeDownload(url(i), delay, i % FAIL_EVERY == 0));
        }
        return tasks;
    }

    static void runDownloads(String label, Supplier<ExecutorService> factory) throws InterruptedException {
        long t0 = System.nanoTime();
        Summary s;
        try (ExecutorService pool = factory.get()) {
            List<Future<String>> futures = new ArrayList<>(DOWNLOADS);
            for (FakeDownload task : downloadTasks()) futures.add(pool.submit(task));
            s = collect(futures, TIMEOUT_SEC);
        } // close() waits for running tasks; the slow one was cancelled, so this returns promptly
        System.out.printf("downloads on %-24s: %s in %d ms%n", label, s, ms(t0));
    }

    record Summary(int ok, int failed, int timedOut) {
        @Override public String toString() {
            return "ok=" + ok + " failed=" + failed + " timedOut=" + timedOut;
        }
    }

    /** Collects every future without letting one bad task crash the run. */
    static Summary collect(List<Future<String>> futures, long timeoutSec) throws InterruptedException {
        int ok = 0, failed = 0, timedOut = 0;
        for (Future<String> f : futures) {
            try {
                f.get(timeoutSec, TimeUnit.SECONDS);
                ok++;
            } catch (ExecutionException e) {
                failed++;                      // the task threw; e.getCause() is the IOException("boom")
            } catch (TimeoutException e) {
                f.cancel(true);                // stop waiting AND interrupt the sleeping task
                timedOut++;
            } catch (CancellationException e) {
                timedOut++;
            }
        }
        return new Summary(ok, failed, timedOut);
    }

    // ------------------------------------------------------------------ Step 6: bonus

    static void completableFutureBonus() throws InterruptedException {
        // Pre-JDK-19 shutdown style shown here: shutdown() + awaitTermination() + shutdownNow().
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        long t0 = System.nanoTime();
        try {
            List<CompletableFuture<String>> cfs = new ArrayList<>();
            for (int i = 1; i <= DOWNLOADS; i++) {
                FakeDownload task = new FakeDownload(url(i), FakeDownload.DEFAULT_DELAY_MS, i % FAIL_EVERY == 0);
                cfs.add(CompletableFuture
                        .supplyAsync(unchecked(task), pool)
                        .handle((result, ex) -> ex == null
                                ? result
                                : task.url() + " -> FALLBACK (" + rootCause(ex).getMessage() + ")"));
            }
            CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new)).join();

            List<String> results = cfs.stream().map(CompletableFuture::join).toList();
            long fallbacks = results.stream().filter(r -> r.contains("FALLBACK")).count();
            System.out.printf("bonus CompletableFuture                : results=%d real=%d fallback=%d in %d ms%n",
                    results.size(), results.size() - fallbacks, fallbacks, ms(t0));
        } finally {
            shutdownGracefully(pool, 5, TimeUnit.SECONDS);
        }
    }

    /** Adapts a Callable (checked exceptions) to the Supplier that supplyAsync wants. */
    static <T> Supplier<T> unchecked(Callable<T> c) {
        return () -> {
            try {
                return c.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    static Throwable rootCause(Throwable t) {
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /** The classic shutdown sequence for JDKs without AutoCloseable executors (before 19). */
    static void shutdownGracefully(ExecutorService pool, long timeout, TimeUnit unit) throws InterruptedException {
        pool.shutdown();
        if (!pool.awaitTermination(timeout, unit)) {
            pool.shutdownNow();
            if (!pool.awaitTermination(timeout, unit)) {
                System.err.println("executor did not terminate");
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    static String url(int i) {
        return "https://example.com/file-" + i + ".bin";
    }

    static long ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}