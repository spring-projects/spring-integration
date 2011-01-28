/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.http.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.ParserContext;

/**
 * @author Oleg Zhurakousky
 * @since 2.0.2
 */
public class HttpParsingUtils {
	
	private static final String[] REST_TEMPLATE_ATTRIBUTES = {
		"request-factory", "error-handler", "message-converters"
	};
	
	static void verifyNoRestTemplateAttributes(Element element, ParserContext parserContext) {
		for (String attributeName : REST_TEMPLATE_ATTRIBUTES) {
			if (element.hasAttribute(attributeName)) {
				parserContext.getReaderContext().error("When providing a 'rest-template' reference, the '"
						+ attributeName + "' attribute is not allowed", parserContext.extractSource(element));
			}
		}
	}
}
