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

package org.springframework.integration.groovy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import groovy.lang.Script;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.AbstractResource;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.scripting.RefreshableResourceScriptSource;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class GroovyScriptExecutingMessageProcessorTests {

	private final AtomicInteger countHolder = new AtomicInteger();

	@Test
	public void testSimpleExecution() {
		int count = countHolder.getAndIncrement();
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar" + count).build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new ResourceScriptSource(resource);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource);
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo, header is bar" + count);
	}

	@Test
	public void testSimpleExecutionWithScriptVariableGenerator() {
		int count = countHolder.getAndIncrement();
		String script = "return \"payload is $payload, header is $headers.testHeader and date is $date\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar" + count).build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new ResourceScriptSource(resource);
		Object result = null;
		for (int i = 0; i < 5; i++) {
			ScriptVariableGenerator scriptVariableGenerator = new CustomScriptVariableGenerator();
			MessageProcessor<Object> processor =
					new GroovyScriptExecutingMessageProcessor(scriptSource, scriptVariableGenerator);
			Object newResult = processor.processMessage(message);
			assertThat(newResult.equals(result)).isFalse(); // make sure that we get different nanotime verifying that generateScriptVariables() is invoked
			result = newResult;
		}
	}

	@Test
	public void testLastModified() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		TestResource resource = new TestResource(script, "simpleTest");
		long lastModified = resource.lastModified();
		Thread.sleep(20L);
		resource.setScript("foo");
		assertThat(lastModified == resource.lastModified())
				.as("Expected last modified to change: " + lastModified + "==" + resource.lastModified()).isFalse();
	}

	@Test
	public void testModifiedScriptExecution() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new ResourceScriptSource(resource);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource);
		Thread.sleep(20L);
		resource.setScript("return \"payload is $payload\"");
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo");
	}

	@Test
	@Disabled("Very sensitive to the time")
	public void testRefreshableScriptExecution() throws Exception {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new RefreshableResourceScriptSource(resource, 2000L);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource);
		// should be the original script
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo, header is bar");
		//reset the script with the new string
		resource.setScript("return \"payload is $payload\"");
		Thread.sleep(20L);
		// should still assert to the old script because not enough time elapsed for refresh to kick in
		result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo, header is bar");
		// sleep some more, past refresh time
		Thread.sleep(2000L);
		// now you go the new script
		resource.setScript("return \"payload is $payload\"");
		result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo");
	}

	@Test
	public void testRefreshableScriptExecutionWithInfiniteDelay() {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new RefreshableResourceScriptSource(resource, -1L);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource);
		// process with the first script
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo, header is bar");
		// change script, but since refresh-delay is less then 0 we should still se old script executing
		resource.setScript("return \"payload is $payload\"");
		// process and see assert that the old script is used
		result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo, header is bar");
	}

	@Test
	public void testRefreshableScriptExecutionWithAlwaysRefresh() {
		String script = "return \"payload is $payload, header is $headers.testHeader\"";
		Message<?> message = MessageBuilder.withPayload("foo").setHeader("testHeader", "bar").build();
		TestResource resource = new TestResource(script, "simpleTest");
		ScriptSource scriptSource = new RefreshableResourceScriptSource(resource, 0);
		MessageProcessor<Object> processor = new GroovyScriptExecutingMessageProcessor(scriptSource);
		// process with the first script
		Object result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo, header is bar");
		// change script, but since refresh-delay is less than 0 we should still se old script executing
		resource.setScript("return \"payload is $payload\"");
		// process and see assert that the old script is used
		result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is foo");
		resource.setScript("return \"payload is 'hello'\"");
		// process and see assert that the old script is used
		result = processor.processMessage(message);
		assertThat(result.toString()).isEqualTo("payload is 'hello'");
	}

	@Test
	public void testInt3166GroovyScriptExecutingMessageProcessorPerformance() throws Exception {
		final Message<?> message = new GenericMessage<Object>("test");

		final AtomicInteger var1 = new AtomicInteger();
		final AtomicInteger var2 = new AtomicInteger();

		String script =
				"var1.incrementAndGet(); Thread.sleep(100); var2.set(Math.max(var1.get(), var2.get())); var1.decrementAndGet()";

		ScriptSource scriptSource = new StaticScriptSource(script, Script.class.getName());
		final MessageProcessor<Object> processor =
				new GroovyScriptExecutingMessageProcessor(scriptSource, message1 -> {
					Map<String, Object> variables = new HashMap<>(2);
					variables.put("var1", var1);
					variables.put("var2", var2);
					return variables;
				});

		ExecutorService executor = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 10; i++) {
			executor.execute(() -> processor.processMessage(message));
		}
		executor.shutdown();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

		assertThat(var2.get() > 1).isTrue();

	}

	private static class TestResource extends AbstractResource {

		private volatile String script;

		private final String filename;

		private volatile long lastModified;

		private TestResource(String script, String filename) {
			setScript(script);
			this.filename = filename;
		}

		@Override
		public long lastModified() throws IOException {
			return lastModified;
		}

		public void setScript(String script) {
			this.lastModified = System.nanoTime();
			this.script = script;
		}

		@Override
		public String getDescription() {
			return "test";
		}

		@Override
		public String getFilename() {
			return this.filename;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
		}

	}

	private static class CustomScriptVariableGenerator implements ScriptVariableGenerator {

		@Override
		public Map<String, Object> generateScriptVariables(Message<?> message) {
			Map<String, Object> variables = new HashMap<>();
			variables.put("date", System.nanoTime());
			variables.put("payload", message.getPayload());
			variables.put("headers", message.getHeaders());
			return variables;
		}

	}

}
