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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.SourceEndpoint;
import org.springframework.integration.endpoint.TargetEndpoint;
import org.springframework.integration.handler.MethodInvokingTarget;
import org.springframework.integration.message.MethodInvokingSource;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the <channel-adapter/> element.
 * 
 * @author Mark Fisher
 */
public class ChannelAdapterParser extends AbstractSimpleBeanDefinitionParser {

	protected final Class<?> getBeanClass(Element element) {
		boolean hasSource = StringUtils.hasText(element.getAttribute("source"));
		boolean hasTarget = StringUtils.hasText(element.getAttribute("target"));
		if (!(hasSource ^ hasTarget)) {
			throw new ConfigurationException("exactly one of 'source' or 'target' is required");
		}
		return hasSource ? SourceEndpoint.class : TargetEndpoint.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected boolean isEligibleAttribute(String name) {
		return (!"source".equals(name)
				&& !"target".equals(name)
				&& !"channel".equals(name)
				&& super.isEligibleAttribute(name));
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String source = element.getAttribute("source");
		String target = element.getAttribute("target");
		String channel = element.getAttribute("channel");
		if (!StringUtils.hasText(channel)) {
			throw new ConfigurationException("'channel' is required");
		}
		boolean isSource = StringUtils.hasText(source); 
		if (isSource) {
			builder.addConstructorArgReference(this.resolveConstructorArgument(
					source, MethodInvokingSource.class, element, parserContext));
			builder.addPropertyValue("outputChannelName", channel);
		}
		else {
			builder.addConstructorArgReference(this.resolveConstructorArgument(
					target, MethodInvokingTarget.class, element, parserContext));
			builder.addPropertyValue("inputChannelName", channel);
		}
		Element scheduleElement = DomUtils.getChildElementByTagName(element, "schedule");
		if (scheduleElement != null) {
			builder.addPropertyValue("schedule", this.parseSchedule(scheduleElement));
		}
		Element interceptorsElement = DomUtils.getChildElementByTagName(element, "interceptors");
		if (interceptorsElement != null) {
			EndpointInterceptorParser parser = new EndpointInterceptorParser();
			ManagedList interceptors = parser.parseEndpointInterceptors(interceptorsElement, parserContext);
			builder.addPropertyValue("interceptors", interceptors);
		}
	}

	private String resolveConstructorArgument(String ref, Class<?> targetClass, Element element, ParserContext parserContext) {
		String method = element.getAttribute("method");
		if (StringUtils.hasText(method)) {
			BeanDefinition adapterDef = new RootBeanDefinition(targetClass);
			adapterDef.getPropertyValues().addPropertyValue("object", new RuntimeBeanReference(ref));
			adapterDef.getPropertyValues().addPropertyValue("methodName", method);
			String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDef);
			parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, adapterBeanName));
			return adapterBeanName;			
		}
		return ref;
	}

	/**
	 * Subclasses may override this method to control the creation of the {@link Schedule}. The default
	 * implementation creates a {@link PollingSchedule} instance based on the provided "period" attribute. 
	 */
	protected Schedule parseSchedule(Element element) {
		String period = element.getAttribute("period");
		if (!StringUtils.hasText(period)) {
			throw new ConfigurationException("The 'period' attribute is required for the 'schedule' element.");
		}
		PollingSchedule schedule = new PollingSchedule(Long.valueOf(period));
		return schedule;
	}

}
