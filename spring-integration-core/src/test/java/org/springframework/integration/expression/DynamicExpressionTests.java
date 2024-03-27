/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.expression;

import java.io.FileOutputStream;

import org.junit.After;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class DynamicExpressionTests {

	private static final String key = "test.greeting";

	private static final String basename = "org/springframework/integration/expression/expressions";

	private static final String filepath = basename + ".properties";

	@After
	public void resetFile() {
		writeExpressionStringToFile("'Hello World!'");
	}

	@Test
	public void expressionUpdate() throws Exception {
		ReloadableResourceBundleExpressionSource source = new ReloadableResourceBundleExpressionSource();
		source.setBasename(basename);
		source.setCacheSeconds(0);
		DynamicExpression expression = new DynamicExpression(key, source);
		assertThat(expression.getValue()).isEqualTo("Hello World!");
		writeExpressionStringToFile("toUpperCase()");
		assertThat(expression.getValue("foo")).isEqualTo("FOO");
	}

	private static void writeExpressionStringToFile(String expressionString) {
		ClassPathResource resource = new ClassPathResource(filepath);
		byte[] bytes = new String(key + "=" + expressionString).getBytes();
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(resource.getFile());
			fileOutputStream.write(bytes);
			fileOutputStream.close();
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to write expression string to file", e);
		}
	}

}
