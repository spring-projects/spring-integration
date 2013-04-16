/*
 * Copyright 2002-2013 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Post-processor for Methods annotated with {@link Filter @Filter}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class FilterAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Filter> {

	public FilterAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	protected MessageHandler createHandler(Object bean, Method method, Filter annotation) {
		Assert.isTrue(boolean.class.equals(method.getReturnType()) || Boolean.class.equals(method.getReturnType()),
				"The Filter annotation may only be applied to methods with a boolean return type.");
		MethodInvokingSelector selector = new MethodInvokingSelector(bean, method);
		MessageFilter filter = new MessageFilter(selector);
		String outputChannelName = annotation.outputChannel();
		if (StringUtils.hasText(outputChannelName)) {
			filter.setOutputChannel(this.channelResolver.resolveChannelName(outputChannelName));
		}
		String discardWithinAdvice = annotation.discardWithinAdvice();
		if (StringUtils.hasText(discardWithinAdvice)) {
			if (discardWithinAdvice.trim().equalsIgnoreCase("false")) {
				filter.setDiscardWithinAdvice(false);
			}
			else {
				Assert.isTrue(discardWithinAdvice.trim().equalsIgnoreCase("true"),
						"'discardWithinAdvice' attribute must be 'true' or 'false' if provided");
			}
		}
		return filter;
	}

}
