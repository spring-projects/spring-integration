/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.mapping.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Pre-defined names and prefixes to be used for setting and/or retrieving JSON
 * entries from/to Message Headers and other adapter, e.g. AMQP.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public final class JsonHeaders {

	private JsonHeaders() {
		super();
	}

	public static final String PREFIX = "json";

	public static final String TYPE_ID = PREFIX + "__TypeId__";

	public static final String CONTENT_TYPE_ID = PREFIX + "__ContentTypeId__";

	public static final String KEY_TYPE_ID = PREFIX + "__KeyTypeId__";

	public static final Collection<String> HEADERS =
			Collections.unmodifiableList(Arrays.asList(TYPE_ID, CONTENT_TYPE_ID, KEY_TYPE_ID));

}
