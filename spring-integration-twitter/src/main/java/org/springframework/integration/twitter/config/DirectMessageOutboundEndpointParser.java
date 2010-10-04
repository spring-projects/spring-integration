package org.springframework.integration.twitter.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.twitter.OutboundDirectMessageStatusMessageHandler;
import org.w3c.dom.Element;

public class DirectMessageOutboundEndpointParser extends AbstractOutboundChannelAdapterParser {
    @Override
    protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(OutboundDirectMessageStatusMessageHandler.class.getName());
        IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "twitter-connection", "configuration");
        return builder.getBeanDefinition();
    }
}
