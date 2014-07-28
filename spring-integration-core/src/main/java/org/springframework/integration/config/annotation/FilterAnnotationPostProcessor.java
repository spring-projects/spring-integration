/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link Filter @Filter}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class FilterAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Filter> {

	public FilterAnnotationPostProcessor(ListableBeanFactory beanFactory, Environment environment) {
		super(beanFactory, environment);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
		MessageSelector selector;
		if (AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {
			Object target = this.resolveTargetBeanFromMethodWithBeanAnnotation(method);
			if (target instanceof MessageSelector) {
				selector = (MessageSelector) target;
			}
			else if (this.extractTypeIfPossible(target, MessageFilter.class) != null) {
				return (MessageHandler) target;
			}
			else {
				selector = new MethodInvokingSelector(target);
			}
		}
		else {
			Assert.isTrue(boolean.class.equals(method.getReturnType()) || Boolean.class.equals(method.getReturnType()),
					"The Filter annotation may only be applied to methods with a boolean return type.");
			selector = new MethodInvokingSelector(bean, method);
		}

		MessageFilter filter = new MessageFilter(selector);

		String discardWithinAdvice = MessagingAnnotationUtils.resolveAttribute(annotations, "discardWithinAdvice",
				String.class);
		if (StringUtils.hasText(discardWithinAdvice)) {
			discardWithinAdvice = this.environment.resolvePlaceholders(discardWithinAdvice);
			if (StringUtils.hasText(discardWithinAdvice)) {
				filter.setDiscardWithinAdvice(Boolean.parseBoolean(discardWithinAdvice));
			}
		}


		String throwExceptionOnRejection = MessagingAnnotationUtils.resolveAttribute(annotations,
				"throwExceptionOnRejection", String.class);
		if (StringUtils.hasText(throwExceptionOnRejection)) {
			String throwExceptionOnRejectionValue = this.environment.resolvePlaceholders(throwExceptionOnRejection);
			if (StringUtils.hasText(throwExceptionOnRejectionValue)) {
				filter.setThrowExceptionOnRejection(Boolean.parseBoolean(throwExceptionOnRejectionValue));
			}
		}

		String discardChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, "discardChannel", String.class);
		if (StringUtils.hasText(discardChannelName)) {
			filter.setDiscardChannelName(discardChannelName);
		}

		this.setOutputChannelIfPresent(annotations, filter);
		return filter;
	}

}
