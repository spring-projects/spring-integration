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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.integration.aggregator.ResequencingMessageHandler;

/**
 * Parser for the &lt;resequencer&gt; element.
 * 
 * @author Marius Bogoevici
 */
public class ResequencerParser extends AbstractSimpleBeanDefinitionParser {

	public static final String OUTPUT_CHANNEL_ATTRIBUTE = "output-channel";

	public static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return ResequencingMessageHandler.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !OUTPUT_CHANNEL_ATTRIBUTE.equals(attributeName)
				&& !DISCARD_CHANNEL_ATTRIBUTE.equals(attributeName)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, OUTPUT_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE);
	}

}
