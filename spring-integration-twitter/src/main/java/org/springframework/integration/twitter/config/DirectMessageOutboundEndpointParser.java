package org.springframework.integration.twitter.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;

import static org.springframework.integration.twitter.config.TwitterNamespaceHandler.BASE_PACKAGE;
public class DirectMessageOutboundEndpointParser extends AbstractOutboundChannelAdapterParser {
    @Override
    protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition( BASE_PACKAGE + ".OutboundDirectMessageStatusMessageHandler" );
        
        IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "twitter-connection", "configuration");
        return builder.getBeanDefinition();
    }
}
