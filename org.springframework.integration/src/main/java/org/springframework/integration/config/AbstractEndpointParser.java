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

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class parser for elements that create Message Endpoints.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpointParser extends AbstractSingleBeanDefinitionParser {

	protected static final String REF_ATTRIBUTE = "ref";

	protected static final String METHOD_ATTRIBUTE = "method";

	protected static final String INPUT_CHANNEL_ATTRIBUTE = "input-channel";

	protected static final String OUTPUT_CHANNEL_ATTRIBUTE = "output-channel";

	private static final String POLLER_ELEMENT = "poller";

	private static final String SELECTOR_ATTRIBUTE = "selector";

	private static final String ERROR_HANDLER_ATTRIBUTE = "error-handler";

	private static final String INTERCEPTORS_ELEMENT = "interceptors";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return this.getEndpointClass();
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String ref = element.getAttribute(REF_ATTRIBUTE);
		if (!StringUtils.hasText(ref)) {
			throw new ConfigurationException("The '" + REF_ATTRIBUTE + "' attribute is required.");
		}
		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			String adapterBeanName = this.parseAdapter(ref, method, element, parserContext);
			builder.addConstructorArgReference(adapterBeanName);
		}
		else {
			builder.addConstructorArgReference(ref);
		}
		String inputChannel = element.getAttribute(INPUT_CHANNEL_ATTRIBUTE);
		if (!StringUtils.hasText(inputChannel)) {
			throw new ConfigurationException("the '" + INPUT_CHANNEL_ATTRIBUTE + "' attribute is required");
		}
		Element pollerElement = DomUtils.getChildElementByTagName(element, POLLER_ELEMENT);
		if (pollerElement != null) {
			IntegrationNamespaceUtils.configureSchedule(pollerElement, builder);
			Element txElement = DomUtils.getChildElementByTagName(pollerElement, "transactional");
			if (txElement != null) {
				IntegrationNamespaceUtils.configureTransactionAttributes(txElement, builder);
			}
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, pollerElement, "task-executor");
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, INPUT_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, OUTPUT_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, SELECTOR_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, ERROR_HANDLER_ATTRIBUTE);
		Element interceptorsElement = DomUtils.getChildElementByTagName(element, INTERCEPTORS_ELEMENT);
		if (interceptorsElement != null) {
			EndpointInterceptorParser parser = new EndpointInterceptorParser();
			ManagedList interceptors = parser.parseInterceptors(interceptorsElement, parserContext);
			builder.addPropertyValue("interceptors", interceptors);
		}
		this.postProcess(element, parserContext, builder);
	}

	private String parseAdapter(String ref, String method, Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(this.getMethodInvokingAdapterClass());
		builder.addConstructorArgReference(ref);
		builder.addConstructorArgValue(method);
		String adapterBeanName = BeanDefinitionReaderUtils.generateBeanName(builder.getBeanDefinition(), parserContext.getRegistry());
		BeanDefinitionHolder holder = new BeanDefinitionHolder(builder.getBeanDefinition(), adapterBeanName);
		parserContext.registerBeanComponent(new BeanComponentDefinition(holder));
		return adapterBeanName;
	}

	/**
	 * Subclasses may implement this method to provide additional configuration.
	 */
	protected void postProcess(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
	}

	protected abstract Class<? extends MessageEndpoint> getEndpointClass();

	protected abstract Class<?> getMethodInvokingAdapterClass();

}
