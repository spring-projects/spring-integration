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
package org.springframework.integration.ftp.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ftp.FtpSendingMessageHandlerFactoryBean;
import org.springframework.integration.ftp.impl.FtpRemoteFileSystemSynchronizingMessageSourceFactoryBean;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;


/**
 * Provides namespace support for using FTP
 *
 * @author Josh Long  (*heavily* influenced by the good done by iwein before)
 */
@SuppressWarnings("unused")
public class FtpNamespaceHandler extends NamespaceHandlerSupport {
    private static final String PACKAGE_NAME = "org.springframework.integration.ftp";
    static private Map<String, Integer> CLIENT_MODES = new HashMap<String, Integer>();

    static {
        CLIENT_MODES.put("active-local-data-connection-mode", 0);
        CLIENT_MODES.put("active-remote-data-connection-mode", 1);
        CLIENT_MODES.put("passive-local-data-connection-mode", 2);
        CLIENT_MODES.put("passive-remote-data-connection-mode", 3);
    }

    public void init() {
        registerBeanDefinitionParser("inbound-channel-adapter", new FTPMessageSourceBeanDefinitionParser());
        registerBeanDefinitionParser("outbound-channel-adapter", new FTPMessageSendingConsumerBeanDefinitionParser());
    }

    /**
     * Configures an object that can take inbound messages and send them.
     */
    private static class FTPMessageSendingConsumerBeanDefinitionParser extends AbstractOutboundChannelAdapterParser {
        @Override
        protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FtpSendingMessageHandlerFactoryBean.class.getName());

            for (String p : "auto-create-directories,username,port,password,host,key-file,key-file-password,remote-directory".split(",")) {
                IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, p);
            }

            int clientMode = CLIENT_MODES.get(element.getAttribute("client-mode"));

            builder.addPropertyValue("clientMode", clientMode);

            return builder.getBeanDefinition();
        }
    }

    /**
     * Configures an object that can recieve files from a remote SFTP endpoint and broadcast their arrival to the
     * consumer
     */
    private static class FTPMessageSourceBeanDefinitionParser extends AbstractPollingInboundChannelAdapterParser {
        @Override
        @SuppressWarnings("unused")
        protected String parseSource(Element element, ParserContext parserContext) {

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
                    FtpRemoteFileSystemSynchronizingMessageSourceFactoryBean.class.getName());

            IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,"filter");

            for (String p : ("auto-delete-remote-files-on-sync,filename-pattern,auto-create-directories,username,password,host,port," +
                    "remote-directory,local-working-directory").split(",")) {
                IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, p);
            }

            int clientMode = CLIENT_MODES.get(element.getAttribute("client-mode"));
            builder.addPropertyValue("clientMode", clientMode);

            return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
        }
    }
}
