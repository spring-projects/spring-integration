/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config;

import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Splitter.
 * 
 * @author Mark Fisher
 */
public class SplitterFactoryBean extends AbstractMessageHandlerFactoryBean {

	@Override
	protected MessageHandler createHandler(Object targetObject, String targetMethodName) {
		if (targetObject instanceof AbstractMessageSplitter) {
			return (AbstractMessageSplitter) targetObject;
		}
		return (StringUtils.hasText(targetMethodName))
				? new MethodInvokingSplitter(targetObject, targetMethodName)
				: new MethodInvokingSplitter(targetObject);
	}

	@Override
	protected MessageHandler createDefaultHandler() {
		return new DefaultMessageSplitter();
	}

}
