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

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Iwein Fuld
 */
public class DefaultResequencerStrategies implements CorrelationStrategy, CompletionStrategy, MessagesProcessor {
    private final ConcurrentMap<Object, AtomicInteger> nextMessagesToPass = new ConcurrentHashMap<Object, AtomicInteger>();
    private volatile DefaultResequencerStrategies.SequenceNumberComparator sequenceSizeComparator = new SequenceNumberComparator();
    private volatile boolean releasePartialSequences;

    public Object getCorrelationKey(Message<?> message) {
        Object key = message.getHeaders().getCorrelationId();
        nextMessagesToPass.putIfAbsent(key, new AtomicInteger(1));
        return key;
    }

    public boolean isComplete(List<Message<?>> messages) {
        return releasePartialSequences||
                messages.get(0).getHeaders().getSequenceSize()==messages.size();
    }

    public void processAndSend(Object correlationKey, Collection<Message<?>> all, MessageChannel outputChannel, BufferedMessagesCallback processedCallback) {
        if (all.size() > 0) {
            List<Message> sorted = new ArrayList(all);
            Collections.sort(sorted, sequenceSizeComparator);
            AtomicInteger nextSequence = nextMessagesToPass.get(correlationKey);
            for (Message message : sorted) {
                final int sequenceNumber = message.getHeaders().getSequenceNumber();
                if (sequenceNumber <= nextSequence.get()) {
                    outputChannel.send(message);
                    nextSequence.compareAndSet(sequenceNumber, sequenceNumber + 1);
                    processedCallback.onProcessingOf(message);
                }
            }
            MessageHeaders headers = sorted.get(0).getHeaders();
            if (all.size() == headers.getSequenceSize()){
                processedCallback.onCompletionOf(correlationKey);
            }
        }
    }

    public void setReleasePartialSequences(boolean releasePartialSequences) {
        this.releasePartialSequences = releasePartialSequences;
    }

    private class SequenceNumberComparator implements Comparator<Message> {
        public int compare(Message o1, Message o2) {
            return o1.getHeaders().getSequenceNumber().compareTo(o2.getHeaders().getSequenceNumber());
        }
    }
}
