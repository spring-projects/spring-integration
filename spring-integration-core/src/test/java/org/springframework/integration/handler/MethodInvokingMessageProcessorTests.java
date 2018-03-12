/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.handler;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.UseSpelInvoker;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.handler.support.MessagingMethodInvokerHelper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MethodInvokingMessageProcessorTests {

	private static final Log logger = LogFactory.getLog(MethodInvokingMessageProcessorTests.class);

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void testHandlerInheritanceMethodImplInSuper() {
		class A {

			@SuppressWarnings("unused")
			public Message<String> myMethod(final Message<String> msg) {
				return MessageBuilder.fromMessage(msg).setHeader("A", "A").build();
			}
		}

		class B extends A {

		}

		@SuppressWarnings("unused")
		class C extends B {

		}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new B(), "myMethod");
		Message<?> message = (Message<?>) processor.processMessage(new GenericMessage<String>(""));
		assertEquals("A", message.getHeaders().get("A"));
	}

	@Test
	public void testHandlerInheritanceMethodImplInLatestSuper() {
		class A {

			@SuppressWarnings("unused")
			public Message<String> myMethod(Message<String> msg) {
				return MessageBuilder.fromMessage(msg).setHeader("A", "A").build();
			}
		}

		class B extends A {

			@Override
			public Message<String> myMethod(Message<String> msg) {
				return MessageBuilder.fromMessage(msg).setHeader("B", "B").build();
			}
		}

		@SuppressWarnings("unused")
		class C extends B {

		}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new B(), "myMethod");
		Message<?> message = (Message<?>) processor.processMessage(new GenericMessage<String>(""));
		assertEquals("B", message.getHeaders().get("B"));
	}

	public void testHandlerInheritanceMethodImplInSubClass() {
		class A {

			@SuppressWarnings("unused")
			public Message<String> myMethod(Message<String> msg) {
				return MessageBuilder.fromMessage(msg).setHeader("A", "A").build();
			}
		}

		class B extends A {

			@Override
			public Message<String> myMethod(Message<String> msg) {
				return MessageBuilder.fromMessage(msg).setHeader("B", "B").build();
			}
		}

		class C extends B {

			@Override
			public Message<String> myMethod(Message<String> msg) {
				return MessageBuilder.fromMessage(msg).setHeader("C", "C").build();
			}
		}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new C(), "myMethod");
		Message<?> message = (Message<?>) processor.processMessage(new GenericMessage<String>(""));
		assertEquals("C", message.getHeaders().get("C"));
	}

	public void testHandlerInheritanceMethodImplInSubClassAndSuper() {
		class A {

			@SuppressWarnings("unused")
			public Message<String> myMethod(Message<String> msg) {
				return MessageBuilder.fromMessage(msg).setHeader("A", "A").build();
			}
		}

		class B extends A {

		}

		class C extends B {

			@Override
			public Message<String> myMethod(Message<String> msg) {
				return MessageBuilder.fromMessage(msg).setHeader("C", "C").build();
			}
		}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new C(), "myMethod");
		Message<?> message = (Message<?>) processor.processMessage(new GenericMessage<String>(""));
		assertEquals("C", message.getHeaders().get("C"));
	}

	@Test
	public void payloadAsMethodParameterAndObjectAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"acceptPayloadAndReturnObject");
		Object result = processor.processMessage(new GenericMessage<String>("testing"));
		assertEquals("testing-1", result);
	}

	@Test
	public void testPayloadCoercedToString() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"acceptPayloadAndReturnObject");
		Object result = processor.processMessage(new GenericMessage<Integer>(123456789));
		assertEquals("123456789-1", result);
	}

	@Test
	public void payloadAsMethodParameterAndMessageAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"acceptPayloadAndReturnMessage");
		Message<?> result = (Message<?>) processor.processMessage(new GenericMessage<String>("testing"));
		assertEquals("testing-2", result.getPayload());
	}

	@Test
	public void messageAsMethodParameterAndObjectAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"acceptMessageAndReturnObject");
		Object result = processor.processMessage(new GenericMessage<String>("testing"));
		assertEquals("testing-3", result);
	}

	@Test
	public void messageAsMethodParameterAndMessageAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"acceptMessageAndReturnMessage");
		Message<?> result = (Message<?>) processor.processMessage(new GenericMessage<String>("testing"));
		assertEquals("testing-4", result.getPayload());
	}

	@Test
	public void messageSubclassAsMethodParameterAndMessageAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"acceptMessageSubclassAndReturnMessage");
		Message<?> result = (Message<?>) processor.processMessage(new GenericMessage<String>("testing"));
		assertEquals("testing-5", result.getPayload());
	}

	@Test
	public void messageSubclassAsMethodParameterAndMessageSubclassAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"acceptMessageSubclassAndReturnMessageSubclass");
		Message<?> result = (Message<?>) processor.processMessage(new GenericMessage<String>("testing"));
		assertEquals("testing-6", result.getPayload());
	}

	@Test
	public void payloadAndHeaderAnnotationMethodParametersAndObjectAsReturnValue() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"acceptPayloadAndHeaderAndReturnObject");
		Message<?> request = MessageBuilder.withPayload("testing").setHeader("number", 123).build();
		Object result = processor.processMessage(request);
		assertEquals("testing-123", result);
	}

	@Test
	public void testVoidMethodsIncludedByDefault() {
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new TestBean(),
				"testVoidReturningMethods");
		assertNull(processor.processMessage(MessageBuilder.withPayload("Something").build()));
		assertEquals(12, processor.processMessage(MessageBuilder.withPayload(12).build()));
	}

	@Test
	public void messageOnlyWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("messageOnly", Message.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Object result = processor.processMessage(new GenericMessage<String>("foo"));
		assertEquals("foo", result);
	}

	@Test
	public void payloadWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Object result = processor.processMessage(new GenericMessage<>(123));
		assertEquals(123, result);
	}

	@Test
	public void convertedPayloadWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Object result = processor.processMessage(new GenericMessage<String>("456"));
		assertEquals(456, result);
	}

	@Test(expected = MessageHandlingException.class)
	public void conversionFailureWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		processor.processMessage(new GenericMessage<String>("foo"));
	}

	@Test
	public void filterSelectsAnnotationMethodsOnly() {
		OverloadedMethodBean bean = new OverloadedMethodBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(bean, ServiceActivator.class);
		processor.processMessage(MessageBuilder.withPayload(123).build());
		assertNotNull(bean.lastArg);
		assertEquals(String.class, bean.lastArg.getClass());
		assertEquals("123", bean.lastArg);
	}

	@Test
	public void testProcessMessageBadExpression() throws Exception {
		expected.expect(MessageHandlingException.class);
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		assertEquals("foo", processor.processMessage(new GenericMessage<String>("foo")));
	}

	@Test
	public void testProcessMessageRuntimeException() throws Exception {
		expected.expect(new ExceptionCauseMatcher(UnsupportedOperationException.class));
		TestErrorService service = new TestErrorService();
		Method method = service.getClass().getMethod("error", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		assertEquals("foo", processor.processMessage(new GenericMessage<String>("foo")));
	}

	@Test
	public void testProcessMessageCheckedException() throws Exception {
		expected.expect(new ExceptionCauseMatcher(CheckedException.class));
		TestErrorService service = new TestErrorService();
		Method method = service.getClass().getMethod("checked", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		assertEquals("foo", processor.processMessage(new GenericMessage<String>("foo")));
	}

	@Test
	public void testProcessMessageMethodNotFound() throws Exception {
		expected.expect(new ExceptionCauseMatcher(SpelEvaluationException.class));
		TestDifferentErrorService service = new TestDifferentErrorService();
		Method method = TestErrorService.class.getMethod("checked", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		processor.setUseSpelInvoker(true);
		processor.processMessage(new GenericMessage<String>("foo"));
	}

	@Test
	public void messageAndHeaderWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("messageAndHeader", Message.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("number", 42).build();
		Object result = processor.processMessage(message);
		assertEquals("foo-42", result);
	}

	@Test
	public void multipleHeadersWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("twoHeaders", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("prop", "bar").setHeader("number", 42)
				.build();
		Object result = processor.processMessage(message);
		assertEquals("bar-42", result);
	}

	@Test
	public void optionalAndRequiredWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		processor.setUseSpelInvoker(true);
		optionalAndRequiredWithAnnotatedMethodGuts(processor, false);
	}

	@Test
	public void compiledOptionalAndRequiredWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		processor.setUseSpelInvoker(true);
		DirectFieldAccessor compilerConfigAccessor = compileImmediate(processor);
		optionalAndRequiredWithAnnotatedMethodGuts(processor, true);
		assertNotNull(TestUtils.getPropertyValue(processor, "delegate.handlerMethod.expression.compiledAst"));
		optionalAndRequiredWithAnnotatedMethodGuts(processor, true);
		compilerConfigAccessor.setPropertyValue("compilerMode", SpelCompilerMode.OFF);
	}

	private void optionalAndRequiredWithAnnotatedMethodGuts(MethodInvokingMessageProcessor processor,
			boolean compiled) {
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", 42)
				.build();
		Object result = processor.processMessage(message);
		assertEquals("null42", result);
		message = MessageBuilder.withPayload("foo")
				.setHeader("prop", "bar")
				.setHeader("num", 42)
				.build();
		result = processor.processMessage(message);
		assertEquals("bar42", result);
		message = MessageBuilder.withPayload("foo")
				.setHeader("prop", "bar")
				.build();
		try {
			result = processor.processMessage(message);
			fail("Expected MessageHandlingException");
		}
		catch (MessageHandlingException e) {
			if (compiled) {
				assertThat(e.getCause().getMessage(), equalTo("required header not available: num"));
			}
			else {
				assertThat(e.getCause().getCause().getMessage(), equalTo("required header not available: num"));
			}
		}
	}

	@Test
	public void optionalAndRequiredDottedWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("optionalAndRequiredDottedHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		processor.setUseSpelInvoker(true);
		optionalAndRequiredDottedWithAnnotatedMethodGuts(processor, false);
	}

	@Test
	public void compiledOptionalAndRequiredDottedWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("optionalAndRequiredDottedHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		processor.setUseSpelInvoker(true);
		DirectFieldAccessor compilerConfigAccessor = compileImmediate(processor);
		optionalAndRequiredDottedWithAnnotatedMethodGuts(processor, true);
		assertNotNull(TestUtils.getPropertyValue(processor, "delegate.handlerMethod.expression.compiledAst"));
		optionalAndRequiredDottedWithAnnotatedMethodGuts(processor, true);
		compilerConfigAccessor.setPropertyValue("compilerMode", SpelCompilerMode.OFF);
	}

	private void optionalAndRequiredDottedWithAnnotatedMethodGuts(MethodInvokingMessageProcessor processor,
			boolean compiled) {
		Message<String> message = MessageBuilder.withPayload("hello")
				.setHeader("dot2", new DotBean())
				.build();
		Object result = processor.processMessage(message);
		assertEquals("null42", result);
		message = MessageBuilder.withPayload("hello")
				.setHeader("dot1", new DotBean())
				.setHeader("dot2", new DotBean())
				.build();
		result = processor.processMessage(message);
		assertEquals("bar42", result);
		message = MessageBuilder.withPayload("hello")
				.setHeader("dot1", new DotBean())
				.build();
		try {
			result = processor.processMessage(message);
			fail("Expected MessageHandlingException");
		}
		catch (MessageHandlingException e) {
			if (compiled) {
				assertThat(e.getCause().getMessage(), equalTo("required header not available: dot2"));
			}
			else { // interpreted
				assertThat(e.getCause().getCause().getMessage(), equalTo("required header not available: dot2"));
			}
		}
	}

	@Test
	public void testOverloadedNonVoidReturningMethodsWithExactMatchForType() {
		AmbiguousMethodBean bean = new AmbiguousMethodBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(bean, "foo");
		processor.processMessage(MessageBuilder.withPayload("true").build());
		assertNotNull(bean.lastArg);
		assertEquals(String.class, bean.lastArg.getClass());
		assertEquals("true", bean.lastArg);
	}

	@Test
	public void gatewayTest() throws Exception {
		GatewayProxyFactoryBean gwFactoryBean = new GatewayProxyFactoryBean();
		gwFactoryBean.setBeanFactory(mock(BeanFactory.class));
		gwFactoryBean.afterPropertiesSet();
		Object target = gwFactoryBean.getObject();
		// just instantiate a helper with a simple target; we're going to invoke getTargetClass with reflection
		MessagingMethodInvokerHelper helper = new MessagingMethodInvokerHelper(new TestErrorService(), "error", true);

		Method method = MessagingMethodInvokerHelper.class.getDeclaredMethod("getTargetClass", Object.class);
		method.setAccessible(true);
		Object result = method.invoke(helper, target);
		assertSame(RequestReplyExchanger.class, result);
	}

	@Test
	public void testInt3199GenericTypeResolvingAndObjectMethod() throws Exception {

		class Foo {

			@SuppressWarnings("unused")
			public String handleMessage(Message<Number> message) {
				return "" + (message.getPayload().intValue() * 2);
			}

			@SuppressWarnings("unused")
			public String objectMethod(Integer foo) {
				return foo.toString();
			}

			@SuppressWarnings("unused")
			public String voidMethod() {
				return "foo";
			}

		}

		MessagingMethodInvokerHelper helper = new MessagingMethodInvokerHelper(new Foo(), (String) null, false);
		assertEquals("4", helper.process(new GenericMessage<Object>(2L)));
		assertEquals("1", helper.process(new GenericMessage<Object>(1)));
		assertEquals("foo", helper.process(new GenericMessage<Object>(new Date())));
	}

	@Test
	public void testInt3199GettersAmbiguity() throws Exception {

		class Foo {

			@SuppressWarnings("unused")
			public String getFoo() {
				return "foo";
			}

			@SuppressWarnings("unused")
			public String getBar() {
				return "foo";
			}
		}

		try {
			new MessagingMethodInvokerHelper(new Foo(), (String) null, false);
			fail("IllegalArgumentException expected");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(IllegalArgumentException.class));
			assertEquals("Found more than one method match for empty parameter for 'payload'", e.getMessage());
		}
	}

	@Test
	public void testInt3199MessageMethods() throws Exception {

		class Foo {

			@SuppressWarnings("unused")
			public String m1(Message<String> message) {
				return message.getPayload();
			}

			@SuppressWarnings("unused")
			public Integer m2(Message<Integer> message) {
				return message.getPayload();
			}

			@SuppressWarnings("unused")
			public Object m3(Message<?> message) {
				return message.getPayload();
			}

		}

		Foo targetObject = new Foo();

		MessagingMethodInvokerHelper helper = new MessagingMethodInvokerHelper(targetObject, (String) null, false);
		assertEquals("foo", helper.process(new GenericMessage<Object>("foo")));
		assertEquals(1, helper.process(new GenericMessage<Object>(1)));
		assertEquals(targetObject, helper.process(new GenericMessage<Object>(targetObject)));
	}

	@Test
	public void testInt3199TypedMethods() throws Exception {

		class Foo {

			@SuppressWarnings("unused")
			public String m1(String payload) {
				return payload;
			}

			@SuppressWarnings("unused")
			public Integer m2(Integer payload) {
				return payload;
			}

			@SuppressWarnings("unused")
			public Object m3(Object payload) {
				return payload;
			}

		}

		Foo targetObject = new Foo();

		MessagingMethodInvokerHelper helper = new MessagingMethodInvokerHelper(targetObject, (String) null, false);
		assertEquals("foo", helper.process(new GenericMessage<Object>("foo")));
		assertEquals(1, helper.process(new GenericMessage<Object>(1)));
		assertEquals(targetObject, helper.process(new GenericMessage<Object>(targetObject)));
	}

	@Test
	public void testInt3199PrecedenceOfCandidates() throws Exception {

		class Foo {

			@SuppressWarnings("unused")
			public Object m1(Message<String> message) {
				fail("This method must not be invoked");
				return message;
			}

			@SuppressWarnings("unused")
			public Object m2(String payload) {
				return payload;
			}

			@SuppressWarnings("unused")
			public Object m3() {
				return "FOO";
			}
		}

		Foo targetObject = new Foo();

		MessagingMethodInvokerHelper helper = new MessagingMethodInvokerHelper(targetObject, (String) null, false);
		assertEquals("foo", helper.process(new GenericMessage<Object>("foo")));
		assertEquals("FOO", helper.process(new GenericMessage<Object>(targetObject)));
	}

	@Test
	public void testIneligible() {
		IneligibleMethodBean bean = new IneligibleMethodBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(bean, "foo");
		processor.processMessage(MessageBuilder.withPayload("true").build());
		assertNotNull(bean.lastArg);
		assertEquals(String.class, bean.lastArg.getClass());
		assertEquals("true", bean.lastArg);
	}

	@Test
	public void testOptionalArgs() throws Exception {
		class Foo {

			private final Map<String, Object> arguments = new LinkedHashMap<String, Object>();

			@SuppressWarnings("unused")
			public void optionalHeaders(Optional<String> foo, @Header(value = "foo", required = false) String foo1,
					@Header("foo") Optional<String> foo2) {
				this.arguments.put("foo", (foo.isPresent() ? foo.get() : null));
				this.arguments.put("foo1", foo1);
				this.arguments.put("foo2", (foo2.isPresent() ? foo2.get() : null));
			}

		}

		Foo targetObject = new Foo();

		MessagingMethodInvokerHelper helper = new MessagingMethodInvokerHelper(targetObject, (String) null, false);

		helper.process(new GenericMessage<>(Optional.empty()));
		assertNull(targetObject.arguments.get("foo"));
		assertNull(targetObject.arguments.get("foo1"));
		assertNull(targetObject.arguments.get("foo2"));

		helper.process(MessageBuilder.withPayload("foo").setHeader("foo", "FOO").build());
		assertEquals("foo", targetObject.arguments.get("foo"));
		assertEquals("FOO", targetObject.arguments.get("foo1"));
		assertEquals("FOO", targetObject.arguments.get("foo2"));
	}

	@Test
	public void testPrivateMethod() throws Exception {
		class Foo {

			@ServiceActivator
			private String service(String payload) {
				return payload.toUpperCase();
			}

		}

		MessagingMethodInvokerHelper helper = new MessagingMethodInvokerHelper(new Foo(), ServiceActivator.class, false);

		assertEquals("FOO", helper.process(new GenericMessage<>("foo")));
		assertEquals("BAR", helper.process(new GenericMessage<>("bar")));
	}

	@Test
	public void testPerformanceSpelVersusInvocable() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, method);
		processor.setUseSpelInvoker(true);

		Message<Integer> message = MessageBuilder.withPayload(42).build();

		StopWatch stopWatch = new StopWatch("SpEL vs Invocable Performance");

		int count = 20_000;

		stopWatch.start("SpEL");
		for (int i = 0; i < count; i++) {
			processor.processMessage(message);
		}
		stopWatch.stop();

		processor = new MethodInvokingMessageProcessor(service, method);

		stopWatch.start("Invocable");
		for (int i = 0; i < count; i++) {
			processor.processMessage(message);
		}
		stopWatch.stop();

		DirectFieldAccessor compilerConfigAccessor = compileImmediate(processor);

		processor = new MethodInvokingMessageProcessor(service, method);
		processor.setUseSpelInvoker(true);

		stopWatch.start("Compiled SpEL");
		for (int i = 0; i < count; i++) {
			processor.processMessage(message);
		}
		stopWatch.stop();

		logger.warn(stopWatch.prettyPrint());
		compilerConfigAccessor.setPropertyValue("compilerMode", SpelCompilerMode.OFF);
	}


	@Test
	public void testNoSpElFallbackWhenUserException() {
		class A {

			@SuppressWarnings("unused")
			public void myMethod(Object payload) {
				throw new IllegalStateException(new IllegalArgumentException("argument type mismatch"));
			}
		}

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new A(), "myMethod");

		try {
			processor.processMessage(new GenericMessage<>("foo"));
		}
		catch (Exception e) {
			assertThat(e.getCause(), instanceOf(IllegalStateException.class));
			assertThat(e.getCause().getCause(), instanceOf(IllegalArgumentException.class));
			assertEquals(A.class.getName(), e.getCause().getStackTrace()[0].getClassName());
		}

		assertEquals(0,
				TestUtils.getPropertyValue(processor, "delegate.handlerMethod.failedAttempts"));

	}

	@Test
	public void testProxyInvocation() {
		final AtomicReference<Object> result = new AtomicReference<>();

		class MyHandler implements MessageHandler {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				result.set(message.getPayload());
			}

		}

		MessageHandler service = new MyHandler();
		final AtomicBoolean adviceCalled = new AtomicBoolean();
		ProxyFactory proxyFactory = new ProxyFactory(service);
		proxyFactory.addAdvice((MethodInterceptor) i -> {
			adviceCalled.set(true);
			return i.proceed();
		});
		service = (MessageHandler) proxyFactory.getProxy(getClass().getClassLoader());

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, "handleMessage");

		processor.processMessage(new GenericMessage<>("foo"));

		assertEquals("foo", result.get());
		assertTrue(adviceCalled.get());
	}

	@Test
	public void testProxyAndHeaderAnnotation() {
		final AtomicReference<Object> payloadReference = new AtomicReference<>();
		final AtomicReference<UUID> idReference = new AtomicReference<>();

		class MyHandler {

			public void handle(@Header(MessageHeaders.ID) UUID id, @Payload Object payload) {
				idReference.set(id);
				payloadReference.set(payload);
			}

		}

		MyHandler service = new MyHandler();

		final AtomicBoolean adviceCalled = new AtomicBoolean();
		ProxyFactory proxyFactory = new ProxyFactory(service);
		proxyFactory.addAdvice((MethodInterceptor) i -> {
			adviceCalled.set(true);
			return i.proceed();
		});
		service = (MyHandler) proxyFactory.getProxy(getClass().getClassLoader());


		GenericMessage<String> testMessage = new GenericMessage<>("foo");

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(service, "handle");

		processor.processMessage(testMessage);

		assertEquals(testMessage.getPayload(), payloadReference.get());
		assertEquals(testMessage.getHeaders().getId(), idReference.get());
		assertTrue(adviceCalled.get());
	}

	@Test
	public void testUseSpelInvoker() throws Exception {
		UseSpelInvokerBean bean = new UseSpelInvokerBean();
		MessagingMethodInvokerHelper<?> helper = new MessagingMethodInvokerHelper<>(bean,
				UseSpelInvokerBean.class.getDeclaredMethod("foo", String.class), false);
		Message<?> message = new GenericMessage<>("Test");
		helper.process(message);
		assertEquals(SpelCompilerMode.OFF,
				TestUtils.getPropertyValue(helper, "handlerMethod.expression.configuration.compilerMode"));

		helper = new MessagingMethodInvokerHelper<>(bean,
				UseSpelInvokerBean.class.getDeclaredMethod("bar", String.class), false);
		helper.process(message);
		assertEquals(SpelCompilerMode.IMMEDIATE,
				TestUtils.getPropertyValue(helper, "handlerMethod.expression.configuration.compilerMode"));

		helper = new MessagingMethodInvokerHelper<>(bean,
				UseSpelInvokerBean.class.getDeclaredMethod("baz", String.class), false);
		helper.process(message);
		assertEquals(SpelCompilerMode.MIXED,
				TestUtils.getPropertyValue(helper, "handlerMethod.expression.configuration.compilerMode"));

		helper = new MessagingMethodInvokerHelper<>(bean,
				UseSpelInvokerBean.class.getDeclaredMethod("qux", String.class), false);
		helper.process(message);
		assertEquals(SpelCompilerMode.OFF,
				TestUtils.getPropertyValue(helper, "handlerMethod.expression.configuration.compilerMode"));

		helper = new MessagingMethodInvokerHelper<>(bean,
				UseSpelInvokerBean.class.getDeclaredMethod("fiz", String.class), false);
		try {
			helper.process(message);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(),
					equalTo("No enum constant org.springframework.expression.spel.SpelCompilerMode.JUNK"));
		}

		helper = new MessagingMethodInvokerHelper<>(bean,
				UseSpelInvokerBean.class.getDeclaredMethod("buz", String.class), false);
		ConfigurableListableBeanFactory bf = mock(ConfigurableListableBeanFactory.class);
		willAnswer(returnsFirstArg()).given(bf).resolveEmbeddedValue(anyString());
		helper.setBeanFactory(bf);
		try {
			helper.process(message);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), equalTo(
					"UseSpelInvoker.compilerMode: Object of class [java.lang.Object] "
							+ "must be an instance of class java.lang.String"));
		}

		// Check other CTORs
		helper = new MessagingMethodInvokerHelper<>(bean, "bar", false);
		helper.process(message);
		assertEquals(SpelCompilerMode.IMMEDIATE,
				TestUtils.getPropertyValue(helper, "handlerMethod.expression.configuration.compilerMode"));

		helper = new MessagingMethodInvokerHelper<>(bean, ServiceActivator.class, false);
		helper.process(message);
		assertEquals(SpelCompilerMode.MIXED,
				TestUtils.getPropertyValue(helper, "handlerMethod.expression.configuration.compilerMode"));
	}

	@Test
	public void testSingleMethodJson() throws Exception {
		SingleMethodJsonWithSpELBean bean = new SingleMethodJsonWithSpELBean();
		MessagingMethodInvokerHelper<?> helper = new MessagingMethodInvokerHelper<>(bean,
				SingleMethodJsonWithSpELBean.class.getDeclaredMethod("foo",
						SingleMethodJsonWithSpELBean.Foo.class),
				false);
		Message<?> message = new GenericMessage<>("{\"bar\":\"bar\"}",
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, "application/json"));
		helper.process(message);
		assertThat(bean.foo.bar, equalTo("bar"));
	}

	@Test
	public void testSingleMethodBadJson() throws Exception {
		SingleMethodJsonWithSpELMessageWildBean bean = new SingleMethodJsonWithSpELMessageWildBean();
		MessagingMethodInvokerHelper<?> helper = new MessagingMethodInvokerHelper<>(bean,
				SingleMethodJsonWithSpELMessageWildBean.class.getDeclaredMethod("foo", Message.class), false);
		Message<?> message = new GenericMessage<>("baz",
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, "application/json"));
		helper.process(message);
		assertThat(bean.foo.getPayload(), equalTo("baz"));
	}

	@Test
	public void testSingleMethodJsonMessageFoo() throws Exception {
		SingleMethodJsonWithSpELMessageFooBean bean = new SingleMethodJsonWithSpELMessageFooBean();
		MessagingMethodInvokerHelper<?> helper = new MessagingMethodInvokerHelper<>(bean,
				SingleMethodJsonWithSpELMessageFooBean.class.getDeclaredMethod("foo", Message.class), false);
		Message<?> message = new GenericMessage<>("{\"bar\":\"bar\"}",
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, "application/json"));
		helper.process(message);
		assertThat(bean.foo.getPayload().bar, equalTo("bar"));
	}

	@Test
	public void testSingleMethodJsonMessageWild() throws Exception {
		SingleMethodJsonWithSpELMessageWildBean bean = new SingleMethodJsonWithSpELMessageWildBean();
		MessagingMethodInvokerHelper<?> helper = new MessagingMethodInvokerHelper<>(bean,
				SingleMethodJsonWithSpELMessageWildBean.class.getDeclaredMethod("foo", Message.class), false);
		Message<?> message = new GenericMessage<>("{\"bar\":\"baz\"}",
				Collections.singletonMap(MessageHeaders.CONTENT_TYPE, "application/json"));
		helper.process(message);
		assertThat(bean.foo.getPayload(), instanceOf(Map.class));
		assertThat(((Map<?, ?>) bean.foo.getPayload()).get("bar"), equalTo("baz"));
	}

	@Test
	public void testCompiledSpELForProxy() {
		Foo foo = new FooImpl();

		foo = (Foo) new ProxyFactory(foo).getProxy();

		SpelExpressionParser expressionParser =
				new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));

		Expression expression = expressionParser.parseExpression("#target.handle(#root)");

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.setVariable("target", foo);

		expression.getValue(evaluationContext, "foo", String.class); // Twice to make a compiler to work
		String result = expression.getValue(evaluationContext, "foo", String.class);

		assertEquals("FOO", result);
	}


	@Test
	public void testCollectionArgument() throws JsonProcessingException {

		class A {

			@SuppressWarnings("unused")
			public String myMethod(List<Employee<Person>> msg) {
				return msg.stream()
						.map(Employee::getEntity)
						.map(Person::getName)
						.collect(Collectors.joining(","));
			}

		}

		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(TestConfiguration.class);

		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(new A(), "myMethod");
		processor.setBeanFactory(applicationContext);

		List<Employee<Person>> testData =
				Arrays.asList(
						new Employee<>(new Person("Foo")),
						new Employee<>(new Person("Bar")));

		ObjectMapper objectMapper = new ObjectMapper();
		byte[] value = objectMapper.writeValueAsBytes(testData);

		Message<?> testMessage =
				MessageBuilder.withPayload(value)
						.setHeader(MessageHeaders.CONTENT_TYPE, "application/json")
						.build();

		String result = (String) processor.processMessage(testMessage);

		assertEquals("Foo,Bar", result);
	}

	public static class Employee<T> {

		private T entity;

		public Employee() {
		}

		public Employee(T entity) {
			this.entity = entity;
		}

		public void setEntity(T entity) {
			this.entity = entity;
		}

		public T getEntity() {
			return this.entity;
		}

	}

	public static class Person {

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	@Configuration
	@EnableIntegration
	public static class TestConfiguration {

	}

	public interface Foo {

		String handle(String payload);

	}

	public class FooImpl implements Foo {

		@Override
		public String handle(String payload) {
			return payload.toUpperCase();
		}

	}

	private DirectFieldAccessor compileImmediate(MethodInvokingMessageProcessor processor) {
		// Update the parser configuration compiler mode
		SpelParserConfiguration config = TestUtils.getPropertyValue(processor,
				"delegate.EXPRESSION_PARSER.configuration", SpelParserConfiguration.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(config);
		accessor.setPropertyValue("compilerMode", SpelCompilerMode.IMMEDIATE);
		return accessor;
	}

	private static class ExceptionCauseMatcher extends TypeSafeMatcher<Exception> {

		private Throwable cause;

		private final Class<? extends Exception> type;

		ExceptionCauseMatcher(Class<? extends Exception> type) {
			this.type = type;
		}

		@Override
		public boolean matchesSafely(Exception item) {
			cause = item.getCause();
			assertNotNull("There is no cause for " + item, cause);
			return type.isAssignableFrom(cause.getClass());
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("cause to be ").appendValue(type).appendText("but was ").appendValue(cause);
		}

	}

	@SuppressWarnings("unused")
	private static class TestErrorService {

		TestErrorService() {
			super();
		}

		public String error(String input) {
			throw new UnsupportedOperationException("Expected test exception");
		}

		public String checked(String input) throws Exception {
			throw new CheckedException("Expected test exception");
		}

	}

	@SuppressWarnings("unused")
	private static class TestDifferentErrorService {

		TestDifferentErrorService() {
			super();
		}

		public String checked(String input) throws Exception {
			throw new CheckedException("Expected test exception");
		}

	}

	@SuppressWarnings("serial")
	private static final class CheckedException extends Exception {

		CheckedException(String string) {
			super(string);
		}

	}

	@SuppressWarnings("unused")
	private static class TestBean {

		TestBean() {
			super();
		}

		public String acceptPayloadAndReturnObject(String s) {
			return s + "-1";
		}

		public Message<?> acceptPayloadAndReturnMessage(String s) {
			return new GenericMessage<String>(s + "-2");
		}

		public String acceptMessageAndReturnObject(Message<?> m) {
			return m.getPayload() + "-3";
		}

		public Message<?> acceptMessageAndReturnMessage(Message<?> m) {
			return new GenericMessage<String>(m.getPayload() + "-4");
		}

		public Message<?> acceptMessageSubclassAndReturnMessage(GenericMessage<String> m) {
			return new GenericMessage<String>(m.getPayload() + "-5");
		}

		public GenericMessage<String> acceptMessageSubclassAndReturnMessageSubclass(GenericMessage<String> m) {
			return new GenericMessage<String>(m.getPayload() + "-6");
		}

		public String acceptPayloadAndHeaderAndReturnObject(String s, @Header("number") Integer n) {
			return s + "-" + n;
		}

		public void testVoidReturningMethods(String s) {
			// do nothing
		}

		public int testVoidReturningMethods(int i) {
			return i;
		}

	}

	public static class AnnotatedTestService {

		AnnotatedTestService() {
			super();
		}

		public String messageOnly(Message<?> message) {
			return (String) message.getPayload();
		}

		public String messageAndHeader(Message<?> message, @Header("number") Integer num) {
			return (String) message.getPayload() + "-" + num.toString();
		}

		public String twoHeaders(@Header String prop, @Header("number") Integer num) {
			return prop + "-" + num.toString();
		}

		public Integer optionalHeader(@Header(required = false) Integer num) {
			return num;
		}

		public Integer requiredHeader(@Header("num") Integer num) {
			return num;
		}

		public String optionalAndRequiredHeader(@Header(required = false) String prop,
				@Header("num") Integer num) {
			return prop + num;
		}

		public String optionalAndRequiredDottedHeader(@Header(name = "dot1.foo", required = false) String prop,
				@Header(name = "dot2.baz") Integer num) {
			return prop + num;
		}

		public Properties propertiesMethod(Properties properties) {
			return properties;
		}

		public Map mapMethod(Map map) {
			return map;
		}

		public Integer integerMethod(Integer i) {
			return i;
		}

	}

	/**
	 * Method names create ambiguities, but the MethodResolver implementation should filter out based on the annotation
	 * or the 'requiresReply' flag.
	 */
	@SuppressWarnings("unused")
	private static class AmbiguousMethodBean {

		private volatile Object lastArg = null;

		AmbiguousMethodBean() {
			super();
		}

		public void foo(boolean b) {
			this.lastArg = b;
		}

		public String foo(String s) {
			this.lastArg = s;
			return s;
		}

		public String foo(int i) {
			this.lastArg = i;
			return Integer.valueOf(i).toString();
		}

	}

	/**
	 * Method names create ambiguities, but the MethodResolver implementation should filter out based on the annotation
	 * or the 'requiresReply' flag.
	 */
	@SuppressWarnings("unused")
	private static class OverloadedMethodBean {

		private volatile Object lastArg = null;

		OverloadedMethodBean() {
			super();
		}

		public void foo(boolean b) {
			this.lastArg = b;
		}

		@ServiceActivator
		public String foo(String s) {
			this.lastArg = s;
			return s;
		}

	}

	private static class IneligibleMethodBean {

		private volatile Object lastArg = null;

		IneligibleMethodBean() {
			super();
		}

		@SuppressWarnings("unused")
		public void foo(String s) {
			this.lastArg = s;
		}

		@SuppressWarnings("unused")
		public void foo(String s, int i) {
			throw new RuntimeException("expected ineligible");
		}

	}

	private static class UseSpelInvokerBean {

		UseSpelInvokerBean() {
			super();
		}

		@UseSpelInvoker
		public void foo(String foo) {
			// empty
		}

		@UseSpelInvoker("IMMEDIATE")
		public void bar(String bar) {
			// empty
		}

		@ServiceActivator
		@UseSpelInvoker("mixed")
		public void baz(String baz) {
			// empty
		}

		@UseSpelInvoker("OfF")
		public void qux(String qux) {
			// empty
		}

		@UseSpelInvoker("JUNK")
		public void fiz(String fiz) {
			// empty
		}

		@UseSpelInvoker("#{new Object()}")
		public void buz(String buz) {
			// empty
		}

	}

	public static class SingleMethodJsonWithSpELBean {

		private Foo foo;

		private final CountDownLatch latch = new CountDownLatch(1);

		@ServiceActivator(inputChannel = "foo")
		@UseSpelInvoker
		public void foo(Foo foo) {
			this.foo = foo;
			this.latch.countDown();
		}

		public static class Foo {

			private String bar;

			public String getBar() {
				return this.bar;
			}

			public void setBar(String bar) {
				this.bar = bar;
			}

			@Override
			public String toString() {
				return "Foo [bar=" + this.bar + "]";
			}

		}

	}

	public static class SingleMethodJsonWithSpELMessageFooBean {

		private Message<Foo> foo;

		private final CountDownLatch latch = new CountDownLatch(1);

		@ServiceActivator(inputChannel = "foo")
		@UseSpelInvoker
		public void foo(Message<Foo> foo) {
			this.foo = foo;
			this.latch.countDown();
		}

		public static class Foo {

			private String bar;

			public String getBar() {
				return this.bar;
			}

			public void setBar(String bar) {
				this.bar = bar;
			}

			@Override
			public String toString() {
				return "Foo [bar=" + this.bar + "]";
			}

		}

	}

	public static class SingleMethodJsonWithSpELMessageWildBean {

		private Message<?> foo;

		private final CountDownLatch latch = new CountDownLatch(1);

		@ServiceActivator(inputChannel = "foo")
		@UseSpelInvoker
		public void foo(Message<?> foo) {
			this.foo = foo;
			this.latch.countDown();
		}

	}

	/*
	 * Public for SpEL access.
	 */
	public static class DotBean {

		private final String foo = "bar";

		private final Integer baz = 42;

		public String getFoo() {
			return this.foo;
		}

		public Integer getBaz() {
			return this.baz;
		}

	}

}
