/*
 * Copyright 2013-2020 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * Base class for Jackson {@link JsonObjectMapper} implementations.
 *
 * @param <N> - The expected type of JSON Node.
 * @param <P> - The expected type of JSON Parser.
 * @param <J> - The expected type of Java Type representation.
 *
 * @author Artem Bilan
 *
 * @since 3.0
 */
public abstract class AbstractJacksonJsonObjectMapper<N, P, J> implements JsonObjectMapper<N, P>, BeanClassLoaderAware {

	protected static final Collection<Class<?>> SUPPORTED_JSON_TYPES =
			Arrays.asList(String.class, byte[].class, File.class, URL.class, InputStream.class, Reader.class);

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	protected ClassLoader getClassLoader() {
		return this.classLoader;
	}

	@Override
	public <T> T fromJson(Object json, Class<T> valueType) throws IOException {
		return fromJson(json, constructType(valueType));
	}

	@Override
	public <T> T fromJson(Object json, ResolvableType valueType) throws IOException {
		return fromJson(json, constructType(valueType.getType()));
	}

	@Override
	public <T> T fromJson(Object json, Map<String, Object> javaTypes) throws IOException {
		J javaType = extractJavaType(javaTypes);
		return fromJson(json, javaType);
	}

	protected J createJavaType(Map<String, Object> javaTypes, String javaTypeKey) {
		Object classValue = javaTypes.get(javaTypeKey);
		if (classValue == null) {
			throw new IllegalArgumentException("Could not resolve '" + javaTypeKey + "' in 'javaTypes'.");
		}
		else {
			Class<?> aClass = null;
			if (classValue instanceof Class<?>) {
				aClass = (Class<?>) classValue;
			}
			else {
				try {
					aClass = ClassUtils.forName(classValue.toString(), this.classLoader);
				}
				catch (ClassNotFoundException | LinkageError e) {
					throw new IllegalStateException(e);
				}
			}

			return this.constructType(aClass);
		}
	}

	protected abstract <T> T fromJson(Object json, J type) throws IOException;

	protected abstract J extractJavaType(Map<String, Object> javaTypes);

	protected abstract J constructType(Type type);

}
