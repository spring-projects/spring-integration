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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MessageHandler that holds a buffer of correlated messages in a MessageStore. This class takes care of correlated
 * groups of messages that can be completed in batches. It is useful for aggregating, resequencing, or custom
 * implementations requiring correlation.
 * <p/>
 * To customize this handler inject {@link org.springframework.integration.aggregator.CorrelationStrategy}, {@link
 * org.springframework.integration.aggregator.CompletionStrategy} and {@link org.springframework.integration.aggregator.MessageGroupProcessor}
 * implementations as you require.
 * <p/>
 * By default the CorrelationStrategy will be a HeaderAttributeCorrelationStrategy and the CompletionStrategy will be a
 * SequenceSizeCompletionStrategy.
 *
 * @author Iwein Fuld
 */
public class CorrelatingMessageHandler extends AbstractMessageHandler implements Lifecycle, BeanFactoryAware {
    private static final Log logger = LogFactory.getLog(CorrelatingMessageHandler.class);

    public static final String COMPONENT_TYPE_LABEL = "aggregator";
    
    private static final long DEFAULT_SEND_TIMEOUT = 1000l;
    private static final long DEFAULT_REAPER_INTERVAL = 1000l;
    private static final long DEFAULT_TIMEOUT = 60000l;

    private final MessageStore store;
    private final MessageGroupProcessor outputProcessor;

    private volatile CorrelationStrategy correlationStrategy = new HeaderAttributeCorrelationStrategy(MessageHeaders.CORRELATION_ID);
    private volatile CompletionStrategy completionStrategy = new SequenceSizeCompletionStrategy();

    private MessageChannel outputChannel;
    private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();

    private volatile MessageChannel discardChannel = new NullChannel();
    private ChannelResolver channelResolver;

    private final IdTracker tracker = new IdTracker();
    private final BlockingQueue<DelayedKey> keysInBuffer = new DelayQueue<DelayedKey>();

    private volatile TaskScheduler taskScheduler;
    private volatile ScheduledFuture<?> reaperFutureTask;
    private volatile long reaperInterval = DEFAULT_REAPER_INTERVAL;
    private volatile long timeout = DEFAULT_TIMEOUT;
    private volatile boolean sendPartialResultOnTimeout;

    private Object lifecycleMonitor = new Object();

    public CorrelatingMessageHandler(MessageStore store,
                                     CorrelationStrategy correlationStrategy,
                                     CompletionStrategy completionStrategy,
                                     MessageGroupProcessor processor) {
        Assert.notNull(store);
        Assert.notNull(processor);
        Assert.notNull(correlationStrategy);
        Assert.notNull(completionStrategy);   
        this.store = store;
        this.outputProcessor = processor;
        this.correlationStrategy = correlationStrategy;
        this.completionStrategy = completionStrategy;
        this.channelTemplate.setSendTimeout(DEFAULT_SEND_TIMEOUT);
    }

    public CorrelatingMessageHandler(MessageStore store,
                                     MessageGroupProcessor processor) {
        this(store, new HeaderAttributeCorrelationStrategy(
                MessageHeaders.CORRELATION_ID),
                new SequenceSizeCompletionStrategy(), processor);
    }

    public CorrelatingMessageHandler(
            MessageGroupProcessor processor) {
        this(new SimpleMessageStore(100),
                new HeaderAttributeCorrelationStrategy(MessageHeaders.CORRELATION_ID),
                new SequenceSizeCompletionStrategy(), processor);
    }

