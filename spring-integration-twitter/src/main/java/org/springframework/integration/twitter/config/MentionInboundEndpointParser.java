package org.springframework.integration.twitter.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;

import static org.springframework.integration.twitter.config.TwitterNamespaceHandler.BASE_PACKAGE;

public class MentionInboundEndpointParser extends AbstractSingleBeanDefinitionParser {
    @Override
    protected String getBeanClassName(Element element) {
        return BASE_PACKAGE + ".InboundMentionStatusEndpoint";
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "channel", "outputChannel");
        IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "twitter-connection", "configuration");
    }
}
