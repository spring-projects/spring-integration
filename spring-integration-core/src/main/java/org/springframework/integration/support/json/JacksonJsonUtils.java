/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.support.json;

import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ClassUtils;

/**
 * Utility methods for Jackson.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
public final class JacksonJsonUtils {

	private JacksonJsonUtils() {
		super();
	}

	private static final ClassLoader classLoader = JacksonJsonUtils.class.getClassLoader();

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", classLoader) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", classLoader);

	public static boolean isJackson2Present() {
		return jackson2Present;
	}

	public static boolean isJacksonPresent() {
		return jacksonPresent;
	}

	/**
	 * Return an {@link com.fasterxml.jackson.databind.ObjectMapper} if available,
	 * supplied with Message specific serializers and deserializers.
	 * Also configured to store typo info in the {@code @class} property.
	 * @return the mapper.
	 * @throws IllegalStateException if an implementation is not available.
	 * @since 4.3.10
	 */
	public static com.fasterxml.jackson.databind.ObjectMapper messagingAwareMapper() {
		if (jackson2Present) {
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			mapper.configure(com.fasterxml.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION, false);
			mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.enableDefaultTyping(com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,
					com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

			GenericMessageJacksonDeserializer genericMessageDeserializer = new GenericMessageJacksonDeserializer();
			genericMessageDeserializer.setMapper(mapper);

			ErrorMessageJacksonDeserializer errorMessageDeserializer = new ErrorMessageJacksonDeserializer();
			errorMessageDeserializer.setMapper(mapper);

			AdviceMessageJacksonDeserializer adviceMessageDeserializer = new AdviceMessageJacksonDeserializer();
			adviceMessageDeserializer.setMapper(mapper);

			MutableMessageJacksonDeserializer mutableMessageDeserializer = new MutableMessageJacksonDeserializer();
			mutableMessageDeserializer.setMapper(mapper);

			mapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
					.addSerializer(new MessageHeadersJacksonSerializer())
					.addDeserializer(GenericMessage.class, genericMessageDeserializer)
					.addDeserializer(ErrorMessage.class, errorMessageDeserializer)
					.addDeserializer(AdviceMessage.class, adviceMessageDeserializer)
					.addDeserializer(MutableMessage.class, mutableMessageDeserializer)
			);
			return mapper;
		}
		else {
			throw new IllegalStateException("No jackson-databind.jar is present in the classpath.");
		}
	}

}
