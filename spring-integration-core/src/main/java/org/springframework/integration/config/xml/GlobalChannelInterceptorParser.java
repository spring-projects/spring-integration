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

package org.springframework.integration.config.xml;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for 'channel-interceptor' elements.
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author David Turanski
 * @since 2.0
 */
public class GlobalChannelInterceptorParser extends AbstractBeanDefinitionParser {

	private static final String BASE_PACKAGE = IntegrationNamespaceUtils.BASE_PACKAGE + ".channel.interceptor.";

	private static final String CHANNEL_NAME_PATTERN_ATTRIBUTE = "pattern";

	private static final String REF_ATTRIBUTE = "ref";

	private static final String GLOBAL_POST_PROCESSOR_CLASSNAME = "GlobalChannelInterceptorBeanPostProcessor";


	private final ManagedList<RuntimeBeanReference> globalInterceptors = new ManagedList<RuntimeBeanReference>();

	private volatile boolean postProcessorCreated;


	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		this.createAndRegisterGlobalPostProcessorIfNecessary(parserContext);
		BeanDefinitionBuilder globalChannelInterceptorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				BASE_PACKAGE + "GlobalChannelInterceptorWrapper");
		Object childBeanDefinition = getBeanDefinitionBuilderConstructorValue(element, parserContext);
		globalChannelInterceptorBuilder.addConstructorArgValue(childBeanDefinition);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(globalChannelInterceptorBuilder, element, "order");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(
				globalChannelInterceptorBuilder, element, CHANNEL_NAME_PATTERN_ATTRIBUTE, "patterns");
		String beanName = BeanDefinitionReaderUtils.generateBeanName(
				globalChannelInterceptorBuilder.getBeanDefinition(), parserContext.getRegistry());
		parserContext.registerBeanComponent(new BeanComponentDefinition(globalChannelInterceptorBuilder.getBeanDefinition(), beanName));
		this.globalInterceptors.add(new RuntimeBeanReference(beanName));
		return null;
	}

	private void createAndRegisterGlobalPostProcessorIfNecessary(ParserContext parserContext) {
		if (!this.postProcessorCreated) {
			BeanDefinitionBuilder postProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					BASE_PACKAGE + GLOBAL_POST_PROCESSOR_CLASSNAME);
			postProcessorBuilder.addConstructorArgValue(this.globalInterceptors);
			BeanDefinition beanDef = postProcessorBuilder.getBeanDefinition();
			String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDef, parserContext.getRegistry());
			parserContext.registerBeanComponent(new BeanComponentDefinition(beanDef, beanName));
			this.postProcessorCreated = true;
		}
	}

	protected Object getBeanDefinitionBuilderConstructorValue(Element element, ParserContext parserContext) {
		BeanComponentDefinition interceptorBeanDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		if (interceptorBeanDefinition != null) {
			return interceptorBeanDefinition;
		}
		String beanName = null;
		if (element.hasAttribute(REF_ATTRIBUTE)) {
			beanName = element.getAttribute(REF_ATTRIBUTE);
		}
		else {	
			List<Element> els = DomUtils.getChildElements(element);
			if (els.isEmpty()) {
				parserContext.getReaderContext().error("child BeanDefinition must not be null", element);
			}
			else {
				Element child = els.get(0);
				if ("wire-tap".equals(child.getLocalName())) {
					beanName = new WireTapParser().parse(child, parserContext);
				}
				else {
					BeanDefinition beanDef = parserContext.getDelegate().parseCustomElement(child);	
					beanName = BeanDefinitionReaderUtils.generateBeanName(beanDef, parserContext.getRegistry());
				}
			}
		}
		return new RuntimeBeanReference(beanName);
	}

}
