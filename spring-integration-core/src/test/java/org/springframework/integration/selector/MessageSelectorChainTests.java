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

package org.springframework.integration.selector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class MessageSelectorChainTests {

	private final Message<?> message = new GenericMessage<String>("test");


	@Test
	public void anyStrategyAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.ANY);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertTrue(chain.accept(message));
	}

	@Test
	public void anyStrategyRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.ANY);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		assertFalse(chain.accept(message));
	}

	@Test
	public void allStrategyAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.ALL);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		assertTrue(chain.accept(message));
	}

	@Test
	public void allStrategyRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.ALL);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		assertFalse(chain.accept(message));
	}

	@Test
	public void majorityOrTieStrategyWithOddNumberAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY_OR_TIE);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertTrue(chain.accept(message));
	}

	@Test
	public void majorityOrTieStrategyWithEvenNumberAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY_OR_TIE);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		assertTrue(chain.accept(message));
	}

	@Test
	public void majorityOrTieStrategyWithOddNumberRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY_OR_TIE);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertFalse(chain.accept(message));
	}

	@Test
	public void majorityOrTieStrategyWithEvenNumberRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY_OR_TIE);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		assertFalse(chain.accept(message));
	}

	@Test
	public void majorityStrategyWithOddNumberAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertTrue(chain.accept(message));
	}

	@Test
	public void majorityStrategyWithEvenNumberAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		assertTrue(chain.accept(message));
	}

	@Test
	public void majorityStrategyWithOddNumberRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertFalse(chain.accept(message));
	}

	@Test
	public void majorityStrategyWithEvenNumberRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertFalse(chain.accept(message));
	}


	private static class TestSelector implements MessageSelector {

		private final boolean accept;

		private TestSelector(boolean accept) {
			this.accept = accept;
		}

		public boolean accept(Message<?> message) {
			return this.accept;
		}
	}

}
