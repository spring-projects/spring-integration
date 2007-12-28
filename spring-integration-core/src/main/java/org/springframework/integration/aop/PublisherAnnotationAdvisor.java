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

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.util.Assert;

/**
 * Advisor whose pointcut matches a method annotation and whose advice will
 * publish a message to the channel provided by that annotation.
 * 
 * @author Mark Fisher
 * @see Publisher
 * @see AnnotationAwareMessagePublishingInterceptor
 */
@SuppressWarnings("serial")
public class PublisherAnnotationAdvisor extends AbstractPointcutAdvisor {

	private AnnotationAwareMessagePublishingInterceptor advice;

	private AnnotationMatchingPointcut pointcut;


	public PublisherAnnotationAdvisor(ChannelRegistry channelRegistry) {
		this(Publisher.class, "channel", channelRegistry);
	}

	public PublisherAnnotationAdvisor(Class<? extends Annotation> publisherAnnotationType, String channelNameAttribute,
			ChannelRegistry channelRegistry) {
		Assert.notNull(publisherAnnotationType, "'publisherAnnotationType' must not be null");
		Assert.notNull(channelNameAttribute, "'channelNameAttribute' must not be null");
		Assert.notNull(channelRegistry, "'channelRegistry' must not be null");
		this.pointcut = AnnotationMatchingPointcut.forMethodAnnotation(publisherAnnotationType);
		this.advice = new AnnotationAwareMessagePublishingInterceptor(publisherAnnotationType, channelNameAttribute,
				channelRegistry);
	}


	public Pointcut getPointcut() {
		return this.pointcut;
	}

	public Advice getAdvice() {
		return this.advice;
	}

}
