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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ResequencerWithMessageStoreParserTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	private MessageGroupStore messageGroupStore;

    @Test
    public void testResequence() {

        input.send(createMessage("123", "id1", 3, 1, null));
        assertEquals(1, messageGroupStore.getMessageGroup("id1").size());
        input.send(createMessage("789", "id1", 3, 3, null));
        assertEquals(2, messageGroupStore.getMessageGroup("id1").size());
        input.send(createMessage("456", "id1", 3, 2, null));

		Message<?> message1 = output.receive(500);
		Message<?> message2 = output.receive(500);
		Message<?> message3 = output.receive(500);

		assertNotNull(message1);
		assertEquals(new Integer(1), new IntegrationMessageHeaderAccessor(message1).getSequenceNumber());
		assertNotNull(message2);
		assertEquals(new Integer(2), new IntegrationMessageHeaderAccessor(message2).getSequenceNumber());
		assertNotNull(message3);
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(message3).getSequenceNumber());

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
