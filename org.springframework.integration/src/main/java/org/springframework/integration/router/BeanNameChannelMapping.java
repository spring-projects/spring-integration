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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.util.Assert;

/**
 * An implementation of the ChannelMapping strategy that retrieves a
 * MessageChannel instance from the {@link BeanFactory} using the provided
 * name.
 * 
 * @author Mark Fisher
 */
public class BeanNameChannelMapping implements ChannelMapping, BeanFactoryAware {

	private volatile BeanFactory beanFactory;


	/**
	 * Constructor for use within a context where the BeanFactory will be
	 * injected via the {@link #setBeanFactory(BeanFactory)} callback method.
	 */
	public BeanNameChannelMapping() {
	}

	/**
	 * Constructor for programmatic creation from within other components that
	 * already have access to the {@link BeanFactory}.
	 */
	public BeanNameChannelMapping(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public MessageChannel getChannel(String name) {
		Assert.state(this.beanFactory != null, "beanFactory must not be null");
		return (MessageChannel) this.beanFactory.getBean(name, MessageChannel.class);
	}

}
