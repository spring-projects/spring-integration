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
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.Resequencer;

/**
 * Parser for the &lt;resequencer&gt; element.
 * 
 * @author Marius Bogoevici
 */
public class ResequencerParser extends AbstractConsumerEndpointParser {

	private static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	private static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	private static final String RELEASE_PARTIAL_SEQUENCES = "release-partial-sequences";

	private static final String SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE = "send-partial-result-on-timeout";

	private static final String REAPER_INTERVAL_ATTRIBUTE = "reaper-interval";

	private static final String TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE = "tracked-correlation-id-capacity";

	private static final String TIMEOUT_ATTRIBUTE = "timeout";


	@Override
	protected BeanDefinitionBuilder parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(Resequencer.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, RELEASE_PARTIAL_SEQUENCES);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REAPER_INTERVAL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, TIMEOUT_ATTRIBUTE);
		return builder;
	}

}
