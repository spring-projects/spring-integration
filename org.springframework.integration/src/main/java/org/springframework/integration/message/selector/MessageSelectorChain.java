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

package org.springframework.integration.message.selector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.integration.message.Message;

/**
 * A message selector implementation that passes incoming messages through a
 * chain of selectors. The chain will be broken by any selector that returns
 * <em>false</em>.
 * 
 * @author Mark Fisher
 */
public class MessageSelectorChain implements MessageSelector {

	private final List<MessageSelector> selectors = new CopyOnWriteArrayList<MessageSelector>();


	/**
	 * Add a selector to the end of the chain.
	 */
	public void add(MessageSelector selector) {
		this.selectors.add(selector);
	}

	/**
	 * Add a selector to the chain at the specified index.
	 */
	public void add(int index, MessageSelector selector) {
		this.selectors.add(index, selector);
	}

	/**
	 * Initialize the selector chain. Removes any existing selectors.
	 */
	public void setSelectors(List<MessageSelector> selectors) {
		this.selectors.clear();
		this.selectors.addAll(selectors);
	}

	/**
	 * Pass the message through the selector chain. As soon as a
	 * selector returns 'false', this method will return 'false'.
	 * If all selectors accept, this method will return 'true'. 
	 */
	public final boolean accept(Message<?> message) {
		for (MessageSelector next : this.selectors) {
			if (!next.accept(message)) {
				return false;
			}
		}
		return true;
	}

}
