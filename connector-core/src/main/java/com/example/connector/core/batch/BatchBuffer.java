package com.example.connector.core.batch;

import com.example.connector.core.model.ConnectorMessage;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Bounded buffer for micro-batching: accumulate messages up to maxSize or maxWait;
 * drain for batch processing. Back pressure: offer blocks when full (with timeout).
 */
public final class BatchBuffer {

    private final BlockingQueue<ConnectorMessage> queue;
    private final int maxBatchSize;
    private final long maxWaitMs;

    public BatchBuffer(int capacity, int maxBatchSize, Duration maxWait) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.maxBatchSize = maxBatchSize;
        this.maxWaitMs = maxWait.toMillis();
    }

    /**
     * Offer a message. Blocks up to timeoutMs if full (back pressure).
     * @return true if accepted, false if timeout
     */
    public boolean offer(ConnectorMessage message, long timeoutMs) throws InterruptedException {
        return queue.offer(message, timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Drain up to maxBatchSize elements, waiting up to maxWait for at least one.
     */
    public List<ConnectorMessage> drain() throws InterruptedException {
        ConnectorMessage first = queue.poll(maxWaitMs, TimeUnit.MILLISECONDS);
        if (first == null) {
            return List.of();
        }
        List<ConnectorMessage> batch = new java.util.ArrayList<>(List.of(first));
        queue.drainTo(batch, maxBatchSize - 1);
        return batch;
    }

    public int size() {
        return queue.size();
    }

    public int remainingCapacity() {
        return queue.remainingCapacity();
    }
}
