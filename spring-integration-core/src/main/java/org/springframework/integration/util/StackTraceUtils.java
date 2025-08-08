/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
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
