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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.interceptor.ConcurrencyInterceptor;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>concurrency-interceptor</em>. element.
 *
 * @author Mark Fisher
 */
public class ConcurrencyInterceptorParser implements BeanDefinitionRegisteringParser {

	public String parse(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ConcurrencyInterceptor.class);
		String taskExecutorRef = element.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutorRef)) {
			if (element.getAttributes().getLength() != 1) {
				parserContext.getReaderContext().error("No other attributes are permitted when "
					+ "specifying a 'task-executor' reference on the <concurrency-interceptor/> element.",
					parserContext.extractSource(element));
			}
			builder.addConstructorArgReference(taskExecutorRef);
		}
		else {
			ConcurrencyPolicy policy = this.parseConcurrencyPolicy(element);
			builder.addConstructorArgValue(policy);
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private ConcurrencyPolicy parseConcurrencyPolicy(Element element) {
		ConcurrencyPolicy policy = new ConcurrencyPolicy();
		String coreSize = element.getAttribute("core");
		String maxSize = element.getAttribute("max");
		String queueCapacity = element.getAttribute("queue-capacity");
		String keepAlive = element.getAttribute("keep-alive");
		if (StringUtils.hasText(coreSize)) {
			policy.setCoreSize(Integer.parseInt(coreSize));
		}
		if (StringUtils.hasText(maxSize)) {
			policy.setMaxSize(Integer.parseInt(maxSize));
		}
		if (StringUtils.hasText(queueCapacity)) {
			policy.setQueueCapacity(Integer.parseInt(queueCapacity));
		}
		if (StringUtils.hasText(keepAlive)) {
			policy.setKeepAliveSeconds(Integer.parseInt(keepAlive));
		}
		return policy;
	}

}
