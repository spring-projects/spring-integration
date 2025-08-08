/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.filter;

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class MethodInvokingSelectorTests {

	@Test
	public void acceptedWithMethodName() {
		MethodInvokingSelector selector = new MethodInvokingSelector(new TestBean(), "acceptString");
		selector.setBeanFactory(mock(BeanFactory.class));
		assertThat(selector.accept(new GenericMessage<>("should accept"))).isTrue();
	}

	@Test
	public void acceptedWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method method = testBean.getClass().getMethod("acceptString", Message.class);
		MethodInvokingSelector selector = new MethodInvokingSelector(testBean, method);
		selector.setBeanFactory(mock(BeanFactory.class));
		assertThat(selector.accept(new GenericMessage<>("should accept"))).isTrue();
	}

	@Test
	public void rejectedWithMethodName() {
		MethodInvokingSelector selector = new MethodInvokingSelector(new TestBean(), "acceptString");
		selector.setBeanFactory(mock(BeanFactory.class));
		assertThat(selector.accept(new GenericMessage<>(99))).isFalse();
	}

	@Test
	public void rejectedWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method method = testBean.getClass().getMethod("acceptString", Message.class);
		MethodInvokingSelector selector = new MethodInvokingSelector(testBean, method);
		selector.setBeanFactory(mock(BeanFactory.class));
		assertThat(selector.accept(new GenericMessage<>(99))).isFalse();
	}

	@Test
	public void noArgMethodWithMethodName() {
		MethodInvokingSelector selector = new MethodInvokingSelector(new TestBean(), "noArgs");
		selector.setBeanFactory(mock(BeanFactory.class));
		assertThat(selector.accept(new GenericMessage<>("test"))).isTrue();
	}

	@Test
	public void noArgMethodWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method method = testBean.getClass().getMethod("noArgs");
		MethodInvokingSelector selector = new MethodInvokingSelector(testBean, method);
		selector.setBeanFactory(mock(BeanFactory.class));
		assertThat(selector.accept(new GenericMessage<>("test"))).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void voidReturningMethodWithMethodName() {
		MethodInvokingSelector selector = new MethodInvokingSelector(new TestBean(), "returnVoid");
		selector.setBeanFactory(mock(BeanFactory.class));
		selector.accept(new GenericMessage<>("test"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void voidReturningMethodWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method method = testBean.getClass().getMethod("returnVoid", Message.class);
		MethodInvokingSelector selector = new MethodInvokingSelector(testBean, method);
		selector.setBeanFactory(mock(BeanFactory.class));
		selector.accept(new GenericMessage<>("test"));
	}

	@SuppressWarnings("unused")
	private static class TestBean {

		TestBean() {
			super();
		}

		public boolean acceptString(Message<?> message) {
			return (message.getPayload() instanceof String);
		}

		public void returnVoid(Message<?> message) {
		}

		public boolean noArgs() {
			return true;
		}

	}

}
