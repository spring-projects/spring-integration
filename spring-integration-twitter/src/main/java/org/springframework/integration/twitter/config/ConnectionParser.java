package org.springframework.integration.twitter.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import static org.springframework.integration.twitter.config.TwitterNamespaceHandler.BASE_PACKAGE;

public class ConnectionParser extends AbstractSingleBeanDefinitionParser {
    @Override
    protected String getBeanClassName(Element element) {
        return BASE_PACKAGE + ".oauth.OAuthConfigurationFactoryBean"; 
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        TwitterNamespaceHandler.configureTwitterConnection(element, parserContext, builder);
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }
}
