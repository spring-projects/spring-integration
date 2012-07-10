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

import org.w3c.dom.Element;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base class for channel parsers.
 * 
 * @author Mark Fisher
 * @author Dave Syer
 */
public abstract class AbstractChannelParser extends AbstractBeanDefinitionParser {

	@SuppressWarnings("rawtypes")
	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = this.buildBeanDefinition(element, parserContext);
		ManagedList interceptors = null;
		Element interceptorsElement = DomUtils.getChildElementByTagName(element, "interceptors");
		if (interceptorsElement != null) {
			ChannelInterceptorParser interceptorParser = new ChannelInterceptorParser();
			interceptors = interceptorParser.parseInterceptors(interceptorsElement, parserContext);
		}
		if (interceptors == null) {
			interceptors = new ManagedList();
		}
		String datatypeAttr = element.getAttribute("datatype");
		if (StringUtils.hasText(datatypeAttr)) {
			builder.addPropertyValue("datatypes", datatypeAttr);
		}
		builder.addPropertyValue("interceptors", interceptors);
		AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		String scopeAttr = element.getAttribute("scope");
		if (StringUtils.hasText(scopeAttr)) {
			builder.setScope(scopeAttr);
		}
		beanDefinition.setSource(parserContext.extractSource(element));
		return beanDefinition;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#registerBeanDefinition(org.springframework.beans.factory.config.BeanDefinitionHolder, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	@Override
	protected void registerBeanDefinition(BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {
		String scope = definition.getBeanDefinition().getScope();
		if (!AbstractBeanDefinition.SCOPE_DEFAULT.equals(scope) && !AbstractBeanDefinition.SCOPE_SINGLETON.equals(scope) && !AbstractBeanDefinition.SCOPE_PROTOTYPE.equals(scope)) {
			definition = ScopedProxyUtils.createScopedProxy(definition, registry, false);
		}
		super.registerBeanDefinition(definition, registry);
	}

	/**
	 * Subclasses must implement this method to create the bean definition.
	 * The class must be defined, and any implementation-specific constructor
	 * arguments or properties should be configured. This base class will
	 * configure the interceptors including the 'datatype' interceptor if
	 * the 'datatype' attribute is defined on the channel element.
	 */
	protected abstract BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext);

	protected void setMaxSubscribersProperty(ParserContext parserContext, BeanDefinitionBuilder builder, Element element, String channelInitializerPropertyName) {
		String maxSubscribers = element.getAttribute("max-subscribers");
		if (!StringUtils.hasText(maxSubscribers)) {
			maxSubscribers = getDefaultMaxSubscribers(parserContext, channelInitializerPropertyName);
		}
		if (StringUtils.hasText(maxSubscribers)) {
			builder.addPropertyValue("maxSubscribers", maxSubscribers);
		}
	}

	protected String getDefaultMaxSubscribers(ParserContext parserContext, String channelInitializerPropertyName) {
		String maxSubscribers = null;
		BeanDefinition channelInitializer = parserContext.getRegistry().getBeanDefinition(
				AbstractIntegrationNamespaceHandler.CHANNEL_INITIALIZER_BEAN_NAME);
		if (channelInitializer != null) {
			PropertyValues propertyValues = channelInitializer.getPropertyValues();
			if (propertyValues != null) {
				PropertyValue propertyValue = propertyValues
						.getPropertyValue(channelInitializerPropertyName);
				if (propertyValue != null) {
					Object propertyValueValue = propertyValue.getValue();
					if (propertyValueValue instanceof TypedStringValue) {
						maxSubscribers = ((TypedStringValue) propertyValueValue).getValue();
					}
					else if (propertyValueValue instanceof String) {
						maxSubscribers = (String) propertyValueValue;
					}
				}
			}
		}
		return maxSubscribers;
	}

}
