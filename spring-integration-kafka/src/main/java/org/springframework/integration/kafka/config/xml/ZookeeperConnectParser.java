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
package org.springframework.integration.kafka.config.xml;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.w3c.dom.Element;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class ZookeeperConnectParser extends AbstractSimpleBeanDefinitionParser {
	@Override
	protected Class<?> getBeanClass(final Element element) {
		return ZookeeperConnect.class;
	}

	@Override
	protected void doParse(final Element element, final ParserContext parserContext, final BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				BeanDefinitionParserDelegate.SCOPE_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "zk-connect");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "zk-connection-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "zk-session-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "zk-sync-time");
	}
}
