package org.springframework.integration.twitter.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.twitter.InboundDirectMessageStatusEndpoint;
import org.w3c.dom.Element;

public class DirectMessageInboundEndpointParser extends AbstractSingleBeanDefinitionParser {
    @Override
    protected String getBeanClassName(Element element) {
        return InboundDirectMessageStatusEndpoint.class.getName(); 
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "channel", "requestChannel");
        IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "twitter-connection", "configuration");
    }
}
