/*
 * Copyright 2025-present the original author or authors.
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

import java.io.Serial;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import tools.jackson.databind.module.SimpleModule;

import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

/**
 * Utility for creating Jackson {@link JsonMapper} instance for Spring messaging.
 *
 * <p>Provides custom serializers/deserializers for Spring messaging types
 * and validates deserialization against trusted package patterns.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public final class JacksonMessagingUtils {

	/**
	 * The packages to trust on JSON deserialization by default.
	 */
	public static final List<String> DEFAULT_TRUSTED_PACKAGES =
			List.of(
					"java.util",
					"java.lang",
					"org.springframework.messaging.support",
					"org.springframework.integration.support",
					"org.springframework.integration.message",
					"org.springframework.integration.store",
					"org.springframework.integration.history",
					"org.springframework.integration.handler"
			);

	private JacksonMessagingUtils() {
	}

	/**
	 * Return an {@link JsonMapper} if available,
	 * supplied with Message specific serializers and deserializers.
	 * Also configured to store typo info in the {@code @class} property.
	 * @param trustedPackages the trusted Java packages for deserialization.
	 * @return the JSON mapper.
	 * @throws IllegalStateException if an implementation is not available.
	 * @since 7.0
	 */
	public static JsonMapper messagingAwareMapper(String @Nullable ... trustedPackages) {
		if (JacksonPresent.isJackson3Present()) {
			GenericMessageJsonDeserializer genericMessageDeserializer = new GenericMessageJsonDeserializer();
			ErrorMessageJsonDeserializer errorMessageDeserializer = new ErrorMessageJsonDeserializer();
			AdviceMessageJsonDeserializer adviceMessageDeserializer = new AdviceMessageJsonDeserializer();
			MutableMessageJsonDeserializer mutableMessageDeserializer = new MutableMessageJsonDeserializer();

			SimpleModule simpleModule = new SimpleModule()
					.addSerializer(new MessageHeadersJsonSerializer())
					.addSerializer(new MimeTypeJsonSerializer())
					.addDeserializer(GenericMessage.class, genericMessageDeserializer)
					.addDeserializer(ErrorMessage.class, errorMessageDeserializer)
					.addDeserializer(AdviceMessage.class, adviceMessageDeserializer)
					.addDeserializer(MutableMessage.class, mutableMessageDeserializer);

			JsonMapper mapper = JsonMapper.builder()
					.findAndAddModules(JacksonMessagingUtils.class.getClassLoader())
					.setDefaultTyping(new AllowListTypeResolverBuilder(trustedPackages))
					.addModules(simpleModule)
					.build();

			genericMessageDeserializer.setMapper(mapper);
			errorMessageDeserializer.setMapper(mapper);
			adviceMessageDeserializer.setMapper(mapper);
			mutableMessageDeserializer.setMapper(mapper);

			return mapper;
		}
		else {
			throw new IllegalStateException("No jackson-databind.jar is present in the classpath.");
		}
	}

	/**
	 * An implementation of {@link DefaultTypeResolverBuilder}
	 * that wraps a default {@link TypeIdResolver} to the {@link AllowListTypeIdResolver}.
	 *
	 * @author Jooyoung Pyoung
	 */
	private static final class AllowListTypeResolverBuilder extends DefaultTypeResolverBuilder {

		@Serial
		private static final long serialVersionUID = 1L;

		private final String @Nullable [] trustedPackages;

		AllowListTypeResolverBuilder(String @Nullable ... trustedPackages) {
			super(
					BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
					DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY
			);

			this.trustedPackages =
					trustedPackages != null ? Arrays.copyOf(trustedPackages, trustedPackages.length) : null;
		}

		@Override
		protected TypeIdResolver idResolver(DatabindContext ctxt,
											JavaType baseType, PolymorphicTypeValidator subtypeValidator,
											Collection<NamedType> subtypes, boolean forSer, boolean forDeser) {

			TypeIdResolver result = super.idResolver(ctxt, baseType, subtypeValidator, subtypes, forSer, forDeser);
			return new AllowListTypeIdResolver(result, this.trustedPackages);
		}

	}

	/**
	 * A {@link TypeIdResolver} that delegates to an existing implementation
	 * and throws an IllegalStateException if the class being looked up is not trusted,
	 * does not provide an explicit mixin mappings.
	 *
	 * @author Jooyoung Pyoung
	 */
	private static final class AllowListTypeIdResolver implements TypeIdResolver {

		private final TypeIdResolver delegate;

		private final Set<String> trustedPackages = new LinkedHashSet<>(DEFAULT_TRUSTED_PACKAGES);

		AllowListTypeIdResolver(TypeIdResolver delegate, String @Nullable ... trustedPackages) {
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
		public void init(JavaType baseType) throws JacksonException {
			this.delegate.init(baseType);
		}

		@Override
		public String idFromValue(DatabindContext ctxt, Object value) throws JacksonException {
			return this.delegate.idFromValue(ctxt, value);
		}

		@Override
		public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> suggestedType) throws JacksonException {
			return this.delegate.idFromValueAndType(ctxt, value, suggestedType);
		}

		@Override
		public String idFromBaseType(DatabindContext ctxt) throws JacksonException {
			return this.delegate.idFromBaseType(ctxt);
		}

		@Override
		public JavaType typeFromId(DatabindContext ctxt, String id) throws JacksonException {
			JavaType result = this.delegate.typeFromId(ctxt, id);

			Package aPackage = result.getRawClass().getPackage();
			if (aPackage == null || isTrustedPackage(aPackage.getName())) {
				return result;
			}

			MapperConfig<?> config = ctxt.getConfig();
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