    public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
        Assert.notNull(correlationStrategy);
        this.correlationStrategy = correlationStrategy;
    }

    public void setCompletionStrategy(CompletionStrategy completionStrategy) {
        Assert.notNull(completionStrategy);
        this.completionStrategy = completionStrategy;
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
        Assert.notNull(outputChannel, "'outputChannel' must not be null");
        this.outputChannel = outputChannel;
    }

    public void setChannelResolver(ChannelResolver channelResolver) {
        this.channelResolver = channelResolver;
    }

    public void setDiscardChannel(MessageChannel discardChannel) {
        this.discardChannel = discardChannel;
    }

    public void setSendTimeout(long sendTimeout) {
        this.channelTemplate.setSendTimeout(sendTimeout);
    }

    public void setSendPartialResultOnTimeout(boolean sendPartialResultOnTimeout) {
        this.sendPartialResultOnTimeout = sendPartialResultOnTimeout;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        if (this.taskScheduler == null) {
            this.taskScheduler = IntegrationContextUtils.getRequiredTaskScheduler(beanFactory);
        }
    }

    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {
        Object correlationKey = correlationStrategy.getCorrelationKey(message);
        if (logger.isDebugEnabled()) {
            logger.debug("Handling message with correllationKey [" + correlationKey + "]: " + message);
        }
        try {
            if (tracker.waitForLockIfNotTracked(correlationKey)) {
                MessageGroup group =
                        new MessageGroup(store.list(correlationKey),
                                completionStrategy, correlationKey, deleteOrTrackCallback());
                if (group.hasNoMessageSuperseding(message)) {
                    store(message, correlationKey);
                    group.add(message);

                    if (group.isComplete()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Completing group with correllationKey [" + correlationKey + "]");
                        }
                        outputProcessor.processAndSend(group,
                                channelTemplate, this.resolveReplyChannel(message, this.outputChannel, this.channelResolver));
                    }
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

    private MessageGroupListener deleteOrTrackCallback() {
        return new MessageGroupListener() {
            public void onProcessingOf(Message<?>... processedMessage) {
                for (Message<?> message : processedMessage) {
                    store.delete(message.getHeaders().getId());
                }
            }

            public void onCompletionOf(Object correlationKey) {
                tracker.pushCorrelationId(correlationKey);
            }
        };
    }

    private void store(Message<?> message, Object correlationKey) {
        Message toStore = message;
        if (!correlationKey.equals(message.getHeaders().getCorrelationId())) {
            toStore = MessageBuilder.fromMessage(message)
                    .setCorrelationId(correlationKey).build();
        }
        store.put(toStore);
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
                    if (!forceComplete(key)) {
                        keysInBuffer.offer(delayedKey);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    protected final boolean forceComplete(Object key) {
        try {
            if (tracker.tryLockFor(key)) {
                List<Message<?>> all = store.list(key);
                MessageGroup group = new MessageGroup(all, completionStrategy, key, deleteOrTrackCallback());
                if (all.size() > 0) {
                    //last chance for normal completion
                    MessageChannel outputChannel = resolveReplyChannel(all.get(0), this.outputChannel, this.channelResolver);
                    boolean processed = false;
                    if (group.isComplete()) {
                        outputProcessor.processAndSend(group, channelTemplate, outputChannel);
                        processed = true;
                    }
                    if (!processed) {
                        if (sendPartialResultOnTimeout) {
                            if (logger.isInfoEnabled()) {
                                logger.info("Processing partially complete messages for key [" + key + "] to: " + outputChannel);
                            }
                            outputProcessor.processAndSend(group, channelTemplate, outputChannel);
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
                return true;
            } else {
                return false;
            }
        } finally {
            tracker.unlock(key);
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
        private final Queue<Object> trackedCorrelationIds = new LinkedBlockingQueue<Object>();

        private void pushCorrelationId(Object correlationKey) {
            while (!trackedCorrelationIds.offer(correlationKey)) {
                //make room in the queue
                trackedCorrelationIds.poll();
            }
            trackerLocks.remove(correlationKey);
        }

        /**
         * Call this method to check if an id is tracked and obtain a lock for it. Don't forget to finally unlock
         * afterwards.
         *
         * @return false if the key was tracked, true after obtaining the lock otherwise.
         */
        private boolean waitForLockIfNotTracked(Object correlationKey) {
            ReentrantLock lock = trackerLocks.get(correlationKey);
            if (lock == null) {
                if (trackedCorrelationIds.contains(correlationKey)) {
                    //this correlation key is already processed in the near past: disallow processing
                    return false;
                }
                lock = new ReentrantLock();
                ReentrantLock original = trackerLocks.putIfAbsent(correlationKey, lock);
                lock = original == null ? lock : original;
            }
            lock.lock();
            return true;
        }

        private boolean tryLockFor(Object correlationKey) {
            ReentrantLock lock = trackerLocks.get(correlationKey);
            if (lock == null) {
                lock = new ReentrantLock();
                ReentrantLock original = trackerLocks.putIfAbsent(correlationKey, lock);
                lock = original == null ? lock : original;
            }
            return lock.tryLock();
        }

        private void unlock(Object correlationKey) {
            ReentrantLock lock = trackerLocks.get(correlationKey);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
