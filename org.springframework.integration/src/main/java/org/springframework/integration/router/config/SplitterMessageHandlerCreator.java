/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.router.config;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.handler.AbstractMessageHandlerAdapter;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.config.AbstractMessageHandlerCreator;
import org.springframework.integration.router.SplitterMessageHandlerAdapter;

/**
 * Creates a {@link MessageHandler} adapter for splitter methods.
 * 
 * @author Mark Fisher
 */
public class SplitterMessageHandlerCreator extends AbstractMessageHandlerCreator {

	public MessageHandler doCreateHandler(Object object, Method method, Map<String, ?> attributes) {
		String outputChannelName = (String) attributes.get(AbstractMessageHandlerAdapter.OUTPUT_CHANNEL_NAME_KEY);
		if (outputChannelName == null) {
			MessageEndpoint endpointAnnotation = AnnotationUtils.findAnnotation(AopUtils.getTargetClass(object), MessageEndpoint.class);
			if (endpointAnnotation != null) {
				outputChannelName = endpointAnnotation.output();
			}
		}
		return new SplitterMessageHandlerAdapter(object, method, outputChannelName);
	}

}
