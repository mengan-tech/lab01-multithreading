package edu.itc.se.lab01.task4;

import java.util.concurrent.Callable;

/**
 * CPU-bound task: counts primes in the half-open range [from, to)
 * using naive trial division.
 */
public record PrimeCounter(long from, long to) implements Callable<Long> {

    @Override
    public Long call() {
        return countPrimes(from, to);
    }

    static long countPrimes(long from, long to) {
        long count = 0;
        for (long n = Math.max(from, 2); n < to; n++) {
            if (isPrime(n)) count++;
        }
        return count;
    }

    static boolean isPrime(long n) {
        if (n < 2) return false;
        for (long d = 2; d * d <= n; d++) {
            if (n % d == 0) return false;
        }
        return true;
    }
}
