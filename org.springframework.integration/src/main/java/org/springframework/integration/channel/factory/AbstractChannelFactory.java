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

import java.util.List;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;

/**
 * Base class for {@link ChannelFactory} implementations. Subclasses should
 * override {@literal createChannelInternal()}.
 * 
 * @author Marius Bogoevici
 */
public abstract class AbstractChannelFactory implements ChannelFactory {

	public AbstractChannelFactory() {
		super();
	}

	public final MessageChannel getChannel(String name, List<ChannelInterceptor> interceptors) {
		AbstractMessageChannel channel = createChannelInternal();
		if (null != interceptors) {
			channel.setInterceptors(interceptors);
		}
		if (name != null && channel.getName() == null) {
			channel.setBeanName(name);
		}
		return channel;
	}

	/**
	 * Factory method to be overridden by subclasses. It assumes that subclasses will return
	 * subclasses of AbstractMessageChannel.
	 */
	protected abstract AbstractMessageChannel createChannelInternal();

}
