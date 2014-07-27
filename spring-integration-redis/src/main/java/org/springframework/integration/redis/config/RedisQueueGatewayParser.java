/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.outbound.RedisQueueGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;int-redis:queue-outbound-gateway&gt; element.
 *
 * @author Artem Bilan
 * @author David Liu
 * @since 3.0
 */
public class RedisQueueGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisQueueGateway.class);
		BeanDefinition queueExpression = IntegrationNamespaceUtils
				.createExpressionDefinitionFromValueOrExpression("queue", "queue-expression", parserContext, element, true);
		builder.addConstructorArgValue(queueExpression);

		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);
		builder.addPropertyValue("expectReply", true);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel","outputChannel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requiresReply");
		return builder;
	}

}
