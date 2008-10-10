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

package org.springframework.integration.config.xml;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Mark Fisher
 */
public class PoolExecutorParser extends AbstractSimpleBeanDefinitionParser {

	private static final String REJECTION_POLICY_ATTRIBUTE = "rejection-policy";

	private static final String CORE_SIZE_ATTRIBUTE = "core-size";

	private static final String MAX_SIZE_ATTRIBUTE = "max-size";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return ThreadPoolTaskExecutor.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !REJECTION_POLICY_ATTRIBUTE.equals(attributeName)
				&& !CORE_SIZE_ATTRIBUTE.equals(attributeName)
				&& !MAX_SIZE_ATTRIBUTE.equals(attributeName)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
		String policyName = element.getAttribute(REJECTION_POLICY_ATTRIBUTE);
		builder.addPropertyValue("rejectedExecutionHandler", createRejectedExecutionHandler(policyName));
		builder.addPropertyValue("corePoolSize", element.getAttribute(CORE_SIZE_ATTRIBUTE));
		builder.addPropertyValue("maxPoolSize", element.getAttribute(MAX_SIZE_ATTRIBUTE));
	}

	private RejectedExecutionHandler createRejectedExecutionHandler(String policyName) {
		if (policyName.equals("ABORT")) {
			return new ThreadPoolExecutor.AbortPolicy();
		}
		if (policyName.equals("DISCARD")) {
			return new ThreadPoolExecutor.DiscardPolicy();
		}
		if (policyName.equals("DISCARD_OLDEST")) {
			return new ThreadPoolExecutor.DiscardOldestPolicy();
		}
		return new ThreadPoolExecutor.CallerRunsPolicy();
	}

}
