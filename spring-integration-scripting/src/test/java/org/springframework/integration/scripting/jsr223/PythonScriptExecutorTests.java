/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.scripting.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.python.core.PyTuple;

import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

/**
 * @author David Turanski
 * @author Gary Russell
 *
 */
public class PythonScriptExecutorTests {
	ScriptExecutor executor;
	@Before
	public void init() {
		 executor = new PythonScriptExecutor();
	}

	@Test
	public void testLiteral() {
		Object obj = executor.executeScript(new StaticScriptSource("3+4"));
		assertEquals(7, obj);

		 obj = executor.executeScript(new StaticScriptSource("'hello,world'"));
		 assertEquals("hello,world", obj);
	}

	@Test

	public void test1() {
		Object obj = executor.executeScript(new StaticScriptSource("x=2"));
		assertEquals(2, obj);
	}

	@Test
	public void test2() {
		Object obj =  executor.executeScript(new StaticScriptSource("def foo(y):\n\tx=y\n\treturn y\nz=foo(2)"));
		assertEquals(2, obj);
	}

	@Test
	public void test3() {
		ScriptSource source =
			new ResourceScriptSource(
					new ClassPathResource("/org/springframework/integration/scripting/jsr223/test3.py"));
		Object obj =  executor.executeScript(source);
		PyTuple tuple = (PyTuple) obj;
		assertEquals(1, tuple.get(0));
	}

	@Test
	public void test3WithVariables() {
		ScriptSource source =
			new ResourceScriptSource(
					new ClassPathResource("/org/springframework/integration/scripting/jsr223/test3.py"));
		HashMap<String, Object> variables = new HashMap<String, Object>();
		variables.put("foo", "bar");
		Object obj =  executor.executeScript(source, variables);
		PyTuple tuple = (PyTuple) obj;
		assertNotNull(tuple);
		assertEquals(1, tuple.get(0));
	}

	@Test
	public void testEmbeddedVariable() {
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("scope", "world");
		Object obj = executor.executeScript(new StaticScriptSource("\"hello, %s\"% scope"), variables);
		assertEquals("hello, world", obj);
	}
}
