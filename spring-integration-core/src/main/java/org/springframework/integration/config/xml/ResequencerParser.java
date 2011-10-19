/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.config.xml;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.ResequencingMessageGroupProcessor;
import org.springframework.integration.aggregator.ResequencingMessageHandler;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;resequencer&gt; element.
 * 
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 */
public class ResequencerParser extends AbstractCorrelatingMessageHandlerParser {


	private static final String COMPARATOR_REF_ATTRIBUTE = "comparator";

	private static final String RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE = "release-partial-sequences";
	
	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ResequencingMessageHandler.class);
		BeanDefinitionBuilder processorBuilder = BeanDefinitionBuilder.genericBeanDefinition(ResequencingMessageGroupProcessor.class);

		// Comparator
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(processorBuilder, element, COMPARATOR_REF_ATTRIBUTE);

		String processorRef = BeanDefinitionReaderUtils.registerWithGeneratedName(processorBuilder.getBeanDefinition(),
				parserContext.getRegistry());

		// Message group processor
		builder.addConstructorArgReference(processorRef);

		// Message store
		builder.addConstructorArgValue(BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".store.SimpleMessageStore").getBeanDefinition());
		
		this.doParse(builder, element, processorBuilder.getBeanDefinition(), parserContext);
		
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE);
		
		return builder;
	}
}
