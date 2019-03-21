/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.locking.NioFileLocker;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;inbound-channel-adapter&gt; element of the 'file' namespace.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class FileInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(FileReadingMessageSourceFactoryBean.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "comparator");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "scanner");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "use-watch-service");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "watch-events");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-create-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "queue-size");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "scan-each-poll");
		String filterBeanName = this.registerFilter(element, parserContext);
		String lockerBeanName = registerLocker(element, parserContext);
		if (filterBeanName != null) {
			builder.addPropertyReference(FileParserUtils.FILTER_ATTRIBUTE, filterBeanName);
		}
		if (lockerBeanName != null) {
			builder.addPropertyReference("locker", lockerBeanName);
		}

		return builder.getBeanDefinition();
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

	private String registerFilter(Element element, ParserContext parserContext) { // NOSONAR
		String filenamePattern = element.getAttribute("filename-pattern");
		String filenameRegex = element.getAttribute("filename-regex");
		String preventDuplicates = element.getAttribute("prevent-duplicates");
		String ignoreHidden = element.getAttribute("ignore-hidden");
		String filter = element.getAttribute(FileParserUtils.FILTER_ATTRIBUTE);
		String filterExpression = element.getAttribute("filter-expression");
		if (!StringUtils.hasText(filter) // NOSONAR
				&& !StringUtils.hasText(filenamePattern)
				&& !StringUtils.hasText(filenameRegex)
				&& !StringUtils.hasText(preventDuplicates)
				&& !StringUtils.hasText(ignoreHidden)
				&& !StringUtils.hasText(filterExpression)) {
			return null;
		}
		BeanDefinitionBuilder factoryBeanBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(FileListFilterFactoryBean.class);
		factoryBeanBuilder.setRole(BeanDefinition.ROLE_SUPPORT);
		if (StringUtils.hasText(filter)) {
			factoryBeanBuilder.addPropertyReference(FileParserUtils.FILTER_ATTRIBUTE, filter);
		}
		if (StringUtils.hasText(filterExpression)) {
			if (StringUtils.hasText(filter)) {
				parserContext.getReaderContext()
						.error("At most one of 'filter' or 'filter-expression' can be provided.", element);
			}
			BeanDefinition expressionFilterBeanDefinition =
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFileListFilter.class)
							.addConstructorArgValue(filterExpression)
							.getBeanDefinition();
			factoryBeanBuilder.addPropertyValue(FileParserUtils.FILTER_ATTRIBUTE, expressionFilterBeanDefinition);
		}
		if (StringUtils.hasText(filenamePattern)) {
			if (StringUtils.hasText(filter)) {
				parserContext.getReaderContext().error(
						"At most one of 'filter' and 'filename-pattern' may be provided.", element);
			}
			factoryBeanBuilder.addPropertyValue("filenamePattern", filenamePattern);
		}
		if (StringUtils.hasText(filenameRegex)) {
			if (StringUtils.hasText(filter)) {
				parserContext.getReaderContext().error(
						"At most one of 'filter' and 'filename-regex' may be provided.", element);
			}
			factoryBeanBuilder.addPropertyValue("filenameRegex", filenameRegex);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(factoryBeanBuilder, element, "prevent-duplicates");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(factoryBeanBuilder, element, "ignore-hidden");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(factoryBeanBuilder.getBeanDefinition(),
				parserContext.getRegistry());
	}

}
