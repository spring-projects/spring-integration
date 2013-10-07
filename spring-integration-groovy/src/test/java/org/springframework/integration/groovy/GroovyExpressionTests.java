/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.groovy;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.Assert;

import groovy.lang.GroovyObject;
import groovy.lang.Script;

/**
 * @author Dave Syer
 * @author Artem Bilan
 */
public class GroovyExpressionTests {

	private static Log logger = LogFactory.getLog(GroovyExpressionTests.class);

	@Before
	public void setLogLevel() {
		LogManager.getLogger(getClass()).setLevel(Level.DEBUG);
	}

	@After
	public void resetLogLevel() {
		LogManager.getLogger(getClass()).setLevel(Level.INFO);
	}

	@Test
	public void testScriptFactoryCustomizer() throws Exception {
		Customizer customizer = new Customizer(Collections.singletonMap("name", (Object) "foo"));
		GroovyScriptExecutor executor = new GroovyScriptExecutor(customizer);
		ResourceScriptSource scriptSource = new ResourceScriptSource(new NamedByteArrayResource("\"name=${name}\"".getBytes(), "InlineScript"));
		Object scriptedObject = executor.executeScript(scriptSource);
		assertEquals("name=foo", scriptedObject.toString());
		customizer.setMap(Collections.singletonMap("name", (Object) "bar"));
		scriptedObject = executor.executeScript(scriptSource);
		assertEquals("name=bar", scriptedObject.toString());
	}

	@Test
	public void testScriptFactoryCustomizerThreadSafety() throws Exception {
		final Customizer customizer = new Customizer(Collections.singletonMap("name", (Object) "foo"));
		final GroovyScriptExecutor executor = new GroovyScriptExecutor(customizer);
		final ResourceScriptSource scriptSource = new ResourceScriptSource(new NamedByteArrayResource(
				"\"name=${name}\"".getBytes(), "InlineScript"));
		Object scriptedObject = executor.executeScript(scriptSource);
		assertEquals("name=foo", scriptedObject.toString());
		CompletionService<String> completionService = new ExecutorCompletionService<String>(Executors.newFixedThreadPool(10));
		for (int i = 0; i < 100; i++) {
			final String name = "bar" + i;
			completionService.submit(new Callable<String>() {
				public String call() throws Exception {
					Object scriptedObject;
					synchronized (customizer) {
						customizer.setMap(Collections.singletonMap("name", (Object) name));
						scriptedObject = executor.executeScript(scriptSource);
					}
					String result = scriptedObject.toString();
					logger.debug("Result=" + result + " with name=" + name);
					if (!("name=" + name).equals(result)) {
						throw new IllegalStateException("Wrong value (" + result + ") for: " + name);
					}
					return name;
				}
			});
		}
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < 100; i++) {
			set.add(completionService.take().get());
		}
		assertEquals(100, set.size());
	}

	@Test
	public void testScriptFactoryCustomizerStatic() throws Exception {
		final Customizer customizer = new Customizer(Collections.singletonMap("name", (Object) "foo"));
		final GroovyScriptExecutor executor = new GroovyScriptExecutor(customizer);
		final ResourceScriptSource scriptSource = new ResourceScriptSource(new NamedByteArrayResource(
				"\"name=${name}\"".getBytes(), "InlineScript"));
		Object scriptedObject = executor.executeScript(scriptSource);
		assertEquals("name=foo", scriptedObject.toString());
		CompletionService<String> completionService = new ExecutorCompletionService<String>(Executors.newFixedThreadPool(10));
		for (int i = 0; i < 100; i++) {
			final String name = "bar" + i;
			completionService.submit(new Callable<String>() {
				public String call() throws Exception {
					Object scriptedObject = executor.executeScript(scriptSource);
					String result = scriptedObject.toString();
					logger.debug("Result=" + result + " with name=" + name);
					if (!("name=foo").equals(result)) {
						throw new IllegalStateException("Wrong value (" + result + ") for: " + name);
					}
					return name;
				}
			});
		}
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < 100; i++) {
			set.add(completionService.take().get());
		}
		assertEquals(100, set.size());
	}

	@Test
	public void testScriptFactoryCustomizerThreadSafetyWithNewScript() throws Exception {
		final Customizer customizer = new Customizer(Collections.singletonMap("name", (Object) "foo"));
		final GroovyScriptExecutor executor = new GroovyScriptExecutor(customizer);
		CompletionService<String> completionService = new ExecutorCompletionService<String>(Executors.newFixedThreadPool(5));
		for (int i = 0; i < 100; i++) {
			final String name = "Bar" + i;
			completionService.submit(new Callable<String>() {
				public String call() throws Exception {
					Object scriptedObject;
					synchronized (customizer) {
						customizer.setMap(Collections.singletonMap("name", (Object) name));
						ResourceScriptSource scriptSource = new ResourceScriptSource(new NamedByteArrayResource(
								"\"name=${name}\"".getBytes(), "InlineScript" + name));
						scriptedObject = executor.executeScript(scriptSource);
					}
					String result = scriptedObject.toString();
					logger.debug("Result=" + result + " with name=" + name);
					if (!("name=" + name).equals(result)) {
						throw new IllegalStateException("Wrong value (" + result + ") for: " + name);
					}
					return name;
				}
			});
		}
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < 100; i++) {
			set.add(completionService.take().get());
		}
		assertEquals(100, set.size());
	}


	private static class Customizer implements GroovyObjectCustomizer {

		private Map<String, Object> map = new HashMap<String, Object>();

		public Customizer(Map<String, Object> map) {
			super();
			this.map.putAll(map);
		}

		public void customize(GroovyObject goo) {
			Assert.state(goo instanceof Script, "Expected a Script");
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				((Script) goo).getBinding().setVariable(entry.getKey(), entry.getValue());
			}
		}

		public void setMap(Map<String, Object> map) {
			this.map.clear();
			this.map.putAll(map);
		}
	}


	private static class NamedByteArrayResource extends ByteArrayResource {

		private final String fileName;

		public NamedByteArrayResource(byte[] bytes, String fileName) {
			super(bytes);
			this.fileName = fileName;
		}

		@Override
		public String getFilename() throws IllegalStateException {
			return fileName;
		}

	}

}
