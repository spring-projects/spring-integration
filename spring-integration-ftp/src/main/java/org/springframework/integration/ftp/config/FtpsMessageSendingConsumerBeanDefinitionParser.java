package org.springframework.integration.ftp.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.ftp.FtpSendingMessageHandlerFactoryBean;
import org.springframework.integration.ftp.FtpsSendingMessageHandlerFactoryBean;
import org.w3c.dom.Element;


/**
 * Logic for parsing the ftp:outbound-channel-adapter
 *
 * @author Josh Long
 */
public class FtpsMessageSendingConsumerBeanDefinitionParser extends AbstractOutboundChannelAdapterParser {
    @Override
    protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FtpsSendingMessageHandlerFactoryBean.class.getName());

        FtpNamespaceParserSupport.configureCoreFtpClient(builder, element, parserContext);

        return builder.getBeanDefinition();
    }
}
