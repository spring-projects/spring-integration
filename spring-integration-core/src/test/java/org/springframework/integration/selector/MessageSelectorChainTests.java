/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.selector;

import org.junit.Test;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(chain.accept(message)).isTrue();
	}

	@Test
	public void anyStrategyRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.ANY);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		assertThat(chain.accept(message)).isFalse();
	}

	@Test
	public void allStrategyAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.ALL);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		assertThat(chain.accept(message)).isTrue();
	}

	@Test
	public void allStrategyRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.ALL);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		assertThat(chain.accept(message)).isFalse();
	}

	@Test
	public void majorityOrTieStrategyWithOddNumberAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY_OR_TIE);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertThat(chain.accept(message)).isTrue();
	}

	@Test
	public void majorityOrTieStrategyWithEvenNumberAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY_OR_TIE);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		assertThat(chain.accept(message)).isTrue();
	}

	@Test
	public void majorityOrTieStrategyWithOddNumberRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY_OR_TIE);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertThat(chain.accept(message)).isFalse();
	}

	@Test
	public void majorityOrTieStrategyWithEvenNumberRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY_OR_TIE);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(false));
		assertThat(chain.accept(message)).isFalse();
	}

	@Test
	public void majorityStrategyWithOddNumberAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertThat(chain.accept(message)).isTrue();
	}

	@Test
	public void majorityStrategyWithEvenNumberAccepts() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY);
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		assertThat(chain.accept(message)).isTrue();
	}

	@Test
	public void majorityStrategyWithOddNumberRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertThat(chain.accept(message)).isFalse();
	}

	@Test
	public void majorityStrategyWithEvenNumberRejects() {
		MessageSelectorChain chain = new MessageSelectorChain();
		chain.setVotingStrategy(MessageSelectorChain.VotingStrategy.MAJORITY);
		chain.add(new TestSelector(false));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(true));
		chain.add(new TestSelector(false));
		assertThat(chain.accept(message)).isFalse();
	}

	private static class TestSelector implements MessageSelector {

		private final boolean accept;

		TestSelector(boolean accept) {
			this.accept = accept;
		}

		@Override
		public boolean accept(Message<?> message) {
			return this.accept;
		}

	}

}
