/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.integration.adapter.file.config.FileSourceAdapterParser;
import org.springframework.integration.adapter.file.config.FileTargetAdapterParser;
import org.springframework.integration.adapter.jms.config.JmsSourceAdapterParser;
import org.springframework.integration.adapter.jms.config.JmsTargetAdapterParser;

/**
 * Namespace handler for the integration namespace.
 * 
 * @author Mark Fisher
 */
public class IntegrationNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		registerBeanDefinitionParser("message-bus", new MessageBusParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenParser());
		registerBeanDefinitionParser("channel", new ChannelParser());
		registerBeanDefinitionParser("source-adapter", new ChannelAdapterParser(true));
		registerBeanDefinitionParser("target-adapter", new ChannelAdapterParser(false));
		registerBeanDefinitionParser("endpoint", new EndpointParser());
		registerBeanDefinitionParser("file-source", new FileSourceAdapterParser());
		registerBeanDefinitionParser("file-target", new FileTargetAdapterParser());
		registerBeanDefinitionParser("jms-source", new JmsSourceAdapterParser());
		registerBeanDefinitionParser("jms-target", new JmsTargetAdapterParser());
	}

}
