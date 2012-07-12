/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.mongodb.config.xml;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * The helper class that would be used for channel adapter parsers. Intended to keep the
 * common logic from the parsers in it
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbChannelAdapterParserUtil {

	/**
	 * The method reads the the common attributes like mongo-db-factory and collection name from the
	 * xml definition and sets them in the target {@link BeanDefinitionBuilder} instance provided.
	 *
	 */
	public static void configureCommonMongoAttributes(BeanDefinitionBuilder builder,Element element) {
		String beanRef = element.getAttribute("mongo-db-factory");
		builder.addConstructorArgReference(beanRef);
		String collection = element.getAttribute("collection");
		if(StringUtils.hasText(collection)) {
			builder.addConstructorArgValue(collection);
		}
		else {
			builder.addConstructorArgValue(null);
		}
	}
}
