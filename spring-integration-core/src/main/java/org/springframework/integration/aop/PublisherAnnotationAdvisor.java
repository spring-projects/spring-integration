/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.context.BeanFactoryChannelResolver;
import org.springframework.integration.core.MessageChannel;

/**
 * An advisor that will apply the {@link MessagePublishingInterceptor} to any
 * methods containing the provided annotations. If no annotations are provided,
 * the default will be {@link Publisher @Publisher}.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
@SuppressWarnings("serial")
public class PublisherAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	private final Set<Class<? extends Annotation>> publisherAnnotationTypes;

	private final MessagePublishingInterceptor interceptor;


	public PublisherAnnotationAdvisor(Class<? extends Annotation> ... publisherAnnotationTypes) {
		this.publisherAnnotationTypes = new HashSet<Class<? extends Annotation>>(Arrays.asList(publisherAnnotationTypes));
		ExpressionSource source = new MethodAnnotationExpressionSource(this.publisherAnnotationTypes);
		this.interceptor = new MessagePublishingInterceptor(source);
	}

	@SuppressWarnings("unchecked")
	public PublisherAnnotationAdvisor() {
		this(Publisher.class);
	}


	public void setDefaultChannel(MessageChannel defaultChannel) {
		this.interceptor.setDefaultChannel(defaultChannel);
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.interceptor.setChannelResolver(new BeanFactoryChannelResolver(beanFactory));
	}

	public Advice getAdvice() {
		return this.interceptor;
	}

	public Pointcut getPointcut() {
		return this.buildPointcut();
	}

	private Pointcut buildPointcut() {
		ComposablePointcut result = null;
		for (Class<? extends Annotation> publisherAnnotationType : this.publisherAnnotationTypes) {
			Pointcut cpc = new AnnotationMatchingPointcut(publisherAnnotationType, true);
			Pointcut mpc = new AnnotationMatchingPointcut(null, publisherAnnotationType);
			if (result == null) {
				result = new ComposablePointcut(cpc).union(mpc);
			}
			else {
				result.union(cpc).union(mpc);
			}
		}
		return result;
	}

}
