/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;

/**
 * @author Iwein Fuld
 */
@RunWith(MockitoJUnitRunner.class)
public class CorrelatingMessageHandlerTests {

    private CorrelatingMessageHandler handler;

    @Mock
    private MessageStore store;

    @Mock
    private CorrelationStrategy correlationStrategy;

    @Mock
    private CompletionStrategy completionStrategy;

    @Mock
    private MessageGroupProcessor processor;

    @Mock
    private MessageChannel outputChannel;

    @Before
    public void initializeSubject() {
        handler = new CorrelatingMessageHandler(
                store, correlationStrategy, completionStrategy, processor);
        handler.setOutputChannel(outputChannel);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MessageGroup messageGroup = (MessageGroup) invocation.getArguments()[0];
                // TODO: remove this?
                return null;
            }
        }).when(processor).processAndSend(isA(MessageGroup.class),
                isA(MessageChannelTemplate.class), eq(outputChannel));
    }

    @Test
    public void bufferCompletesNormally() throws Exception {
        String correlationKey = "key";
        Message<?> message1 = testMessage(correlationKey, 1);
        Message<?> message2 = testMessage(correlationKey, 2);
        List<Message<?>> storedMessages = new ArrayList<Message<?>>();

        when(store.list(correlationKey)).thenReturn(storedMessages);

        when(correlationStrategy.getCorrelationKey(isA(Message.class)))
                .thenReturn(correlationKey);

        when(completionStrategy.isComplete(Arrays.<Message<?>>asList(message1))).thenReturn(false);

        handler.handleMessage(message1);
        storedMessages.add(message1);

        when(completionStrategy.isComplete(Arrays.<Message<?>>asList(message1, message2))).thenReturn(true);
        handler.handleMessage(message2);
        storedMessages.add(message2);

        verify(store).put(correlationKey, message1);
        verify(store).put(correlationKey, message2);
        verify(store, times(2)).list(correlationKey);
        verify(correlationStrategy).getCorrelationKey(message1);
        verify(correlationStrategy).getCorrelationKey(message2);
        verify(completionStrategy).isComplete(Arrays.<Message<?>>asList(message1));
        verify(completionStrategy).isComplete(Arrays.<Message<?>>asList(message1, message2));
        verify(processor).processAndSend(isA(MessageGroup.class),
                isA(MessageChannelTemplate.class), eq(outputChannel)
        );
    }

    /*
    The next test verifies that when pruning happens after the completing message arrived, but before the group was
    processed locking prevents forced completion and the group completes normally.
     */

    @Test
    public void shouldNotPruneWhileCompleting() throws Exception {
        String correlationKey = "key";
        final Message<?> message1 = testMessage(correlationKey, 1);
        final Message<?> message2 = testMessage(correlationKey, 2);
        final List<Message<?>> storedMessages = new ArrayList<Message<?>>();

        final CountDownLatch bothMessagesHandled = new CountDownLatch(2);

        when(store.list(correlationKey)).thenReturn(storedMessages);

        when(correlationStrategy.getCorrelationKey(isA(Message.class)))
                .thenReturn(correlationKey);

        when(completionStrategy.isComplete(Arrays.<Message<?>>asList(message1, message2)))
        		.thenAnswer(new Answer<Boolean>() {
        				public Boolean answer(InvocationOnMock invocation) throws Throwable {
        					Thread.sleep(50);
        					return true;
        				}
        		}).thenReturn(true);

        handler.handleMessage(message1);
        bothMessagesHandled.countDown();
        storedMessages.add(message1);
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            public void run() {
                handler.handleMessage(message2);
                storedMessages.add(message2);
                bothMessagesHandled.countDown();
            }
        });

        Thread.sleep(20);
        assertFalse(handler.forceComplete("key"));

        bothMessagesHandled.await();
        verify(store).put(correlationKey, message1);
        verify(store).put(correlationKey, message2);
        verify(store).deleteAll(correlationKey);
    }

    private Message<?> testMessage(String correlationKey, int sequenceNumber) {
        return MessageBuilder.withPayload("test" + sequenceNumber)
                .setCorrelationId(correlationKey)
                .setSequenceNumber(sequenceNumber).build();
    }

}
