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

package org.springframework.integration.channel.factory;

import java.util.Comparator;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * A {@link ChannelFactory} for creating {@link PriorityChannel} instances.
 * @author Marius Bogoevici
 *
 */
public class PriorityChannelFactory extends AbstractChannelFactory {

	private int capacity = PriorityChannel.DEFAULT_CAPACITY;

	private Comparator<Message<?>> comparator;


	public void setCapacity(int capacity) {
		Assert.isTrue(capacity > 0, "capacity must be a positive value");
		this.capacity = capacity;
	}

	public void setComparator(Comparator<Message<?>> comparator) {
		this.comparator = comparator;
	}

	@Override
	protected AbstractMessageChannel createChannelInternal() {
		return new PriorityChannel(this.capacity, this.comparator);
	}

}
