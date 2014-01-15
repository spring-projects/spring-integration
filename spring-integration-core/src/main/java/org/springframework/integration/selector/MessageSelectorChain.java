/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A message selector implementation that passes incoming messages through a
 * chain of selectors. Whether the Message is {@link #accept(Message) accepted}
 * is based upon the tallied results of the individual selectors' responses in
 * accordance with this chain's {@link VotingStrategy}.
 *
 * @author Mark Fisher
 */
public class MessageSelectorChain implements MessageSelector {

	public static enum VotingStrategy { ALL, ANY, MAJORITY, MAJORITY_OR_TIE };


	private volatile VotingStrategy votingStrategy = VotingStrategy.ALL;

	private final List<MessageSelector> selectors = new CopyOnWriteArrayList<MessageSelector>();


	/**
	 * Specify the voting strategy for this selector chain.
	 * <p>The default is {@link VotingStrategy#ALL}.
	 *
	 * @param votingStrategy The voting strategy.
	 */
	public void setVotingStrategy(VotingStrategy votingStrategy) {
		Assert.notNull(votingStrategy, "votingStrategy must not be null");
		this.votingStrategy = votingStrategy;
	}

	/**
	 * Add a selector to the end of the chain.
	 *
	 * @param selector The message selector.
	 */
	public void add(MessageSelector selector) {
		this.selectors.add(selector);
	}

	/**
	 * Add a selector to the chain at the specified index.
	 *
	 * @param index The index.
	 * @param selector The message selector.
	 */
	public void add(int index, MessageSelector selector) {
		this.selectors.add(index, selector);
	}

	/**
	 * Initialize the selector chain. Removes any existing selectors.
	 *
	 * @param selectors The message selectors.
	 */
	public void setSelectors(List<MessageSelector> selectors) {
		Assert.notEmpty(selectors, "selectors must not be empty");
		synchronized (this.selectors) {
			this.selectors.clear();
			this.selectors.addAll(selectors);
		}
	}

	/**
	 * Pass the message through the selector chain. Whether the Message is
	 * {@link #accept(Message) accepted} is based upon the tallied results of
	 * the individual selectors' responses in accordance with this chain's
	 * {@link VotingStrategy}.
	 */
	@Override
	public final boolean accept(Message<?> message) {
		int count = 0;
		int accepted = 0;
		for (MessageSelector next : this.selectors) {
			count++;
			if (next.accept(message)) {
				if (this.votingStrategy.equals(VotingStrategy.ANY)) {
					return true;
				}
				accepted++;
			}
			else if (this.votingStrategy.equals(VotingStrategy.ALL)) {
				return false;
			}
		}
		return this.decide(accepted, count);
	}

	private boolean decide(int accepted, int total) {
		if (accepted == 0) {
			return false;
		}
		switch (this.votingStrategy) {
			case ANY:
				return true;
			case ALL:
				return (accepted == total);
			case MAJORITY:
				return (2 * accepted) > total;
			case MAJORITY_OR_TIE:
				return (2 * accepted) >= total;
			default:
				throw new IllegalArgumentException("unsupported voting strategy " + this.votingStrategy);
		}
	}

}
