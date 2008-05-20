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

package org.springframework.integration.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.util.Assert;

/**
 * {@link MessagePublishingInterceptor} that resolves the channel from the
 * publisher annotation of the invoked method.
 * 
 * @author Mark Fisher
 */
public class AnnotationAwareMessagePublishingInterceptor extends MessagePublishingInterceptor {

	private Class<? extends Annotation> publisherAnnotationType;

	private String channelAttributeName;

	private ChannelRegistry channelRegistry;


	public AnnotationAwareMessagePublishingInterceptor(Class<? extends Annotation> publisherAnnotationType,
			String channelAttributeName, ChannelRegistry channelRegistry) {
		Assert.notNull(publisherAnnotationType, "'publisherAnnotationType' must not be null");
		Assert.notNull(channelAttributeName, "'channelAttributeName' must not be null");
		Assert.notNull(channelRegistry, "'channelRegistry' must not be null");
		this.publisherAnnotationType = publisherAnnotationType;
		this.channelAttributeName = channelAttributeName;
		this.channelRegistry = channelRegistry;
	}


	@Override
	protected MessageChannel resolveChannel(MethodInvocation invocation) {
		Class<?> targetClass = AopUtils.getTargetClass(invocation.getThis());
		Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
		Annotation annotation = AnnotationUtils.getAnnotation(method, this.publisherAnnotationType);
		if (annotation != null) {
			String channelName = (String) AnnotationUtils.getValue(annotation, this.channelAttributeName);
			if (channelName != null) {
				MessageChannel channel = this.channelRegistry.lookupChannel(channelName);
				if (channel != null) {
					return channel;
				}
			}
		}
		return super.resolveChannel(invocation);
	}

}
