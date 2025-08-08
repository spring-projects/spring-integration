/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element of the jms namespace.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JmsOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsSendingMessageHandler.class);
		String jmsTemplate = element.getAttribute(JmsParserUtils.JMS_TEMPLATE_ATTRIBUTE);
		String destination = element.getAttribute(JmsParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsParserUtils.DESTINATION_NAME_ATTRIBUTE);
		String destinationExpression = element.getAttribute(JmsParserUtils.DESTINATION_EXPRESSION_ATTRIBUTE);
		boolean hasJmsTemplate = StringUtils.hasText(jmsTemplate);
		boolean hasDestinationRef = StringUtils.hasText(destination);
		boolean hasDestinationName = StringUtils.hasText(destinationName);
		boolean hasDestinationExpression = StringUtils.hasText(destinationExpression);
		if (hasJmsTemplate) {
			JmsParserUtils.verifyNoJmsTemplateAttributes(element, parserContext);
			builder.addConstructorArgReference(jmsTemplate);
		}
		else {
			builder.addConstructorArgValue(JmsParserUtils.parseJmsTemplateBeanDefinition(element, parserContext));
		}

		if (hasDestinationRef || hasDestinationName || hasDestinationExpression) {
			if (!(hasDestinationRef ^ hasDestinationName ^ hasDestinationExpression)) {
				parserContext.getReaderContext()
						.error("The 'destination', 'destination-name', and 'destination-expression' " +
										"attributes are mutually exclusive.",
								parserContext.extractSource(element));
			}
			if (hasDestinationRef) {
				builder.addPropertyReference(JmsParserUtils.DESTINATION_PROPERTY, destination);
			}
			else if (hasDestinationName) {
				builder.addPropertyValue(JmsParserUtils.DESTINATION_NAME_PROPERTY, destinationName);
			}
			else {
				BeanDefinitionBuilder expressionBuilder =
						BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
								.addConstructorArgValue(destinationExpression);
				builder.addPropertyValue(JmsParserUtils.DESTINATION_EXPRESSION_PROPERTY,
						expressionBuilder.getBeanDefinition());
			}
		}
		else if (!hasJmsTemplate) {
			parserContext.getReaderContext()
					.error("either a '" + JmsParserUtils.JMS_TEMPLATE_ATTRIBUTE +
							"' or one of '" + JmsParserUtils.DESTINATION_ATTRIBUTE + "', '"
							+ JmsParserUtils.DESTINATION_NAME_ATTRIBUTE + "', or '" +
							JmsParserUtils.DESTINATION_EXPRESSION_ATTRIBUTE +
							"' attributes must be provided", parserContext.extractSource(element));
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				JmsParserUtils.HEADER_MAPPER_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delivery-mode-expression",
				"deliveryModeExpressionString");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "time-to-live-expression",
				"timeToLiveExpressionString");
		return builder.getBeanDefinition();
	}

}
