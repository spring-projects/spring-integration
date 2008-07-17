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

package org.springframework.integration.router;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class SequenceSizeCompletionStrategyTests {

	@Test
	public void testIncompleteList() {
		Message<String> message = MessageBuilder.fromPayload("test1")
				.setSequenceSize(2).build();
		List<Message<?>> messages = new ArrayList<Message<?>>();
		messages.add(message);
		SequenceSizeCompletionStrategy completionStrategy = new SequenceSizeCompletionStrategy();
		assertFalse(completionStrategy.isComplete(messages));
	}

	@Test
	public void testCompleteList() {
		Message<String> message1 = MessageBuilder.fromPayload("test1")
				.setSequenceSize(2).build();
		Message<String> message2 = MessageBuilder.fromPayload("test2")
				.setSequenceSize(2).build();
		List<Message<?>> messages = new ArrayList<Message<?>>();
		messages.add(message1);
		messages.add(message2);
		SequenceSizeCompletionStrategy completionStrategy = new SequenceSizeCompletionStrategy();
		assertTrue(completionStrategy.isComplete(messages));
	}

	@Test
	public void testEmptyList() {
		SequenceSizeCompletionStrategy completionStrategy = new SequenceSizeCompletionStrategy();
		assertFalse(completionStrategy.isComplete(new ArrayList<Message<?>>()));
	}

	@Test
	public void testNullList() {
		SequenceSizeCompletionStrategy completionStrategy = new SequenceSizeCompletionStrategy();
		assertFalse(completionStrategy.isComplete(null));
	}

}
