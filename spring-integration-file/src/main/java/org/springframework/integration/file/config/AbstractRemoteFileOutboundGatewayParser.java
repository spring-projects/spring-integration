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
package org.springframework.integration.file.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public abstract class AbstractRemoteFileOutboundGatewayParser extends AbstractConsumerEndpointParser {
	
	private final Log logger = LogFactory.getLog(this.getClass());

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(getGatewayClassName());

		// build the SessionFactory and provide it as a constructor argument

		// This whole block must be refactored once cache-session attribute is removed
		String sessionFactoryName = element.getAttribute("session-factory");
		BeanDefinition sessionFactoryDefinition = parserContext.getReaderContext().getRegistry().getBeanDefinition(sessionFactoryName);
		String sessionFactoryClassName = sessionFactoryDefinition.getBeanClassName();
		if (StringUtils.hasText(sessionFactoryClassName) && sessionFactoryClassName.endsWith(CachingSessionFactory.class.getName())) {
			builder.addConstructorArgValue(sessionFactoryDefinition);
		}
		else {
			String cacheSessions = element.getAttribute("cache-sessions");
			if (StringUtils.hasText(cacheSessions) && logger.isWarnEnabled()) {
				logger.warn("The 'cache-sessions' attribute is deprecated as of version 2.1." +
						"Please configure a CachingSessionFactory explicitly instead.");
			}
			if ("false".equalsIgnoreCase(cacheSessions)) {
				builder.addConstructorArgReference(element.getAttribute("session-factory"));
			}
			else {		
				BeanDefinitionBuilder sessionFactoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(CachingSessionFactory.class);		
				sessionFactoryBuilder.addConstructorArgReference(sessionFactoryName);
				builder.addConstructorArgValue(sessionFactoryBuilder.getBeanDefinition());
			}
		}
		// end of what needs to be refactored once cache-session is removed

		builder.addConstructorArgValue(element.getAttribute("command"));
		builder.addConstructorArgValue(element.getAttribute(EXPRESSION_ATTRIBUTE));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "command-options", "options");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "temporary-file-suffix");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		this.configureFilter(builder, element, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "remote-file-separator");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-create-local-directory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "order");
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
