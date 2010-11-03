/**
 * 
 */
package org.springframework.integration.xmpp.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author ozhurakousky
 *
 */
public class XmppConnectionParser extends AbstractSingleBeanDefinitionParser {
	private static String[] connectionFactoryAttributes = 
		new String[]{"userid", "password", "resource","subscription-mode"};
	
	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.xmpp" + ".XmppConnectionFactoryBean";
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String serviceName = element.getAttribute("service-name");
		String host = element.getAttribute("host");
		String port = element.getAttribute("port");	
		BeanDefinitionBuilder connectionConfigurationBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.jivesoftware.smack.ConnectionConfiguration");
		if (StringUtils.hasText(host)) {
			Assert.hasLength(port, "Port must be provided if 'host' is specified");
			connectionConfigurationBuilder.addConstructorArgValue(host);
			connectionConfigurationBuilder.addConstructorArgValue(port);
		}
		else {
			Assert.hasText(serviceName, "'serviceName' is requuired if 'host' is not provided");
		}
		if (StringUtils.hasText(serviceName)){
			connectionConfigurationBuilder.addConstructorArgValue(port);
		}
		for (String attribute : connectionFactoryAttributes) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, attribute);
		}
		builder.addConstructorArgValue(connectionConfigurationBuilder.getBeanDefinition());
	}
}
