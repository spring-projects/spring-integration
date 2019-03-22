/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.MapToObjectTransformer;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class MapToObjectTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return MapToObjectTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String ref = element.getAttribute("ref");
		String type = element.getAttribute("type");
		if (StringUtils.hasText(ref) && StringUtils.hasText(type)) {
			parserContext.getReaderContext().error("'type' and 'ref' attributes are mutually-exclusive, " +
							"but both have valid values; type: " + type + "; ref: " + ref,
					IntegrationNamespaceUtils.createElementDescription(element));
		}
		if (StringUtils.hasText(ref)) {
			builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(ref, "java.lang.String");
		}
		else if (StringUtils.hasText(type)) {
			builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(type, "java.lang.Class");
		}
	}

}
