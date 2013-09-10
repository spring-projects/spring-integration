/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.annotation.ReleaseStrategy;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Post-processor for the {@link Aggregator @Aggregator} annotation.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class AggregatorAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Aggregator> {

	public AggregatorAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, Aggregator annotation) {
		MethodInvokingMessageGroupProcessor processor = new MethodInvokingMessageGroupProcessor(bean, method);
		MethodInvokingReleaseStrategy releaseStrategy = getReleaseStrategy(bean);
		MethodInvokingCorrelationStrategy correlationStrategy = getCorrelationStrategy(bean);
		AggregatingMessageHandler handler = new AggregatingMessageHandler(processor, new SimpleMessageStore(), correlationStrategy, releaseStrategy);
		String discardChannelName = annotation.discardChannel();
		if (StringUtils.hasText(discardChannelName)) {
			MessageChannel discardChannel = this.channelResolver.resolveDestination(discardChannelName);
			Assert.notNull(discardChannel, "failed to resolve discardChannel '" + discardChannelName + "'");
			handler.setDiscardChannel(discardChannel);
		}
		String outputChannelName = annotation.outputChannel();
		if (StringUtils.hasText(outputChannelName)) {
			handler.setOutputChannel(this.channelResolver.resolveDestination(outputChannelName));
		}
		handler.setSendTimeout(annotation.sendTimeout());
		handler.setSendPartialResultOnExpiry(annotation.sendPartialResultsOnExpiry());
		handler.setBeanFactory(this.beanFactory);
		handler.afterPropertiesSet();
		return handler;
	}

	private MethodInvokingReleaseStrategy getReleaseStrategy(final Object bean) {
    	final AtomicReference<MethodInvokingReleaseStrategy> reference = new AtomicReference<MethodInvokingReleaseStrategy>();
		ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.getAnnotation(method, ReleaseStrategy.class);
				if (annotation != null) {
	                  reference.set(new MethodInvokingReleaseStrategy(bean, method));
				}
			}
		});
        return reference.get();
	}

    private MethodInvokingCorrelationStrategy getCorrelationStrategy(final Object bean) {
    	final AtomicReference<MethodInvokingCorrelationStrategy> reference = new AtomicReference<MethodInvokingCorrelationStrategy>();
        ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                Annotation annotation = AnnotationUtils.getAnnotation(method, CorrelationStrategy.class);
                if (annotation != null) {
                    reference.set(new MethodInvokingCorrelationStrategy(bean, method));
                }
            }
        });
        return reference.get();
    }

}
