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

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * @author Marius Bogoevici
 * @author Iwein Fuld
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
        inputChannel.send(MessageBuilder.withPayload("A1").setSequenceNumber(0).build());
        inputChannel.send(MessageBuilder.withPayload("B2").setSequenceNumber(0).build());
        inputChannel.send(MessageBuilder.withPayload("C3").setSequenceNumber(0).build());
        inputChannel.send(MessageBuilder.withPayload("A4").setSequenceNumber(1).build());
        inputChannel.send(MessageBuilder.withPayload("B5").setSequenceNumber(1).build());
        inputChannel.send(MessageBuilder.withPayload("C6").setSequenceNumber(1).build());
        inputChannel.send(MessageBuilder.withPayload("A7").setSequenceNumber(2).build());
        inputChannel.send(MessageBuilder.withPayload("B8").setSequenceNumber(2).build());
        inputChannel.send(MessageBuilder.withPayload("C9").setSequenceNumber(2).build());
        receiveAndCompare(outputChannel, "A1","A4","A7");
        receiveAndCompare(outputChannel, "B2","B5","B8");
        receiveAndCompare(outputChannel, "C3","C6","C9");
    }

    @Test
    public void testCorrelationAndCompletionWithPojo() {
        // the test verifies how a pojo strategy is applied
        // Strings are correlated by their first letter, integers are correlated by the last digit
        pojoInputChannel.send(MessageBuilder.withPayload("X1").setSequenceNumber(0).build());
        pojoInputChannel.send(MessageBuilder.withPayload(93).setSequenceNumber(0).build());
        pojoInputChannel.send(MessageBuilder.withPayload("X4").setSequenceNumber(1).build());
        pojoInputChannel.send(MessageBuilder.withPayload(113).setSequenceNumber(1).build());
        pojoInputChannel.send(MessageBuilder.withPayload("X7").setSequenceNumber(2).build());
        pojoInputChannel.send(MessageBuilder.withPayload(213).setSequenceNumber(2).build());
        receiveAndCompare(pojoOutputChannel, "X1","X4","X7");
        receiveAndCompare(pojoOutputChannel, "93","113","213");
    }

    private void receiveAndCompare(PollableChannel outputChannel, String... expectedValues) {
        Message<?> message = outputChannel.receive(500);
        Assert.assertNotNull(message);
        for (String expectedValue : expectedValues) {
            assertThat((String)message.getPayload(), containsString(expectedValue));
        }
    }


    public static class MessageCountCompletionStrategy implements CompletionStrategy {

        private final int expectedSize;


        public MessageCountCompletionStrategy(int expectedSize) {
            this.expectedSize = expectedSize;
        }

        public boolean isComplete(Collection<? extends Message<?>> messages) {
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

        public String correlate(Integer message) {
            return Integer.toString(message % 10);
        }

    }

    public static class SimpleAggregator {

        @Aggregator
        public String concatenate(List<Object> payloads) {
            StringBuffer buffer = new StringBuffer();
            for (Object payload: payloads) {
                buffer.append(payload.toString());
            }
            return buffer.toString();
        }

    }

}
