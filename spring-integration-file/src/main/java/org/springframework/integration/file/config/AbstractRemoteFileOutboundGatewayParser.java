/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.remote.session.SessionFactoryFactoryBean;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 * @since 2.1
 */
public abstract class AbstractRemoteFileOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(getGatewayClassName());

		// build the SessionFactory and provide it as a constructor argument
		BeanDefinitionBuilder sessionFactoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(SessionFactoryFactoryBean.class);
		sessionFactoryBuilder.addConstructorArgReference(element.getAttribute("session-factory"));
		sessionFactoryBuilder.addConstructorArgValue(element.getAttribute("cache-sessions"));

		builder.addConstructorArgValue(sessionFactoryBuilder.getBeanDefinition());

		builder.addConstructorArgValue(element.getAttribute("command"));
		builder.addConstructorArgValue(element.getAttribute(EXPRESSION_ATTRIBUTE));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "command-options", "options");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "temporary-file-suffix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		this.configureFilter(builder, element, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "remote-file-separator");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-create-local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "order");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "rename-expression");
		return builder;
	}

	protected void configureFilter(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String filter = element.getAttribute("filter");
		String fileNamePattern = element.getAttribute("filename-pattern");
		String fileNameRegex = element.getAttribute("filename-regex");
		boolean hasFilter = StringUtils.hasText(filter);
		boolean hasFileNamePattern = StringUtils.hasText(fileNamePattern);
		boolean hasFileNameRegex = StringUtils.hasText(fileNameRegex);
		int count = hasFilter ? 1 : 0;
		count += hasFileNamePattern ? 1 : 0;
		count += hasFileNameRegex ? 1 : 0;
		if (count > 1) {
			parserContext.getReaderContext().error("at most one of 'filename-pattern', " +
					"'filename-regex', or 'filter' is allowed on remote file inbound adapter", element);
		}
		else if (hasFilter) {
			builder.addPropertyReference("filter", filter);
		}
		else if (hasFileNamePattern) {
			BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					this.getSimplePatternFileListFilterClassname());
			filterBuilder.addConstructorArgValue(fileNamePattern);
			builder.addPropertyValue("filter", filterBuilder.getBeanDefinition());
		}
		else if (hasFileNameRegex) {
			BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					this.getRegexPatternFileListFilterClassname());
			filterBuilder.addConstructorArgValue(fileNameRegex);
			builder.addPropertyValue("filter", filterBuilder.getBeanDefinition());
		}
	}

	protected abstract String getRegexPatternFileListFilterClassname();

	protected abstract String getSimplePatternFileListFilterClassname();

	protected abstract String getGatewayClassName();

}
