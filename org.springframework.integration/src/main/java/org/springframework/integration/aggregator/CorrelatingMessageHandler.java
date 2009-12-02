/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * MessageHandler that holds a buffer of messages in a MessageStore. This class takes care of 
 * correlated groups of messages that can be completed in batches. It is useful for aggregating,
 * resequencing, or custom implementations requiring correlation. 
 *
 * @author Iwein Fuld
 */
public class CorrelatingMessageHandler extends AbstractMessageHandler implements Lifecycle {

    private MessageStore store = new SimpleMessageStore(100);
    private final CorrelationStrategy correlationStrategy;
    private final IdTracker tracker = new IdTracker();
    private final CompletionStrategy completionStrategy;
    private MessageGroupProcessor outputProcessor;
    private MessageChannel outputChannel;
    private volatile MessageChannel discardChannel = new NullChannel();
    private TaskScheduler taskScheduler;
    private Object lifecycleMonitor = new Object();
    private ScheduledFuture<?> reaperFutureTask;
    private volatile long reaperInterval = 1000l;
    private final BlockingQueue<DelayedKey> keysInBuffer = new DelayQueue<DelayedKey>();
    private volatile long timeout = 60000l;
    private volatile boolean sendPartialResultOnTimeout;
    private ChannelResolver channelResolver;

    public CorrelatingMessageHandler(MessageStore store,
                                   CorrelationStrategy correlationStrategy,
                                   CompletionStrategy completionStrategy,
                                   MessageGroupProcessor processor) {
        Assert.notNull(store);
        Assert.notNull(correlationStrategy);
        Assert.notNull(completionStrategy);
        Assert.notNull(processor);
        this.store = store;
        this.correlationStrategy = correlationStrategy;
        this.completionStrategy = completionStrategy;
        this.outputProcessor = processor;
    }

    public CorrelatingMessageHandler(MessageStore store,
                                   MessageGroupProcessor processor) {
        this(store, new HeaderAttributeCorrelationStrategy(
                MessageHeaders.CORRELATION_ID),
                new SequenceSizeCompletionStrategy(), processor);
    }

    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setReaperInterval(long reaperInterval) {
        this.reaperInterval = reaperInterval;
    }

    public void setOutputChannel(MessageChannel outputChannel) {
        this.outputChannel = outputChannel;
    }

    public void setChannelResolver(ChannelResolver channelResolver) {
        this.channelResolver = channelResolver;
    }

    public void setDiscardChannel(MessageChannel discardChannel) {
        this.discardChannel = discardChannel;
    }

    public void setSendPartialResultOnTimeout(boolean sendPartialResultOnTimeout) {
        this.sendPartialResultOnTimeout = sendPartialResultOnTimeout;
    }

    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {
        Object correlationKey = correlationStrategy.getCorrelationKey(message);
        try {
            if (tracker.aquireLockFor(correlationKey)) {
                List<Message<?>> group = store.list(correlationKey);
                if (noSupersedingMessage(message, group)) {
                    store(message, correlationKey);
                    group.add(message);
                    complete(correlationKey, group, this.resolveReplyChannel(message, this.outputChannel, this.channelResolver));
                } else {
                    discardChannel.send(message);
                }
            } else {
                discardChannel.send(message);
            }
        } finally {
            tracker.unlock(correlationKey);
        }
    }

    private boolean noSupersedingMessage(Message<?> message, List<Message<?>> group) {
        for (Message<?> member : group) {
            if (member.getHeaders().getSequenceNumber() == message.getHeaders().getSequenceNumber()) {
                return false;
            }
        }
        return true;
    }

    private boolean complete(Object correlationKey, List<Message<?>> correlatedMessages, MessageChannel messageChannel) {
        boolean processed = false;
        if (completionStrategy.isComplete(correlatedMessages)) {
            outputProcessor.processAndSend(correlationKey, correlatedMessages, messageChannel, deleteOrTrackCallback());
            processed = true;
        }
        return processed;
    }

    private BufferedMessagesCallback deleteOrTrackCallback() {
        return new BufferedMessagesCallback() {
            public void onProcessingOf(Message<?>... processedMessage) {
                for (Message<?> message : processedMessage) {
                    store.delete(message.getHeaders().getId());
                }
            }

            public void onCompletionOf(Object correlationKey) {
                tracker.pushCorrellationId(correlationKey);
            }
        };
    }

