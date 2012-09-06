/*
 * Copyright 2007-2012 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisParserUtils {

	public static void setRedisSerializers(boolean redisTemplateSet, Element element, ParserContext parserContext, BeanDefinitionBuilder builder){
		if (redisTemplateSet){
			if (element.hasAttribute("key-serializer") ||
				element.hasAttribute("value-serializer") ||
				element.hasAttribute("hash-key-serializer") ||
				element.hasAttribute("hash-value-serializer")){

				parserContext.getReaderContext().error("Serializers can not be provided if this adapter is initailized " +
						"with RedisTemplate. You may set serializers directly on RedisTemplate", element);
			}
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "key-serializer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "value-serializer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "hash-key-serializer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "hash-value-serializer");
	}
}
