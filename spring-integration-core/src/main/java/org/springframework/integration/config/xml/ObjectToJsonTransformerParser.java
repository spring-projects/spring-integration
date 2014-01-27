/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class ObjectToJsonTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return ObjectToJsonTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String objectMapper = element.getAttribute("object-mapper");
		if (StringUtils.hasText(objectMapper)) {
			builder.addConstructorArgReference(objectMapper);
		}
		String resultType = element.getAttribute("result-type");
		if (StringUtils.hasText(resultType)) {
			builder.addConstructorArgValue(resultType);
		}
		if (element.hasAttribute("content-type")){
			builder.addPropertyValue("contentType", element.getAttribute("content-type"));
		}
	}

}
