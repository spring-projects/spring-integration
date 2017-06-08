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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
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
		super();
	}

	/**
	 * Return an {@link com.fasterxml.jackson.databind.ObjectMapper} if available,
	 * supplied with Message specific serializers and deserializers.
	 * Also configured to store typo info in the {@code @class} property.
	 * @param trustedPackages the trusted Java packages for deserialization.
	 * @return the mapper.
	 * @throws IllegalStateException if an implementation is not available.
	 * @since 4.3.10
	 */
	public static com.fasterxml.jackson.databind.ObjectMapper messagingAwareMapper(String... trustedPackages) {
		if (JacksonPresent.isJackson2Present()) {
			ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			mapper.setDefaultTyping(new WhitelistTypeResolverBuilder(trustedPackages));

			GenericMessageJacksonDeserializer genericMessageDeserializer = new GenericMessageJacksonDeserializer();
			genericMessageDeserializer.setMapper(mapper);

			ErrorMessageJacksonDeserializer errorMessageDeserializer = new ErrorMessageJacksonDeserializer();
			errorMessageDeserializer.setMapper(mapper);

			AdviceMessageJacksonDeserializer adviceMessageDeserializer = new AdviceMessageJacksonDeserializer();
			adviceMessageDeserializer.setMapper(mapper);

			MutableMessageJacksonDeserializer mutableMessageDeserializer = new MutableMessageJacksonDeserializer();
			mutableMessageDeserializer.setMapper(mapper);

			mapper.registerModule(new SimpleModule()
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

	/**
	 * An implementation of {@link ObjectMapper.DefaultTypeResolverBuilder}
	 * that wraps a default {@link TypeIdResolver} to the {@link WhitelistTypeIdResolver}.
	 *
	 * @author Rob Winch
	 * @author Artem Bilan
	 *
	 * @since 4.3.11
	 */
	private static final class WhitelistTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {

		private static final long serialVersionUID = 1L;

		private final String[] trustedPackages;

		WhitelistTypeResolverBuilder(String... trustedPackages) {
			super(ObjectMapper.DefaultTyping.NON_FINAL);
			this.trustedPackages = trustedPackages;

			init(JsonTypeInfo.Id.CLASS, null)
					.inclusion(JsonTypeInfo.As.PROPERTY);
		}

		@Override
		protected TypeIdResolver idResolver(MapperConfig<?> config, JavaType baseType, Collection<NamedType> subtypes,
				boolean forSer, boolean forDeser) {
			TypeIdResolver delegate = super.idResolver(config, baseType, subtypes, forSer, forDeser);
			return new WhitelistTypeIdResolver(delegate, this.trustedPackages);
		}

	}

	/**
	 * A {@link TypeIdResolver} that delegates to an existing implementation
	 * and throws an IllegalStateException if the class being looked up is not whitelisted,
	 * does not provide an explicit mixin mappings.
	 *
	 * @author Rob Winch
	 * @author Artem Bilan
	 *
	 * @since 4.3.11
	 */
	private static final class WhitelistTypeIdResolver implements TypeIdResolver {

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

		WhitelistTypeIdResolver(TypeIdResolver delegate, String... trustedPackages) {
			this.delegate = delegate;
			if (trustedPackages != null) {
				for (String whiteListClass : trustedPackages) {
					if ("*".equals(whiteListClass)) {
						this.trustedPackages.clear();
						break;
					}
					else {
						this.trustedPackages.add(whiteListClass);
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

			String packageName = result.getRawClass().getPackage().getName();
			if (isTrustedPackage(packageName)) {
				return this.delegate.typeFromId(context, id);
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
					if (packageName.equals(trustedPackage) || packageName.startsWith(trustedPackage + ".")) {
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
