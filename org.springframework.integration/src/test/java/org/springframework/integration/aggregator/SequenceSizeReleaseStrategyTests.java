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
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;

/**
 * @author Mark Fisher
 */
public class SequenceSizeReleaseStrategyTests {

	@Test
	public void testIncompleteList() {
		Message<String> message = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		MessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message);
		SequenceSizeReleaseStrategy ReleaseStrategy = new SequenceSizeReleaseStrategy();
		assertFalse(ReleaseStrategy.canRelease(messages));
	}

	@Test
	public void testCompleteList() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceSize(2).build();
		MessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message1);
		messages.add(message2);
		SequenceSizeReleaseStrategy ReleaseStrategy = new SequenceSizeReleaseStrategy();
		assertTrue(ReleaseStrategy.canRelease(messages));
	}

	@Test
	public void testEmptyList() {
		SequenceSizeReleaseStrategy ReleaseStrategy = new SequenceSizeReleaseStrategy();
		assertTrue(ReleaseStrategy.canRelease(new SimpleMessageGroup("FOO")));
	}

}
