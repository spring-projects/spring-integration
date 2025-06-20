/*
 * Copyright 2015-present the original author or authors.
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
