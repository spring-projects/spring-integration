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

package org.springframework.integration.channel;

import java.util.List;

import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.BlockingTarget;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * Base channel interface defining common behavior for message sending and receiving.
 * 
 * @author Mark Fisher
 */
public interface MessageChannel extends BlockingSource, BlockingTarget {

	static final int DEFAULT_CAPACITY = 100;


	/**
	 * Return the name of this channel.
	 */
	String getName();

	/**
	 * Set the name of this channel.
	 */
	void setName(String name);

	/**
	 * Return this channel's dispatcher policy
	 */
	DispatcherPolicy getDispatcherPolicy();

	/**
	 * Remove all {@link Message Messages} from this channel.
	 */
	List<Message<?>> clear();

	/**
	 * Remove any {@link Message Messages} that are not accepted by the provided selector.
	 */
	List<Message<?>> purge(MessageSelector selector);

}