    private void store(Message<?> message, Object correlationKey) {
        store.put(message);
        if (!keysInBuffer.contains(correlationKey)) {
            keysInBuffer.add(new DelayedKey(correlationKey, timeout));
        }
    }

    public boolean isRunning() {
        synchronized (this.lifecycleMonitor) {
            return this.reaperFutureTask != null;
        }
    }

    public void start() {
        synchronized (this.lifecycleMonitor) {
            if (this.isRunning()) {
                return;
            }
            Assert.state(this.taskScheduler != null, "'taskScheduler' must not be null");
            this.reaperFutureTask = this.taskScheduler.scheduleWithFixedDelay(
                    new PrunerTask(), this.reaperInterval);
        }
    }

    public void stop() {
        synchronized (this.lifecycleMonitor) {
            if (this.isRunning()) {
                this.reaperFutureTask.cancel(true);
            }
        }
    }

    private class PrunerTask implements Runnable {
        public void run() {
            if (logger.isTraceEnabled()) {
                logger.trace("PrunerTask is running");
            }
            DelayedKey delayedKey;
            try {
                while ((delayedKey = keysInBuffer.poll(reaperInterval, TimeUnit.MILLISECONDS)) != null) {
                    Object key = delayedKey.getKey();
                    if (logger.isDebugEnabled()) {
                        logger.debug(this + "'s PrunerTask is processing " + key);
                    }
                    forceComplete(key);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    protected final void forceComplete(Object key) {
        List<Message<?>> all = store.list(key);
        if (all.size() > 0) {
            //last chance for normal completion
            MessageChannel outputChannel = resolveReplyChannel(all.get(0), this.outputChannel, this.channelResolver);
            boolean fullyCompleted = complete(key, all, outputChannel);
            if (!fullyCompleted) {
                if (sendPartialResultOnTimeout) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Processing partially complete messages for key [" + key + "] to: " + outputChannel);
                    }
                    outputProcessor.processAndSend(key, all, outputChannel, deleteOrTrackCallback());
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("Discarding partially complete messages for key [" + key + "] to: " + discardChannel);
                    }
                    for (Message<?> message : all) {
                        discardChannel.send(message);
                        store.delete(message.getHeaders().getId());
                    }
                }
            }
        }
    }


    private final class DelayedKey implements Delayed {
        private Object key;
        private Long releaseTime;
        private TimeUnit unit = TimeUnit.MILLISECONDS;

        public DelayedKey(Object correlationKey, long delay) {
            Assert.notNull(correlationKey, "'correlationKey' must not be null");
            this.key = correlationKey;
            this.releaseTime = System.currentTimeMillis() + delay;
        }

        public long getDelay(TimeUnit unit) {
            return unit.convert(this.releaseTime - System.currentTimeMillis(), this.unit);
        }

        public int compareTo(Delayed o) {
            return ((Long) this.getDelay(this.unit)).compareTo(o.getDelay(this.unit));
        }

        public Object getKey() {
            return key;
        }
    }

    private final class IdTracker {
        private ConcurrentMap<Object, ReentrantLock> trackerLocks = new ConcurrentHashMap<Object, ReentrantLock>();
        private final Queue<Object> trackedCorrellationIds = new LinkedBlockingQueue<Object>();

        private void pushCorrellationId(Object correlationKey) {
            while (!trackedCorrellationIds.offer(correlationKey)) {
                //make room in the queue
                trackedCorrellationIds.poll();
            }
            trackerLocks.remove(correlationKey);
        }

        private boolean aquireLockFor(Object correlationKey) {
            ReentrantLock lock = trackerLocks.get(correlationKey);
            if (lock == null) {
                if (trackedCorrellationIds.contains(correlationKey)) {
                    return false;
                }
                lock = new ReentrantLock();
                ReentrantLock original = trackerLocks.putIfAbsent(correlationKey, lock);
                lock = original == null ? lock : original;
            }
            lock.lock();
            return true;
        }

        private void unlock(Object correlationKey) {
            ReentrantLock lock = trackerLocks.get(correlationKey);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
