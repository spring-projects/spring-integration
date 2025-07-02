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

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class for channel parsers.
 *
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class AbstractChannelParser extends AbstractBeanDefinitionParser {

	@SuppressWarnings("rawtypes")
	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = this.buildBeanDefinition(element, parserContext);
		AbstractBeanDefinition beanDefinition = Objects.requireNonNull(builder).getBeanDefinition();
		Element interceptorsElement = DomUtils.getChildElementByTagName(element, "interceptors");
		String datatypeAttr = element.getAttribute("datatype");
		String messageConverter = element.getAttribute("message-converter");
		if (!FixedSubscriberChannel.class.getName().equals(builder.getBeanDefinition().getBeanClassName())) {
			ManagedList interceptors = null;
			if (interceptorsElement != null) {
				ChannelInterceptorParser interceptorParser = new ChannelInterceptorParser();
				interceptors = interceptorParser.parseInterceptors(interceptorsElement, parserContext);
			}
			if (interceptors == null) {
				interceptors = new ManagedList();
			}
			if (StringUtils.hasText(datatypeAttr)) {
				builder.addPropertyValue("datatypes", datatypeAttr);
			}
			if (StringUtils.hasText(messageConverter)) {
				builder.addPropertyReference("messageConverter", messageConverter);
			}
			builder.addPropertyValue("interceptors", interceptors);
			String scopeAttr = element.getAttribute("scope");
			if (StringUtils.hasText(scopeAttr)) {
				builder.setScope(scopeAttr);
			}
		}
		else {
			if (interceptorsElement != null) {
				parserContext.getReaderContext().error("Cannot have interceptors when 'fixed-subscriber=\"true\"'", element);
			}
			if (StringUtils.hasText(datatypeAttr)) {
				parserContext.getReaderContext().error("Cannot have 'datatype' when 'fixed-subscriber=\"true\"'", element);
			}
			if (StringUtils.hasText(messageConverter)) {
				parserContext.getReaderContext().error("Cannot have 'message-converter' when 'fixed-subscriber=\"true\"'", element);
			}
		}
		beanDefinition.setSource(parserContext.extractSource(element));
		return beanDefinition;
	}

	@Override
	protected void registerBeanDefinition(BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {
		String scope = definition.getBeanDefinition().getScope();
		if (!AbstractBeanDefinition.SCOPE_DEFAULT.equals(scope) && !AbstractBeanDefinition.SCOPE_SINGLETON.equals(scope)
				&& !AbstractBeanDefinition.SCOPE_PROTOTYPE.equals(scope)) {
			super.registerBeanDefinition(ScopedProxyUtils.createScopedProxy(definition, registry, false), registry);
		}
		else {
			super.registerBeanDefinition(definition, registry);
		}
	}

	/**
	 * Subclasses must implement this method to create the bean definition.
	 * The class must be defined, and any implementation-specific constructor
	 * arguments or properties should be configured. This base class will
	 * configure the interceptors including the 'datatype' interceptor if
	 * the 'datatype' attribute is defined on the channel element.
	 *
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @return The bean definition builder.
	 */
	protected abstract @Nullable BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext);

}
