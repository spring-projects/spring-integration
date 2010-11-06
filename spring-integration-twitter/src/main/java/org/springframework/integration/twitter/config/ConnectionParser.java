/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.twitter.config;

import static org.springframework.integration.twitter.config.TwitterNamespaceHandler.BASE_PACKAGE;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for the 'twitter-connection' element.
 * 
 * @author Josh Long
 * @author Mark Fisher
 * @since 2.0
 */
public class ConnectionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return BASE_PACKAGE + ".oauth.OAuthTwitterFactoryBean";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

		BeanDefinitionBuilder accessTokenBuilder = BeanDefinitionBuilder.genericBeanDefinition("twitter4j.http.AccessToken");
		accessTokenBuilder.addConstructorArgValue(element.getAttribute("access-token"));
		accessTokenBuilder.addConstructorArgValue(element.getAttribute("access-token-secret"));
		builder.addConstructorArgValue(element.getAttribute("consumer-key"));
		builder.addConstructorArgValue(element.getAttribute("consumer-secret"));
		builder.addConstructorArgValue(accessTokenBuilder.getBeanDefinition());
	}

}
