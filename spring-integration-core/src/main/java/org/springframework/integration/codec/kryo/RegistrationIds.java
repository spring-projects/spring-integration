/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

/**
 * Default registration ids for serializers provided by the framework.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public final class RegistrationIds {

	public static final int DEFAULT_FILE_REGISTRATION_ID = 40;

	public static final int DEFAULT_MESSAGEHEADERS_ID = 41;

	public static final int DEFAULT_MUTABLE_MESSAGEHEADERS_ID = 42;

	public static final int DEFAULT_GENERIC_MESSAGE_ID = 43;

	public static final int DEFAULT_ERROR_MESSAGE_ID = 44;

	public static final int DEFAULT_ADVICE_MESSAGE_ID = 45;

	public static final int DEFAULT_MUTABLE_MESSAGE_ID = 46;

	public static final int DEFAULT_HASH_MAP_ID = 47;

	public static final int DEFAULT_UUID_ID = 48;

	private RegistrationIds() {
	}

}
