/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.annotation;

/**
 * Common value constants for annotation attributes.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
public final class AnnotationConstants {

	/**
	 * Constant defining a value as a replacement for {@code null} which
	 * we cannot use in annotation attributes.
	 */
	public static final String NULL = "__NULL__";

	private AnnotationConstants() {
	}

}
