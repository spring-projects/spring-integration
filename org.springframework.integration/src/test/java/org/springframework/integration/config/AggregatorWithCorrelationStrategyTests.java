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

package org.springframework.integration.config;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.aggregator.CompletionStrategy;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author: Marius Bogoevici
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatorWithCorrelationStrategyTests {

    @Autowired
    @Qualifier("inputChannel")
    MessageChannel inputChannel;

    @Autowired
    @Qualifier("outputChannel")
    PollableChannel outputChannel;

    @Autowired
    @Qualifier("pojoInputChannel")
    MessageChannel pojoInputChannel;

    @Autowired
    @Qualifier("pojoOutputChannel")
    PollableChannel pojoOutputChannel;


    @Test
    public void testCorrelationAndCompletion() {
        inputChannel.send(MessageBuilder.withPayload("A1").build());
        inputChannel.send(MessageBuilder.withPayload("B2").build());
        inputChannel.send(MessageBuilder.withPayload("C3").build());
        inputChannel.send(MessageBuilder.withPayload("A4").build());
        inputChannel.send(MessageBuilder.withPayload("B5").build());
        inputChannel.send(MessageBuilder.withPayload("C6").build());
        inputChannel.send(MessageBuilder.withPayload("A7").build());
        inputChannel.send(MessageBuilder.withPayload("B8").build());
        inputChannel.send(MessageBuilder.withPayload("C9").build());
        receiveAndCompare(outputChannel, "A1A4A7");
        receiveAndCompare(outputChannel, "B2B5B8");
        receiveAndCompare(outputChannel, "C3C6C9");
    }

    @Test
    public void testCorrelationAndCompletionWithPojo() {
        // the test verifies how a pojo strategy is applied
        // Strings are correlated by their first letter, integers are correlated by the last digit
        pojoInputChannel.send(MessageBuilder.withPayload("X1").build());
        pojoInputChannel.send(MessageBuilder.withPayload("Y2").build());
        pojoInputChannel.send(MessageBuilder.withPayload(93).build());
        pojoInputChannel.send(MessageBuilder.withPayload("X4").build());
        pojoInputChannel.send(MessageBuilder.withPayload("Y5").build());
        pojoInputChannel.send(MessageBuilder.withPayload(113).build());
        pojoInputChannel.send(MessageBuilder.withPayload("X7").build());
        pojoInputChannel.send(MessageBuilder.withPayload("Y8").build());
        pojoInputChannel.send(MessageBuilder.withPayload(213).build());
        receiveAndCompare(pojoOutputChannel, "X1X4X7");
        receiveAndCompare(pojoOutputChannel, "Y2Y5Y8");
        receiveAndCompare(pojoOutputChannel, "93113213");
    }

    private void receiveAndCompare(PollableChannel outputChannel, String expectedValue) {
        Message<?> firstResult = outputChannel.receive(500);
        Assert.assertNotNull(firstResult);
        Assert.assertEquals(expectedValue, firstResult.getPayload());
    }


    public static class MessageCountCompletionStrategy implements CompletionStrategy {

        private final int expectedSize;


        public MessageCountCompletionStrategy(int expectedSize) {
            this.expectedSize = expectedSize;
        }

        public boolean isComplete(List<Message<?>> messages) {
            return messages.size() == expectedSize;
        }

    }

    public static class FirstLetterCorrelationStrategy implements CorrelationStrategy {

        public Object getCorrelationKey(Message<?> message) {
            return message.getPayload().toString().subSequence(0,1);
        }

    }

    public static class PojoCorrelationStrategy {

        public String correlate(String message) {
            return message.substring(0,1);
        }

        public String correlate(Integer mesage) {
            return Integer.toString(mesage % 10);
        }

    }

    public static class SimpleAggregator {

        @Aggregator
        protected String concatenate(List<Object> payloads) {
            StringBuffer buffer = new StringBuffer();
            for (Object payload: payloads) {
                buffer.append(payload.toString());
            }
            return buffer.toString();
        }

    }

}
