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

package org.springframework.integration.ws.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.integration.ws.adapter.MarshallingWebServiceTargetAdapter;
import org.springframework.integration.ws.adapter.SimpleWebServiceTargetAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;ws-target/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class WebServiceTargetAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return DefaultMessageEndpoint.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String channel = element.getAttribute("channel");
		String uri = element.getAttribute("uri");
		if (!StringUtils.hasText(channel)) {
			throw new ConfigurationException("The 'channel' attribute is required.");
		}
		if (!StringUtils.hasText(uri)) {
			throw new ConfigurationException("The 'uri' attribute is required.");
		}
		String marshallerRef = element.getAttribute("marshaller");
		String unmarshallerRef = element.getAttribute("unmarshaller");
		String sourceExtractorRef = element.getAttribute("source-extractor");
		RootBeanDefinition adapterDef = (StringUtils.hasText(marshallerRef)) ?
				this.parseMarshallingAdapter(uri, marshallerRef, unmarshallerRef) :
				this.parseSimpleAdapter(uri, sourceExtractorRef);
		String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, adapterBeanName));
		builder.addConstructorArgReference(adapterBeanName);
		Subscription subscription = new Subscription(channel);
		builder.addPropertyValue("subscription", subscription);
	}

	private RootBeanDefinition parseMarshallingAdapter(String uri, String marshallerRef, String unmarshallerRef) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(MarshallingWebServiceTargetAdapter.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(uri);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(marshallerRef));
		if (StringUtils.hasText(unmarshallerRef)) {
			beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(unmarshallerRef));
		}
		return beanDefinition;
	}

	private RootBeanDefinition parseSimpleAdapter(String uri, String sourceExtrractorRef) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleWebServiceTargetAdapter.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(uri);
		if (StringUtils.hasText(sourceExtrractorRef)) {
			beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(sourceExtrractorRef));
		}
		return beanDefinition;
	}

}
