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

package org.springframework.integration.channel.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.ThreadLocalChannel;

/**
 * Parser for the &lt;thread-local-channel&gt; element.
 * 
 * @author Mark Fisher
 */
public class ThreadLocalChannelParser extends AbstractChannelParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return ThreadLocalChannel.class;
	}

	@Override
	protected void configureConstructorArgs(BeanDefinitionBuilder builder, Element element, DispatcherPolicy dispatcherPolicy) {
	}

}
