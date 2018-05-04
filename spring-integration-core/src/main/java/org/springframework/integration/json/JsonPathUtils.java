/*
 * Copyright 2013-2018 the original author or authors.
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;

/**
 * Utility class to {@link #evaluate} a jsonPath on the provided object.
 * Delegates evaluation to <a href="http://code.google.com/p/json-path">JsonPath</a>.
 * Note {@link #evaluate} is used as {@code #jsonPath()} SpEL function.
 * Note: selecting the charset for a byte[] conversion is not supported via SpEL.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public final class JsonPathUtils {

	private static final Charset UTF8 = StandardCharsets.UTF_8;

	/**
	 * Evaluate the Json Path; if the json is a byte[], UTF-8 is used for conversion.
	 * @param json the json.
	 * @param jsonPath the path.
	 * @param predicates the predicates.
	 * @return the value.
	 * @throws Exception if an exception occurs.
	 */
	public static <T> T evaluate(Object json, String jsonPath, Predicate... predicates) throws Exception {
		return evaluate(json, jsonPath, UTF8, predicates);
	}

	/**
	 * Evaluate the Json Path.
	 * @param json the json.
	 * @param jsonPath the path.
	 * @param charset the charset to convert from byte[] to String.
	 * @param predicates the predicates.
	 * @return the value.
	 * @throws Exception if an exception occurs.
	 * @since 4.3.16
	 */
	public static <T> T evaluate(Object json, String jsonPath, String charset, Predicate... predicates)
			throws Exception {
		return evaluate(json, jsonPath, Charset.forName(charset), predicates);
	}

	/**
	 * Evaluate the Json Path.
	 * @param json the json.
	 * @param jsonPath the path.
	 * @param charset the charset to convert from byte[] to String.
	 * @param predicates the predicates.
	 * @return the value.
	 * @throws Exception if an exception occurs.
	 * @since 4.3.16
	 */
	public static <T> T evaluate(Object json, String jsonPath, Charset charset, Predicate... predicates)
			throws Exception {
		if (json instanceof String) {
			return JsonPath.read((String) json, jsonPath, predicates);
		}
		else if (json instanceof byte[]) {
			return JsonPath.read(new String((byte[]) json, charset == null ? UTF8 : charset), jsonPath, predicates);
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
