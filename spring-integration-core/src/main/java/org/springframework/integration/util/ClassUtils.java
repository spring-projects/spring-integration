/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.util;

import java.util.Set;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class ClassUtils {

	public static Class<?> findClosestMatch(Class<?> type, Set<Class<?>> candidates, boolean failOnTie) {
		int minTypeDiffWeight = Integer.MAX_VALUE;
		Class<?> closestMatch = null;
		for (Class<?> candidate : candidates) {
			int typeDiffWeight = getTypeDifferenceWeight(candidate, type);
			if (typeDiffWeight < minTypeDiffWeight) {
				minTypeDiffWeight = typeDiffWeight;
				closestMatch = candidate;
			}
			else if (failOnTie && typeDiffWeight < Integer.MAX_VALUE && (typeDiffWeight == minTypeDiffWeight)) {
				throw new IllegalStateException("Unresolvable ambiguity while attempting to find closest match for [" +
						type.getName() + "]. Candidate types [" + closestMatch.getName() + "] and [" + candidate.getName() + 
						"] have equal weight.");
			}
		}
		return closestMatch;
	}

	private static int getTypeDifferenceWeight(Class<?> candidate, Class<?> type) {
		int result = 0;
		if (!org.springframework.util.ClassUtils.isAssignable(candidate, type)) {
			return Integer.MAX_VALUE;
		}
		Class<?> superClass = type.getSuperclass();
		while (superClass != null) {
			if (type.equals(superClass)) {
				result = result + 2;
				superClass = null;
			}
			else if (org.springframework.util.ClassUtils.isAssignable(candidate, superClass)) {
				result = result + 2;
				superClass = superClass.getSuperclass();
			}
			else {
				superClass = null;
			}
		}
		if (candidate.isInterface()) {
			result = result + 1;
		}
		return result;
	}

}
