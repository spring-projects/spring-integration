package org.springframework.integration.ftp.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ftp.FtpSendingMessageHandlerFactoryBean;
import org.w3c.dom.Element;


/**
 * Logic for parsing the ftp:outbound-channel-adapter
 *
 * @author Josh Long
 */
public class FtpMessageSendingConsumerBeanDefinitionParser
		extends AbstractOutboundChannelAdapterParser {
	@Override
	protected AbstractBeanDefinition parseConsumer(Element element,
												   ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				FtpSendingMessageHandlerFactoryBean.class.getName());
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder,element,"charset");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder,element,"filename-generator", "fileNameGenerator");

		FtpNamespaceParserSupport.configureCoreFtpClient(builder, element,
				parserContext);

		return builder.getBeanDefinition();
	}
}
