/*
 * Copyright 2002-2008 the original author or authors.
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
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Dave Syer
 */
public class TimeoutCountSequenceSizeReleaseStrategyTests {

	@Test
	public void testIncompleteList() {
		Message<String> message = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message);
		TimeoutCountSequenceSizeReleaseStrategy releaseStrategy = new TimeoutCountSequenceSizeReleaseStrategy();
		assertFalse(releaseStrategy.canRelease(messages));
	}

	@Test
	public void testIncompleteListWithTimeout() {
		Message<String> message = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message);
		TimeoutCountSequenceSizeReleaseStrategy releaseStrategy = new TimeoutCountSequenceSizeReleaseStrategy(TimeoutCountSequenceSizeReleaseStrategy.DEFAULT_THRESHOLD, -100);
		assertTrue(releaseStrategy.canRelease(messages));
	}

	@Test
	public void testIncompleteListWithCount() {
		Message<String> message = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message);
		TimeoutCountSequenceSizeReleaseStrategy releaseStrategy = new TimeoutCountSequenceSizeReleaseStrategy(1, TimeoutCountSequenceSizeReleaseStrategy.DEFAULT_TIMEOUT);
		assertTrue(releaseStrategy.canRelease(messages));
	}

	@Test
	public void testCompleteList() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceSize(2).build();
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

}
