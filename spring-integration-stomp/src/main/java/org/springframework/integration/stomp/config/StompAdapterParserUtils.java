/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.stomp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.stomp.support.StompHeaderMapper;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @since 4.2
 */
abstract class StompAdapterParserUtils {

	static void configureStompAdapter(BeanDefinitionBuilder builder, ParserContext parserContext, Element element) {
		String stompSessionManager = element.getAttribute("stomp-session-manager");
		if (!StringUtils.hasText(stompSessionManager)) {
			parserContext.getReaderContext().error("The 'stomp-session-manager' is required", element);
		}
		builder.addConstructorArgReference(stompSessionManager);

		String headerMapper = element.getAttribute("header-mapper");

		String mappedHeaders = element.getAttribute("mapped-headers");

		boolean hasMappedHeaders = StringUtils.hasText(mappedHeaders);

		if (StringUtils.hasText(headerMapper)) {
			if (hasMappedHeaders) {
				parserContext.getReaderContext().error("The 'mapped-headers' " +
								"attribute is not allowed when a 'header-mapper' has been specified.",
						parserContext.extractSource(element));
			}
			builder.addPropertyReference("headerMapper", headerMapper);
		}
		else if (hasMappedHeaders) {
			BeanDefinitionBuilder headerMapperBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(StompHeaderMapper.class);

			// This is tricky a bit, but from one side the 'headerMapper' is an internal instance
			// and isn't accessible outside and from other side allow us to avoid extra 'boolean' variable
			headerMapperBuilder.addPropertyValue("inboundHeaderNames", mappedHeaders);
			headerMapperBuilder.addPropertyValue("outboundHeaderNames", mappedHeaders);

			builder.addPropertyValue("headerMapper", headerMapperBuilder.getBeanDefinition());
		}
	}

}
