/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.twitter.*;
import org.springframework.integration.twitter.oauth.OAuthConfigurationFactoryBean;

import org.springframework.util.StringUtils;

import org.w3c.dom.Element;


/**
 * @author Josh Long
 * @since 2.0
 */
@SuppressWarnings("unused")
public class TwitterNamespaceHandler extends org.springframework.beans.factory.xml.NamespaceHandlerSupport {
    public static final String DIRECT_MESSAGES = "direct-messages";
    public static final String MENTIONS = "mentions";
    public static final String FRIENDS = "friends";

    public void init() {
        // twitter connections
        registerBeanDefinitionParser("twitter-connection", new TwitterConnectionParser());

        // inbound
        registerBeanDefinitionParser("inbound-update-channel-adapter", new TwitterUpdatedStatusInboundEndpointParser());
        registerBeanDefinitionParser("inbound-dm-channel-adapter", new TwitterDMInboundEndpointParser());
        registerBeanDefinitionParser("inbound-mention-channel-adapter", new TwitterMentionInboundEndpointParser());

        // outbound
        registerBeanDefinitionParser("outbound-update-channel-adapter", new TwitterUpdatedStatusOutboundEndpointParser());
        registerBeanDefinitionParser("outbound-dm-channel-adapter", new TwitterDMOutboundEndpointParser());
    }

    private static void configureTwitterConnection(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String ref = element.getAttribute("twitter-connection");

        if (org.springframework.util.StringUtils.hasText(ref)) {
            builder.addPropertyReference("twitterConnection", ref);
        } else {
            for (String attribute : new String[] { "consumer-key", "consumer-secret", "access-token", "access-token-secret" }) {
                IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, attribute);
            }
        }
    }

    // twitter:twitter-connection
    private static class TwitterConnectionParser extends AbstractSingleBeanDefinitionParser {
        @Override
        protected Class getBeanClass(Element element) {
            return OAuthConfigurationFactoryBean.class;
        }

        @Override
        protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
            configureTwitterConnection(element, parserContext, builder);
        }

        @Override
        protected boolean shouldGenerateIdAsFallback() {
            return true;
        }
    }

    // twitter:inbound-mention-channel-adapter
    private static class TwitterMentionInboundEndpointParser extends AbstractSingleBeanDefinitionParser {
        @Override
        protected String getBeanClassName(Element element) {
            return InboundMentionStatusEndpoint.class.getName();
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

    // twitter:inbound-dm-channel-adapter
    private static class TwitterDMInboundEndpointParser extends AbstractSingleBeanDefinitionParser {
        @Override
        protected String getBeanClassName(Element element) {
            return InboundDMStatusEndpoint.class.getName();
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

    // twitter:inbound-update-channel-adapter
    private static class TwitterUpdatedStatusInboundEndpointParser extends AbstractSingleBeanDefinitionParser {
        @Override
        protected String getBeanClassName(Element element) {
            return InboundUpdatedStatusEndpoint.class.getName();
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

    // twitter:outbound-update-channel-adapter
    private static class TwitterUpdatedStatusOutboundEndpointParser extends AbstractOutboundChannelAdapterParser {
        @Override
        protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(OutboundUpdatedStatusMessageHandler.class.getName());
            IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "twitter-connection", "configuration");
            return builder.getBeanDefinition();
        }
    }

    // twitter:outbound-dm-channel-adapter
    private static class TwitterDMOutboundEndpointParser extends AbstractOutboundChannelAdapterParser {
        @Override
        protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(OutboundDMStatusMessageHandler.class.getName());
            IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "twitter-connection", "configuration");
            return builder.getBeanDefinition();
        }
    }
}
