/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.test.matcher;

import java.lang.reflect.Method;

import org.hamcrest.BaseMatcher;

/**
 * This class was copied from JUnit to avoid using it from org.junit.internal (causing a backwards compatibility issue).
 * If you want to extend this class use a recent version of JUnit, and extend
 * <code>org.junit.matchers.TypeSafeMatcher</code>
 * <p>
 * Convenient base class for Matchers that require a non-null value of a specific type.
 * This simply implements the null check, checks the type and then casts.
 *
 * @author Joe Walnes
 */
abstract class TypeSafeMatcher<T> extends BaseMatcher<T> {

    private final Class<?> expectedType;

    /**
     * Subclasses should implement this. The item will already have been checked for
     * the specific type and will never be null.
     *
     * @param item The item.
     * @return true if matches.
     */
    public abstract boolean matchesSafely(T item);

    protected TypeSafeMatcher() {
        expectedType = findExpectedType(getClass());
    }

    private static Class<?> findExpectedType(Class<?> fromClass) {
        for (Class<?> c = fromClass; c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (isMatchesSafelyMethod(method)) {
                    return method.getParameterTypes()[0];
                }
            }
        }

        throw new Error("Cannot determine correct type for matchesSafely() method.");
    }

    private static boolean isMatchesSafelyMethod(Method method) {
        return method.getName().equals("matchesSafely")
                && method.getParameterTypes().length == 1
                && !method.isSynthetic();
    }

    protected TypeSafeMatcher(Class<T> expectedType) {
        this.expectedType = expectedType;
    }

    /**
     * Method made final to prevent accidental override.
     * If you need to override this, there's no point on extending TypeSafeMatcher.
     * Instead, extend the {@link BaseMatcher}.
     */
    @Override
	@SuppressWarnings({"unchecked"})
    public final boolean matches(Object item) {
        return item != null
                && expectedType.isInstance(item)
                && matchesSafely((T) item);
    }
}
