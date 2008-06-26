package org.springframework.integration.security.target.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class SecuredTargetsParser extends AbstractSingleBeanDefinitionParser {

	public SecuredTargetsParser() {
		super();
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		
	}


}
