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

package org.springframework.integration.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.channel.ChannelResolver;
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

	private ChannelResolver channelResolver;


	public AnnotationAwareMessagePublishingInterceptor(Class<? extends Annotation> publisherAnnotationType,
			String channelAttributeName, ChannelResolver channelResolver) {
		Assert.notNull(publisherAnnotationType, "'publisherAnnotationType' must not be null");
		Assert.notNull(channelAttributeName, "'channelAttributeName' must not be null");
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.publisherAnnotationType = publisherAnnotationType;
		this.channelAttributeName = channelAttributeName;
		this.channelResolver = channelResolver;
	}


	@Override
	protected MessageChannel resolveChannel(MethodInvocation invocation) {
		String channelName = this.extractAnnotationValue(invocation, this.channelAttributeName, String.class);
		if (channelName != null) {
			MessageChannel channel = this.channelResolver.resolveChannelName(channelName);
			if (channel != null) {
				return channel;
			}
		}
		return super.resolveChannel(invocation);
	}

	@Override
	protected PayloadType determinePayloadType(MethodInvocation invocation) {
		PayloadType payloadType = this.extractAnnotationValue(invocation, "payloadType", PayloadType.class);
		if (payloadType != null) {
			return payloadType;
		}
		return super.determinePayloadType(invocation);
	}

	@SuppressWarnings("unchecked")
	private <T> T extractAnnotationValue(MethodInvocation invocation, String attributeName, Class<T> type) {
		Class<?> targetClass = AopUtils.getTargetClass(invocation.getThis());
		Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
		Annotation annotation = AnnotationUtils.getAnnotation(method, this.publisherAnnotationType);
		if (annotation != null) {
			Object value = AnnotationUtils.getValue(annotation, attributeName);
			if (value != null && type.isAssignableFrom(value.getClass())) {
				return (T) value;
			}
		}
		return null;
	}

}
