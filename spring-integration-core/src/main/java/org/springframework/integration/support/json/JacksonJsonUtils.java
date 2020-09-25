/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
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

			mapper.setDefaultTyping(new AllowListTypeResolverBuilder(trustedPackages));

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
					.addSerializer(new MimeTypeSerializer())
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
	 * that wraps a default {@link TypeIdResolver} to the {@link AllowlistTypeIdResolver}.
	 *
	 * @author Rob Winch
	 * @author Artem Bilan
	 * @author Filip Hanik
	 * @author Gary Russell
	 *
	 * @since 4.3.11
	 */
	private static final class AllowListTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {

		private static final long serialVersionUID = 1L;

		private final String[] trustedPackages;

		AllowListTypeResolverBuilder(String... trustedPackages) {
			super(ObjectMapper.DefaultTyping.NON_FINAL,
					//we do explicit validation in the TypeIdResolver
					BasicPolymorphicTypeValidator.builder()
							.allowIfSubType(Object.class)
							.build());

			this.trustedPackages =
					trustedPackages != null ? Arrays.copyOf(trustedPackages, trustedPackages.length) : null;

			init(JsonTypeInfo.Id.CLASS, null)
					.inclusion(JsonTypeInfo.As.PROPERTY);
		}

		@Override
		protected TypeIdResolver idResolver(MapperConfig<?> config,
				JavaType baseType,
				PolymorphicTypeValidator subtypeValidator,
				Collection<NamedType> subtypes, boolean forSer, boolean forDeser) {
			TypeIdResolver result = super.idResolver(config, baseType, subtypeValidator, subtypes, forSer, forDeser);
			return new AllowlistTypeIdResolver(result, this.trustedPackages);
		}

	}

	/**
	 * A {@link TypeIdResolver} that delegates to an existing implementation
	 * and throws an IllegalStateException if the class being looked up is not trusted,
	 * does not provide an explicit mixin mappings.
	 *
	 * @author Rob Winch
	 * @author Artem Bilan
	 *
	 * @since 4.3.11
	 */
	private static final class AllowlistTypeIdResolver implements TypeIdResolver {

		private static final List<String> TRUSTED_PACKAGES =
				Arrays.asList(
						"java.util",
						"java.lang",
						"org.springframework.messaging.support",
						"org.springframework.integration.support",
						"org.springframework.integration.message",
						"org.springframework.integration.store"
				);

		private final TypeIdResolver delegate;

		private final Set<String> trustedPackages = new LinkedHashSet<>(TRUSTED_PACKAGES);

		AllowlistTypeIdResolver(TypeIdResolver delegate, String... trustedPackages) {
			this.delegate = delegate;
			if (trustedPackages != null) {
				for (String trustedPackage : trustedPackages) {
					if ("*".equals(trustedPackage)) {
						this.trustedPackages.clear();
						break;
					}
					else {
						this.trustedPackages.add(trustedPackage);
					}
				}
			}
		}

		@Override
		public void init(JavaType baseType) {
			this.delegate.init(baseType);
		}

		@Override
		public String idFromValue(Object value) {
			return this.delegate.idFromValue(value);
		}

		@Override
		public String idFromValueAndType(Object value, Class<?> suggestedType) {
			return this.delegate.idFromValueAndType(value, suggestedType);
		}

		@Override
		public String idFromBaseType() {
			return this.delegate.idFromBaseType();
		}

		@Override
		public JavaType typeFromId(DatabindContext context, String id) throws IOException {
			DeserializationConfig config = (DeserializationConfig) context.getConfig();
			JavaType result = this.delegate.typeFromId(context, id);

			Package aPackage = result.getRawClass().getPackage();
			if (aPackage == null || isTrustedPackage(aPackage.getName())) {
				return result;
			}

			boolean isExplicitMixin = config.findMixInClassFor(result.getRawClass()) != null;
			if (isExplicitMixin) {
				return result;
			}

			throw new IllegalArgumentException("The class with " + id + " and name of " +
					"" + result.getRawClass().getName() + " is not in the trusted packages: " +
					"" + this.trustedPackages + ". " +
					"If you believe this class is safe to deserialize, please provide its name or an explicit Mixin. " +
					"If the serialization is only done by a trusted source, you can also enable default typing.");
		}

		private boolean isTrustedPackage(String packageName) {
			if (!this.trustedPackages.isEmpty()) {
				for (String trustedPackage : this.trustedPackages) {
					if (packageName.equals(trustedPackage) ||
							(!packageName.equals("java.util.logging")
									&& packageName.startsWith(trustedPackage + "."))) {

						return true;
					}
				}
				return false;
			}

			return true;
		}

		@Override
		public String getDescForKnownTypeIds() {
			return this.delegate.getDescForKnownTypeIds();
		}

		@Override
		public JsonTypeInfo.Id getMechanism() {
			return this.delegate.getMechanism();
		}

	}

}
