package org.springframework.integration.store;

import org.junit.Test;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.support.GenericMessage;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Laszlo Szabo
 */
public class MessageGroupMetadataJacksonTest {

	private Jackson2JsonObjectMapper om = new Jackson2JsonObjectMapper();

	@Test
	public void testSerializerStartJobCommand() throws Exception {

		SimpleMessageGroup messageGroup = mock(SimpleMessageGroup.class);
		when(messageGroup.isComplete()).thenReturn(true);
		when(messageGroup.getLastModified()).thenReturn(11111L);
		when(messageGroup.getLastReleasedMessageSequenceNumber()).thenReturn(11);
		when(messageGroup.getGroupId()).thenReturn(true);
		when(messageGroup.getTimestamp()).thenReturn(111111L);
		when(messageGroup.getSequenceSize()).thenReturn(11);
		when(messageGroup.getMessages())
				.thenReturn(Arrays.asList(new GenericMessage<>(String.class), new GenericMessage<>(String.class)));

		MessageGroupMetadata reference = new MessageGroupMetadata(messageGroup);

		//Serialize
		String serialize = om.toJson(reference);
		assertFalse(serialize.isEmpty());

		//Deserialize
		MessageGroupMetadata deserialize = om.fromJson(serialize, MessageGroupMetadata.class);

		//check values after deserialization
		assertEquals(reference.getLastReleasedMessageSequenceNumber(), deserialize.getLastReleasedMessageSequenceNumber());
		assertEquals(reference.getLastModified(), deserialize.getLastModified());
		assertEquals(reference.getTimestamp(), deserialize.getTimestamp());
		assertNotNull(deserialize.firstId());
		assertEquals(reference.firstId(), deserialize.firstId());
		assertEquals(reference.getMessageIds().size(), deserialize.getMessageIds().size());
	}

}
