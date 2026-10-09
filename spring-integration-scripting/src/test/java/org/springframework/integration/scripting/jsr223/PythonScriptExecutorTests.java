/*
 * Copyright 2002-present the original author or authors.
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
