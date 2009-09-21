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

import org.springframework.context.Lifecycle;
import org.springframework.integration.aggregator.*;
import org.springframework.integration.channel.ChannelResolutionException;
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

import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * MessageHandler that holds a buffer of messages in a MessageStore
 *
 * @author Iwein Fuld
 */
public class BufferingMessageHandler extends AbstractMessageHandler implements Lifecycle {

    private MessageStore store = new SimpleMessageStore(100);
    private final CorrelationStrategy correlationStrategy;
    //TODO decide if we still support tracking capacity, and if this needs to be moved into the Store instead
    private final Queue trackedCorrellationIds = new LinkedBlockingQueue();
    private final CompletionStrategy completionStrategy;
    private MessagesProcessor outputProcessor;
    private MessageChannel outputChannel;
    private volatile MessageChannel discardChannel = new NullChannel();
    private TaskScheduler taskScheduler;
    private Object lifecycleMonitor = new Object();
    private ScheduledFuture reaperFutureTask;
    private volatile long reaperInterval = 1000l;
    private final BlockingQueue<DelayedKey> keysInBuffer = new DelayQueue<DelayedKey>();
    private volatile long timeout = 60000l;
    private volatile boolean sendPartialResultOnTimeout;
    private ChannelResolver channelResolver;

    public BufferingMessageHandler(MessageStore store,
                                   CorrelationStrategy correlationStrategy,
                                   CompletionStrategy completionStrategy, MessagesProcessor processor
    ) {
        Assert.notNull(store);
        Assert.notNull(correlationStrategy);
        Assert.notNull(completionStrategy);
        Assert.notNull(processor);
        this.store = store;
        this.correlationStrategy = correlationStrategy;
        this.completionStrategy = completionStrategy;
        this.outputProcessor = processor;
    }

    public BufferingMessageHandler(MessageStore store,
                                   MessagesProcessor processor) {
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
        if (!trackedCorrellationIds.contains(correlationKey)) {
            store(message, correlationKey);
            List<Message<?>> all = store.getAll(correlationKey);
            complete(correlationKey, all, this.resolveReplyChannel(message));
        } else {
            discardChannel.send(message);
        }
    }

    private boolean complete(Object correlationKey, List<Message<?>> correlatedMessages, MessageChannel messageChannel) {
        boolean processed = false;
        if (completionStrategy.isComplete(correlatedMessages)) {
            outputProcessor.processAndSend(correlationKey, correlatedMessages, messageChannel, deleteOrTrackCallback());
            processed = true;
        }
        return processed;
    }

    private void pushCorrellationId(Queue trackedCorrellationIds, Object correlationKey) {
        while (!trackedCorrellationIds.offer(correlationKey)) {
            //make room in the queue
            trackedCorrellationIds.poll();
        }
    }

    private BufferedMessagesCallback deleteOrTrackCallback() {
        return new BufferedMessagesCallback() {
            public void onProcessingOf(Message<?>... processedMessage) {
                for (Message<?> message : processedMessage) {
                    store.delete(message.getHeaders().getId());
                }
            }
            public void onCompletionOf(Object correlationKey) {
                pushCorrellationId(trackedCorrellationIds, correlationKey);
            }
        };
    }

    private void store(Message<?> message, Object correlationKey) {
        store.put(message);
        if (!keysInBuffer.contains(correlationKey)) {
            keysInBuffer.add(new DelayedKey(correlationKey, timeout));
        }
    }

    //TODO copied from AbstractReplyProducingMessageHandler
    private MessageChannel resolveReplyChannel(Message<?> requestMessage) {
        MessageChannel replyChannel = outputChannel;
        if (replyChannel == null) {
            Object replyChannelHeader = requestMessage.getHeaders().getReplyChannel();
            if (replyChannelHeader != null) {
                if (replyChannelHeader instanceof MessageChannel) {
                    replyChannel = (MessageChannel) replyChannelHeader;
                } else if (replyChannelHeader instanceof String) {
                    Assert.state(this.channelResolver != null,
                            "ChannelResolver is required for resolving a reply channel by name");
                    replyChannel = this.channelResolver.resolveChannelName((String) replyChannelHeader);
                } else {
                    throw new ChannelResolutionException("expected a MessageChannel or String for 'replyChannel', but type is ["
                            + replyChannelHeader.getClass() + "]");
                }
            }
        }
        if (replyChannel == null) {
            throw new ChannelResolutionException(
                    "unable to resolve reply channel for message: " + requestMessage);
        }
        return replyChannel;
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
            List<Message<?>> all = store.getAll(key);
            if (all.size() > 0) {
                //last chance for normal completion
                MessageChannel outputChannel = resolveReplyChannel(all.get(0));
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


    private class DelayedKey implements Delayed {
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
}
