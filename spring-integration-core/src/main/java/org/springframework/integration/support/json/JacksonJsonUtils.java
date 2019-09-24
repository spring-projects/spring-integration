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

package org.springframework.integration.support.json;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.Builder;
import com.fasterxml.jackson.databind.module.SimpleModule;

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

	/**
	 * Return an {@link ObjectMapper} if available,
	 * supplied with Message specific serializers and deserializers.
	 * Also configured to store typo info in the {@code @class} property.
	 * @param trustedPackages the trusted Java packages for deserialization.
	 * @return the mapper.
	 * @throws IllegalStateException if an implementation is not available.
	 * @since 4.3.10
	 */
	public static ObjectMapper messagingAwareMapper(String... trustedPackages) {
		if (JacksonPresent.isJackson2Present()) {
			ObjectMapper mapper = new Jackson2JsonObjectMapper().getObjectMapper();

			mapper.setDefaultTyping(new WhitelistTypeResolverBuilder(trustedPackages));

			GenericMessageJacksonDeserializer genericMessageDeserializer = new GenericMessageJacksonDeserializer();
			genericMessageDeserializer.setMapper(mapper);

			ErrorMessageJacksonDeserializer errorMessageDeserializer = new ErrorMessageJacksonDeserializer();
			errorMessageDeserializer.setMapper(mapper);

			AdviceMessageJacksonDeserializer adviceMessageDeserializer = new AdviceMessageJacksonDeserializer();
			adviceMessageDeserializer.setMapper(mapper);

			MutableMessageJacksonDeserializer mutableMessageDeserializer = new MutableMessageJacksonDeserializer();
			mutableMessageDeserializer.setMapper(mapper);

			SimpleModule simpleModule = new SimpleModule()
					.addSerializer(new MessageHeadersJacksonSerializer())
					.addDeserializer(GenericMessage.class, genericMessageDeserializer)
					.addDeserializer(ErrorMessage.class, errorMessageDeserializer)
					.addDeserializer(AdviceMessage.class, adviceMessageDeserializer)
					.addDeserializer(MutableMessage.class, mutableMessageDeserializer);

			mapper.registerModule(simpleModule);
			return mapper;
		}
		else {
			throw new IllegalStateException("No jackson-databind.jar is present in the classpath.");
		}
	}

	/**
	 * An implementation of {@link ObjectMapper.DefaultTypeResolverBuilder}
	 * that verifies packages.
	 *
	 * @author Rob Winch
	 * @author Artem Bilan
	 * @author Gary Russell
	 *
	 * @since 4.3.11
	 */
	private static final class WhitelistTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {

		private static final long serialVersionUID = 1L;

		WhitelistTypeResolverBuilder(String... trustedPackages) {
			super(ObjectMapper.DefaultTyping.NON_FINAL, validator(trustedPackages));
			init(JsonTypeInfo.Id.CLASS, null)
					.inclusion(JsonTypeInfo.As.PROPERTY);
		}

		private static BasicPolymorphicTypeValidator validator(String... trustedPackages) {
			Builder builder = BasicPolymorphicTypeValidator.builder();
			boolean anyMatch = false;
			for (String pkg : trustedPackages) {
				if (pkg.equals("*")) {
					builder.allowIfSubType(Pattern.compile(".*"));
					anyMatch = true;
				}
			}
			if (!anyMatch) {
				Arrays.asList(
						"java.util",
						"java.lang",
						"org.springframework.messaging.support",
						"org.springframework.integration.support",
						"org.springframework.integration.message",
						"org.springframework.integration.store"
				).forEach(pkg -> builder.allowIfSubType(pkg));
				for (String pkg : trustedPackages) {
					builder.allowIfSubType(pkg);
				}
				builder.allowIfSubType(byte[].class);
			}
			return builder.build();
		}

	}

}
