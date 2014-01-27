/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support.json;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.util.ClassUtils;

/**
 * Base class for Jackson {@link JsonObjectMapper} implementations.
 *
 * @param <N> - The expected type of JSON Node.
 * @param <P> - The expected type of JSON Parser.
 * @param <J> - The expected type of Java Type representation.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public abstract class AbstractJacksonJsonObjectMapper<N, P, J> implements JsonObjectMapper<N, P>, BeanClassLoaderAware {

	protected static final Collection<Class<?>> supportedJsonTypes =
			Arrays.<Class<?>> asList(String.class, byte[].class, File.class, URL.class, InputStream.class, Reader.class);

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public <T> T fromJson(Object json, Class<T> valueType) throws Exception {
		return this.fromJson(json, this.constructType(valueType));
	}

	@Override
	public <T> T fromJson(Object json, Map<String, Object> javaTypes) throws Exception {
		J javaType = this.extractJavaType(javaTypes);
		return this.fromJson(json, javaType);
	}

	protected J createJavaType(Map<String, Object> javaTypes, String javaTypeKey) throws Exception {
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
				aClass = ClassUtils.forName(classValue.toString(), this.classLoader);
			}

			return this.constructType(aClass);
		}
	}

	protected abstract <T> T fromJson(Object json, J type) throws Exception;

	protected abstract J extractJavaType(Map<String, Object> javaTypes) throws Exception;

	protected abstract J constructType(Type type);

}
