/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.support.json;

import org.springframework.util.ClassUtils;

/**
 * The utility to check if Jackson JSON processor is present in the classpath.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public final class JacksonPresent {

	private static final boolean JACKSON_2_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", null) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", null);

	public static boolean isJackson2Present() {
		return JACKSON_2_PRESENT;
	}

	private JacksonPresent() {
	}

}
