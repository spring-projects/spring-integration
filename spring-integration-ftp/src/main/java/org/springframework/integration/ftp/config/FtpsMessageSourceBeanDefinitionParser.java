package org.springframework.integration.ftp.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ftp.impl.FtpRemoteFileSystemSynchronizingMessageSourceFactoryBean;
import org.springframework.integration.ftp.impl.FtpsRemoteFileSystemSynchronizingMessageSourceFactoryBean;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Logic that configures an ftp:inbound-channel-adapter
 *
 * @author Josh Long
 */
public class FtpsMessageSourceBeanDefinitionParser extends AbstractPollingInboundChannelAdapterParser {
    private Set<String> receiveAttrs = new HashSet<String>(Arrays.asList("auto-delete-remote-files-on-sync,filename-pattern,local-working-directory".split(",")));

    @Override
    @SuppressWarnings("unused")
    protected String parseSource(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FtpsRemoteFileSystemSynchronizingMessageSourceFactoryBean.class.getName());

        IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "filter");

        for (String a : receiveAttrs)
            IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, a);

        FtpNamespaceParserSupport.configureCoreFtpClient(builder, element, parserContext);

        return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
    }
}
