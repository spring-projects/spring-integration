/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;

/**
 * Base class for parsers that create an instance of
 * {@link org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler}.
 *
 * @author Oleg Zhurakousky
 * @author Stefan Ferstl
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 */
public abstract class AbstractCorrelatingMessageHandlerParser extends AbstractConsumerEndpointParser {

	private static final String CORRELATION_STRATEGY_REF_ATTRIBUTE = "correlation-strategy";

	private static final String CORRELATION_STRATEGY_METHOD_ATTRIBUTE = "correlation-strategy-method";

	private static final String CORRELATION_STRATEGY_EXPRESSION_ATTRIBUTE = "correlation-strategy-expression";

	private static final String CORRELATION_STRATEGY_PROPERTY = "correlationStrategy";

	private static final String RELEASE_STRATEGY_REF_ATTRIBUTE = "release-strategy";

	private static final String RELEASE_STRATEGY_METHOD_ATTRIBUTE = "release-strategy-method";

	private static final String RELEASE_STRATEGY_EXPRESSION_ATTRIBUTE = "release-strategy-expression";

	private static final String RELEASE_STRATEGY_PROPERTY = "releaseStrategy";

	private static final String MESSAGE_STORE_ATTRIBUTE = "message-store";

	private static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	private static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	private static final String SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE = "send-partial-result-on-expiry";

	private static final String EXPIRE_GROUPS_UPON_TIMEOUT = "expire-groups-upon-timeout";

	private static final String RELEASE_LOCK = "release-lock-before-send";

	protected void doParse(BeanDefinitionBuilder builder, Element element, BeanMetadataElement processor,
			ParserContext parserContext) {
		IntegrationNamespaceUtils.injectPropertyWithAdapter(CORRELATION_STRATEGY_REF_ATTRIBUTE,
				CORRELATION_STRATEGY_METHOD_ATTRIBUTE,
				CORRELATION_STRATEGY_EXPRESSION_ATTRIBUTE, CORRELATION_STRATEGY_PROPERTY, "CorrelationStrategy",
				element, builder, processor, parserContext);
		IntegrationNamespaceUtils.injectPropertyWithAdapter(RELEASE_STRATEGY_REF_ATTRIBUTE,
				RELEASE_STRATEGY_METHOD_ATTRIBUTE,
				RELEASE_STRATEGY_EXPRESSION_ATTRIBUTE, RELEASE_STRATEGY_PROPERTY, "ReleaseStrategy",
				element, builder, processor, parserContext);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, MESSAGE_STORE_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "scheduler", "taskScheduler");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "lock-registry");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE,
				"discardChannelName");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "empty-group-min-timeout",
				"minimumTimeoutForEmptyGroups");

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "pop-sequence");

		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("group-timeout",
						"group-timeout-expression", parserContext, element, false);
		builder.addPropertyValue("groupTimeoutExpression", expressionDef);

		Element txElement = DomUtils.getChildElementByTagName(element, "expire-transactional");
		Element adviceChainElement = DomUtils.getChildElementByTagName(element, "expire-advice-chain");

		IntegrationNamespaceUtils.configureAndSetAdviceChainIfPresent(adviceChainElement, txElement,
				builder.getRawBeanDefinition(), parserContext, "forceReleaseAdviceChain");

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, EXPIRE_GROUPS_UPON_TIMEOUT);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, RELEASE_LOCK);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expire-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expire-duration",
				"expireDurationMillis");

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "group-condition-supplier");
	}

}
