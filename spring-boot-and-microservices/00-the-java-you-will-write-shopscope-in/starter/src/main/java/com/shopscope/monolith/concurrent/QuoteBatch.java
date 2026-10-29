package com.shopscope.monolith.concurrent;

import com.shopscope.monolith.order.Basket;
import com.shopscope.monolith.pricing.PriceContext;
import com.shopscope.monolith.pricing.PricingService;
import com.shopscope.monolith.pricing.Quote;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Quote many baskets at once: one task per basket, submitted to an executor, results collected from
 * futures in order. The executor is a pool of platform threads here, which is what Java 17 has.
 *
 * <p>On Java 21 and later the only change is the executor:
 * {@code Executors.newVirtualThreadPerTaskExecutor()}, one virtual thread per basket, as cheap as an
 * object, and the pool size stops being a decision. The code around it is the same.
 */
public final class QuoteBatch {

    private static final int POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());

    private QuoteBatch() {
    }

    public static List<Quote> quoteAll(PricingService pricing, List<Basket> baskets, PriceContext context) {
        // TODO(lab-7.1) one Callable<Quote> per basket, invokeAll on a fixed pool of POOL_SIZE threads,
        // join the futures in order, and shut the executor down in a finally block.
        // For now: sequential, one basket after another.
        return baskets.stream().map(basket -> pricing.quote(basket, context)).toList();
    }

    private static Quote join(Future<Quote> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while quoting", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("A quote failed", e.getCause());
        }
    }
}
