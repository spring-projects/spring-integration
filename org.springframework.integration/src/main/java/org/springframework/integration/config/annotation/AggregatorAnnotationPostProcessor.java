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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.aggregator.AbstractMessageAggregator;
import org.springframework.integration.aggregator.CompletionStrategyAdapter;
import org.springframework.integration.aggregator.MethodInvokingAggregator;
import org.springframework.integration.aggregator.CorrelationStrategyAdapter;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CompletionStrategy;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Post-processor for the {@link Aggregator @Aggregator} annotation.
 * 
 * @author Mark Fisher
 */
public class AggregatorAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Aggregator> {

	public AggregatorAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, Aggregator annotation) {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(bean, method);
		this.configureCompletionStrategy(bean, aggregator);
        this.configureCorrelationStrategy(bean, aggregator);
		String discardChannelName = annotation.discardChannel();
		if (StringUtils.hasText(discardChannelName)) {
			MessageChannel discardChannel = this.channelResolver.resolveChannelName(discardChannelName);
			Assert.notNull(discardChannel, "failed to resolve discardChannel '" + discardChannelName + "'");
			aggregator.setDiscardChannel(discardChannel);
		}
		String outputChannelName = annotation.outputChannel();
		if (StringUtils.hasText(outputChannelName)) {
			aggregator.setOutputChannel(this.channelResolver.resolveChannelName(outputChannelName));
		}
		aggregator.setSendTimeout(annotation.sendTimeout());
		aggregator.setSendPartialResultOnTimeout(annotation.sendPartialResultsOnTimeout());
		aggregator.setReaperInterval(annotation.reaperInterval());
		aggregator.setTimeout(annotation.timeout());
		aggregator.setTrackedCorrelationIdCapacity(annotation.trackedCorrelationIdCapacity());
		aggregator.setBeanFactory(this.beanFactory);
		aggregator.afterPropertiesSet();
		return aggregator;
	}

	private void configureCompletionStrategy(final Object bean, final AbstractMessageAggregator aggregator) {
		ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.getAnnotation(method, CompletionStrategy.class);
				if (annotation != null) {
					aggregator.setCompletionStrategy(new CompletionStrategyAdapter(bean, method));
				}
			}
		});
	}

    private void configureCorrelationStrategy(final Object bean, final AbstractMessageAggregator aggregator) {
        ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                Annotation annotation = AnnotationUtils.getAnnotation(method, CorrelationStrategy.class);
                if (annotation != null) {
                    aggregator.setCorrelationStrategy(new CorrelationStrategyAdapter(bean, method));
                }
            }
        });
    }

}
