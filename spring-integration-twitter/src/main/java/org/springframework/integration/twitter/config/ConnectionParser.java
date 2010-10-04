package org.springframework.integration.twitter.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.twitter.oauth.OAuthConfigurationFactoryBean;
import org.w3c.dom.Element;

public class ConnectionParser extends AbstractSingleBeanDefinitionParser {
    @Override
    protected String getBeanClassName(Element element) {
        return OAuthConfigurationFactoryBean.class.getName();
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
