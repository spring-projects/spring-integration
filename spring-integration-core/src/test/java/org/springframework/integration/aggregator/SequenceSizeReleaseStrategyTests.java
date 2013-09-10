/*
 * Copyright 2002-2011 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class SequenceSizeReleaseStrategyTests {

	@Test
	public void testIncompleteList() {
		Message<String> message = MessageBuilder.withPayload("test1").setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message);
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		assertFalse(releaseStrategy.canRelease(messages));
	}

	@Test
	public void testCompleteList() {
		Message<String> message1 = MessageBuilder.withPayload("test1").setSequenceSize(2).build();
		Message<String> message2 = MessageBuilder.withPayload("test2").setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message1);
		messages.add(message2);
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		assertTrue(releaseStrategy.canRelease(messages));
	}

	@Test
	public void testEmptyList() {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		assertTrue(releaseStrategy.canRelease(new SimpleMessageGroup("FOO")));
	}

	@Test
	public void testEmptyGroup() {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		releaseStrategy.setReleasePartialSequences(true);
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		Message<String> message = MessageBuilder.withPayload("test1").setSequenceSize(1).build();
		messages.add(message);
		messages.remove(message);
		assertTrue(releaseStrategy.canRelease(messages));
	}

	@Test
	public void shouldReleaseHeadOfSequenceDeliveredInOrder() {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		releaseStrategy.setReleasePartialSequences(true);

		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");

		assertTrue(releaseStrategy.canRelease(groupWithFirstMessagesOfIncompleteSequence(messages)));
	}

	private SimpleMessageGroup groupWithFirstMessagesOfIncompleteSequence(SimpleMessageGroup messages) {
		Message<String> message1 = MessageBuilder.withPayload("test1").setSequenceSize(3).setSequenceNumber(1).build();
		Message<String> message2 = MessageBuilder.withPayload("test2").setSequenceSize(3).setSequenceNumber(2).build();

		messages.add(message1);
		messages.add(message2);
		return messages;
	}

	@Test
	public void shouldReleaseHeadOfSequenceDeliveredOutOfOrder() {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		releaseStrategy.setReleasePartialSequences(true);

		boolean canRelease = releaseStrategy.canRelease(groupWithLastAndFirstMessagesOfIncompleteSequence());

		assertTrue(canRelease);
	}

	private MessageGroup groupWithLastAndFirstMessagesOfIncompleteSequence() {
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");

		Message<String> message1 = MessageBuilder.withPayload("test1").setSequenceSize(3).setSequenceNumber(3).build();
		Message<String> message2 = MessageBuilder.withPayload("test2").setSequenceSize(3).setSequenceNumber(1).build();

		messages.add(message1);
		messages.add(message2);
		return messages;
	}

	@Test
	public void shouldPartiallyReleaseAsEarlyAsPossible() {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		releaseStrategy.setReleasePartialSequences(true);

		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");

		Message<String> message1 = MessageBuilder.withPayload("test1").setSequenceSize(5).setSequenceNumber(1).build();
		Message<String> message2 = MessageBuilder.withPayload("test2").setSequenceSize(5).setSequenceNumber(2).build();
		Message<String> message3 = MessageBuilder.withPayload("test3").setSequenceSize(5).setSequenceNumber(3).build();
		Message<String> message4 = MessageBuilder.withPayload("test4").setSequenceSize(5).setSequenceNumber(4).build();
		Message<String> message5 = MessageBuilder.withPayload("test5").setSequenceSize(5).setSequenceNumber(5).build();

		messages.add(message5);
		assertFalse(releaseStrategy.canRelease(messages));
		messages.add(message1);
		assertTrue(releaseStrategy.canRelease(messages));
		messages.add(message2);
		assertTrue(releaseStrategy.canRelease(messages));
		messages.add(message3);
		assertTrue(releaseStrategy.canRelease(messages));
		messages.add(message4);
		assertTrue(releaseStrategy.canRelease(messages));
	}

}
