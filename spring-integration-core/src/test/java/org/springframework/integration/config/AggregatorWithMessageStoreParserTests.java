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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatorWithMessageStoreParserTests {
	
	@Autowired
	@Qualifier("input")
	private MessageChannel input;
	
	@Autowired
	private TestAggregatorBean aggregatorBean;
	
	@Autowired
	private MessageGroupStore messageGroupStore;

    @Test
    @DirtiesContext
    public void testAggregation() {
 
        input.send(createMessage("123", "id1", 3, 1, null));
        assertEquals(1, messageGroupStore.getMessageGroup("id1").size());
        input.send(createMessage("789", "id1", 3, 3, null));
        assertEquals(2, messageGroupStore.getMessageGroup("id1").size());
        input.send(createMessage("456", "id1", 3, 2, null));
        assertEquals("One and only one message should have been aggregated", 1, aggregatorBean
                .getAggregatedMessages().size());
        Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
        assertEquals("The aggregated message payload is not correct", "123456789", aggregatedMessage
                .getPayload());
    }


    @Test
    @DirtiesContext
    public void testExpiry() {
 
        input.send(createMessage("123", "id1", 3, 1, null));
        assertEquals(1, messageGroupStore.getMessageGroup("id1").size());
        input.send(createMessage("456", "id1", 3, 2, null));
        assertEquals(2, messageGroupStore.getMessageGroup("id1").size());
        messageGroupStore.expireMessageGroups(-10000);
        assertEquals("One and only one message should have been aggregated", 1, aggregatorBean
                .getAggregatedMessages().size());
        Message<?> aggregatedMessage = aggregatorBean.getAggregatedMessages().get("id1");
        assertEquals("The aggregated message payload is not correct", "123456", aggregatedMessage
                .getPayload());
    }


    private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
                                                MessageChannel outputChannel) {
        return MessageBuilder.withPayload(payload)
                .setCorrelationId(correlationId)
                .setSequenceSize(sequenceSize)
                .setSequenceNumber(sequenceNumber)
                .setReplyChannel(outputChannel).build();
    }

}
