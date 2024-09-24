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

package org.springframework.integration.scripting.jsr223;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.scripting.PolyglotScriptExecutor;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class PythonScriptExecutorTests {

	private ScriptExecutor executor;

	@BeforeEach
	public void init() {
		this.executor = new PolyglotScriptExecutor("python");
	}

	@Test
	public void testLiteral() {
		Object obj = this.executor.executeScript(new StaticScriptSource("3+4"));
		assertThat(obj).isEqualTo(7);

		obj = this.executor.executeScript(new StaticScriptSource("'hello,world'"));
		assertThat(obj).isEqualTo("hello,world");
	}

	@Test

	public void test1() {
		Object obj = this.executor.executeScript(new StaticScriptSource("x=2"));
		assertThat(obj).isEqualTo(2);
	}

	@Test
	public void test2() {
		Object obj = this.executor.executeScript(new StaticScriptSource("def foo(y):\n\tx=y\n\treturn y\nz=foo(2)"));
		assertThat(obj).isEqualTo(2);
	}

	@Test
	public void test3() {
		ScriptSource source =
				new ResourceScriptSource(
						new ClassPathResource("/org/springframework/integration/scripting/jsr223/test3.py"));
		Object obj = this.executor.executeScript(source);
		assertThat(obj)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.containsOnly(1, 2, 3);
	}

	@Test
	public void test3WithVariables() {
		ScriptSource source =
				new ResourceScriptSource(
						new ClassPathResource("/org/springframework/integration/scripting/jsr223/test3.py"));
		HashMap<String, Object> variables = new HashMap<>();
		variables.put("foo", "bar");
		Object obj = this.executor.executeScript(source, variables);
		assertThat(obj)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.containsOnly(1, 2, 3);
	}

	@Test
	public void testEmbeddedVariable() {
		Map<String, Object> variables = new HashMap<>();
		variables.put("scope", "world");
		Object obj = this.executor.executeScript(new StaticScriptSource("\"hello, %s\"% scope"), variables);
		assertThat(obj).isEqualTo("hello, world");
	}

}
