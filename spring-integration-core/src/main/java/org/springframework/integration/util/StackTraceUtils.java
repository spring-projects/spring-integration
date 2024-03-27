/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.integration.util;

/**
 * Utility methods for analyzing stack traces.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public final class StackTraceUtils {

	private StackTraceUtils() {
	}

	/**
	 * Traverses the stack trace element array looking for instances that contain the first or second
	 * Strings in the className property.
	 * @param firstClass The first class to look for.
	 * @param secondClass The second class to look for.
	 * @param stackTrace The stack trace.
	 * @return true if the first class appears first, false if the second appears first
	 * @throws IllegalArgumentException if neither class is found.
	 */
	public static boolean isFrameContainingXBeforeFrameContainingY(String firstClass, String secondClass, StackTraceElement[] stackTrace) {
		for (StackTraceElement element : stackTrace) {
			if (element.getClassName().contains(firstClass)) {
				return true;
			}
			else if (element.getClassName().contains(secondClass)) {
				return false;
			}
		}
		throw new IllegalArgumentException("Neither " + firstClass + " nor " + secondClass + " class found");
	}

}
