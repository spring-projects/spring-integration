/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for 'channel-interceptor' elements.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class GlobalChannelInterceptorParser extends AbstractBeanDefinitionParser {

	private static final String CHANNEL_NAME_PATTERN_ATTRIBUTE = "pattern";

	private static final String REF_ATTRIBUTE = "ref";

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected boolean shouldFireEvents() {
		return false;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder globalChannelInterceptorBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(GlobalChannelInterceptorWrapper.class);
		Object childBeanDefinition = getBeanDefinitionBuilderConstructorValue(element, parserContext);
		globalChannelInterceptorBuilder.addConstructorArgValue(childBeanDefinition);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(globalChannelInterceptorBuilder, element, "order");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(globalChannelInterceptorBuilder, element,
				CHANNEL_NAME_PATTERN_ATTRIBUTE, "patterns");
		return globalChannelInterceptorBuilder.getBeanDefinition();
	}

	protected @Nullable Object getBeanDefinitionBuilderConstructorValue(Element element, ParserContext parserContext) {
		BeanComponentDefinition interceptorBeanDefinition =
				IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		if (interceptorBeanDefinition != null) {
			return interceptorBeanDefinition;
		}

		BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
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
				else if (delegate.nodeNameEquals(child, BeanDefinitionParserDelegate.REF_ELEMENT)) {
					return delegate.parsePropertySubElement(child, null);
				}
				else {
					BeanDefinition beanDef = delegate.parseCustomElement(child);
					beanName = BeanDefinitionReaderUtils.generateBeanName(Objects.requireNonNull(beanDef), // NOSONAR never null
							parserContext.getRegistry());
				}
			}
		}
		return new RuntimeBeanReference(Objects.requireNonNull(beanName));
	}

}
