package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * I/O-bound task: pretends to download a URL by sleeping (simulated network latency).
 *
 * @param url     the "resource" to download
 * @param delayMs how long the simulated network call takes
 * @param fail    if true, the download throws {@code IOException("boom")} after the delay
 */
public record FakeDownload(String url, long delayMs, boolean fail) implements Callable<String> {

    public static final long DEFAULT_DELAY_MS = 200;

    /** The plain version from the lab spec: 200 ms, always succeeds. */
    public FakeDownload(String url) {
        this(url, DEFAULT_DELAY_MS, false);
    }

    @Override
    public String call() throws IOException, InterruptedException {
        // Thread.sleep is interruptible: future.cancel(true) wakes it with InterruptedException,
        // which is exactly what lets a cancelled task free its thread.
        Thread.sleep(delayMs);
        if (fail) throw new IOException("boom");
        return url + " -> " + url.length();
    }
}