/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.scripting.jsr223;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

/**
 * @author David Turanski
 * @author Artem Bilan
 */
public class Jsr223ScriptExecutorTests {

	@Test
	public void test() {
		ScriptExecutor executor = ScriptExecutorFactory.getScriptExecutor("jruby");
		executor.executeScript(new StaticScriptSource("'hello, world'"));
		executor.executeScript(new StaticScriptSource("'hello, again'"));

		Map<String, Object> variables = new HashMap<>();

		Map<String, Object> headers = new HashMap<>();
		headers.put("one", 1);
		headers.put("two", "two");
		headers.put("three", 3);

		variables.put("payload", "payload");
		variables.put("headers", headers);

		Resource resource = new ClassPathResource("/org/springframework/integration/scripting/jsr223/print_message.rb");
		String result = (String) executor.executeScript(new ResourceScriptSource(resource), variables);

		assertThat(result).isNotNull().contains("payload modified");
	}

	@Test
	public void testPython() {
		ScriptExecutor executor = ScriptExecutorFactory.getScriptExecutor("python");
		Object obj = executor.executeScript(new StaticScriptSource("x=2"));
		assertThat(obj).isEqualTo(2);

		obj = executor.executeScript(new StaticScriptSource("def foo(y):\n\tx=y\n\treturn y\nz=foo(2)"));
		assertThat(obj).isEqualTo(2);
	}

	@Test
	public void testInvalidLanguageThrowsIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ScriptExecutorFactory.getScriptExecutor("foo"));
	}

}
