/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
