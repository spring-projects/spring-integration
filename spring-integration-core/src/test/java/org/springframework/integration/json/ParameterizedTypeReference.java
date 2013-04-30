/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.util.Assert;

/**
 * Copy of {@link org.springframework.core.ParameterizedTypeReference} from Spring Framework 3.2
 * @author Artem Bilan
 * @since 3.0
 */
//TODO Remove it after upgrade to Spring Framework 3.2 in favor to use org.springframework.core.ParameterizedTypeReference
public abstract class ParameterizedTypeReference<T> {

	private final Type type;

	protected ParameterizedTypeReference() {
		Class<?> parameterizedTypeReferenceSubClass = findParameterizedTypeReferenceSubClass(getClass());

		Type type = parameterizedTypeReferenceSubClass.getGenericSuperclass();
		Assert.isInstanceOf(ParameterizedType.class, type);

		ParameterizedType parameterizedType = (ParameterizedType) type;
		Assert.isTrue(parameterizedType.getActualTypeArguments().length == 1);

		this.type = parameterizedType.getActualTypeArguments()[0];
	}

	private static Class<?> findParameterizedTypeReferenceSubClass(Class<?> child) {

		Class<?> parent = child.getSuperclass();

		if (Object.class.equals(parent)) {
			throw new IllegalStateException("Expected ParameterizedTypeReference superclass");
		}
		else if (ParameterizedTypeReference.class.equals(parent)) {
			return child;
		}
		else {
			return findParameterizedTypeReferenceSubClass(parent);
		}
	}

	public Type getType() {
		return this.type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof ParameterizedTypeReference) {
			ParameterizedTypeReference<?> other = (ParameterizedTypeReference<?>) o;
			return this.type.equals(other.type);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.type.hashCode();
	}

	@Override
	public String toString() {
		return "ParameterizedTypeReference<" + this.type + ">";
	}
}
