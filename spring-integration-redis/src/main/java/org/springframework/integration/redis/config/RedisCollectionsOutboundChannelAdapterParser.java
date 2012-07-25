package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.outbound.RedisCollectionPopulatingMessageHandler;
import org.springframework.util.StringUtils;

public class RedisCollectionsOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisCollectionPopulatingMessageHandler.class);
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);

		boolean atLeastOneRequired = false;
		RootBeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("key", "key-expression",
						parserContext, element, atLeastOneRequired);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "collection-type");

		if (expressionDef != null){
			builder.addConstructorArgValue(expressionDef);
		}

		return builder.getBeanDefinition();
	}
}