/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.integration.json;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;

/**
 * Utility class to {@link #evaluate} a jsonPath on the provided object.
 * Delegates evaluation to <a href="http://code.google.com/p/json-path">JsonPath</a>.
 * Note {@link #evaluate} is used as {@code #jsonPath()} SpEL function.
 *
 * @author Artem Bilan
 *
 * @since 3.0
 */
public final class JsonPathUtils {

	public static <T> T evaluate(Object json, String jsonPath, Predicate... predicates) throws Exception {
		if (json instanceof String) {
			return JsonPath.read((String) json, jsonPath, predicates);
		}
		else if (json instanceof File) {
			return JsonPath.read((File) json, jsonPath, predicates);
		}
		else if (json instanceof URL) {
			return JsonPath.read(((URL) json).openStream(), jsonPath, predicates);
		}
		else if (json instanceof InputStream) {
			return JsonPath.read((InputStream) json, jsonPath, predicates);
		}
		else {
			return JsonPath.read(json, jsonPath, predicates);
		}

	}

	private JsonPathUtils() {
	}

}
