/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.file.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.locking.NioFileLocker;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;inbound-channel-adapter&gt; element of the 'file' namespace.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 */
public class FileInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(FileReadingMessageSourceFactoryBean.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "comparator");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "scanner");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-create-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "queue-size");
		String dispositionExpression = element.getAttribute("disposition-expression");
		if (StringUtils.hasText(dispositionExpression)) {
			RootBeanDefinition expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(dispositionExpression);
			builder.addPropertyValue("dispositionExpression", expressionDef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "disposition-result-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "disposition-send-timeout");
		String filterBeanName = this.registerFilter(element, parserContext);
		String lockerBeanName = registerLocker(element, parserContext);
		if (lockerBeanName != null) {
			builder.addPropertyReference("locker", lockerBeanName);
		}
		builder.addPropertyReference("filter", filterBeanName);
		String beanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
		return new RuntimeBeanReference(beanName);
	}

	private String registerLocker(Element element, ParserContext parserContext) {
		String lockerBeanName = null;
		Element nioLocker = DomUtils.getChildElementByTagName(element, "nio-locker");
		if (nioLocker != null) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(NioFileLocker.class);
			lockerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					builder.getBeanDefinition(), parserContext.getRegistry());
		}
		else {
			Element locker = DomUtils.getChildElementByTagName(element, "locker");
			if (locker != null) {
				lockerBeanName = locker.getAttribute("ref");
			}
		}
		return lockerBeanName;
	}

	private String registerFilter(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder factoryBeanBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(FileListFilterFactoryBean.class);
		factoryBeanBuilder.setRole(BeanDefinition.ROLE_SUPPORT);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(factoryBeanBuilder, element, "filter");
		String filenamePattern = element.getAttribute("filename-pattern");
		if (StringUtils.hasText(filenamePattern)) {
			if (element.hasAttribute("filter")) {
				parserContext.getReaderContext().error(
						"At most one of 'filter' and 'filename-pattern' may be provided.", element);
			}
			factoryBeanBuilder.addPropertyValue("filenamePattern", filenamePattern);
		}
		String filenameRegex = element.getAttribute("filename-regex");
		if (StringUtils.hasText(filenameRegex)) {
			if (element.hasAttribute("filter")) {
				parserContext.getReaderContext().error(
						"At most one of 'filter' and 'filename-regex' may be provided.", element);
			}
			factoryBeanBuilder.addPropertyValue("filenameRegex", filenameRegex);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(factoryBeanBuilder, element, "prevent-duplicates");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				factoryBeanBuilder.getBeanDefinition(), parserContext.getRegistry());
	}

}
