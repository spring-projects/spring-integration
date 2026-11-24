/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.integration.config.SpelFunctionFactoryBean;

/**
 * Parser for the &lt;spel-function&gt; element.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class SpelFunctionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return SpelFunctionFactoryBean.class;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		builder.addConstructorArgValue(element.getAttribute("class"))
				.addConstructorArgValue(element.getAttribute("method"));

	}

}
