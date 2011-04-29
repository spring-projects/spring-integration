package org.springframework.integration.config.xml;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * parser for the top level 'wire-tap' element
 * @author David Turanski
 * @since 2.1
 *
 */
public class GlobalWireTapParser extends GlobalChannelInterceptorParser {
	@Override
	protected Object getBeanDefinitionBuilderConstructorValue(Element element, ParserContext parserContext){
		String wireTapBeanName = new WireTapParser().parse(element, parserContext);
		return new RuntimeBeanReference(wireTapBeanName);
	}
}
