/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.amqp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'channel' and 'publish-subscribe-channel' elements of the
 * Spring Integration AMQP namespace.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class AmqpChannelParser extends AbstractChannelParser {

	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AmqpChannelFactoryBean.class);
		String messageDriven = element.getAttribute("message-driven");
		if (StringUtils.hasText(messageDriven)) {
			builder.addConstructorArgValue(messageDriven);
		}
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "rabbitConnectionFactory";
		}
		builder.addPropertyReference("connectionFactory", connectionFactory);

		builder.addPropertyValue("pubSub", "publish-subscribe-channel".equals(element.getLocalName()));

		populateConsumersPerQueueIfAny(element, parserContext, builder);

		String[] valuesToPopulate = {
				"max-subscribers",
				"acknowledge-mode",
				"auto-startup",
				"channel-transacted",
				"template-channel-transacted",
				"concurrent-consumers",
				"encoding",
				"expose-listener-channel",
				"phase",
				"prefetch-count",
				"queue-name",
				"receive-timeout",
				"recovery-interval",
				"missing-queues-fatal",
				"shutdown-timeout",
				"tx-size",
				"default-delivery-mode",
				"extract-payload",
				"headers-last"
		};

		for (String attribute : valuesToPopulate) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, attribute);
		}

		String[] referencesToPopulate = {
				"advice-chain",
				"amqp-admin",
				"error-handler",
				"exchange",
				"message-converter",
				"message-properties-converter",
				"task-executor",
				"transaction-attribute",
				"transaction-manager",
				"outbound-header-mapper",
				"inbound-header-mapper"
		};

		for (String attribute : referencesToPopulate) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, attribute);
		}

		return builder;
	}

	private void populateConsumersPerQueueIfAny(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {

		String consumersPerQueue = element.getAttribute("consumers-per-queue");
		if (StringUtils.hasText(consumersPerQueue)) {
			if (StringUtils.hasText(element.getAttribute("concurrent-consumers"))) {
				parserContext.getReaderContext()
						.error("'consumers-per-queue' and 'concurrent-consumers' are mutually exclusive", element);
			}
			if (StringUtils.hasText(element.getAttribute("tx-size"))) {
				parserContext.getReaderContext().error("'tx-size' is not allowed with 'consumers-per-queue'", element);
			}
			if (StringUtils.hasText(element.getAttribute("receive-timeout"))) {
				parserContext.getReaderContext()
						.error("'receive-timeout' is not allowed with 'consumers-per-queue'", element);
			}
			builder.addPropertyValue("consumersPerQueue", consumersPerQueue);
		}
	}

}
