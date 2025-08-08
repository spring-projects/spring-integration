/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.message.selector;

import org.junit.Test;

import org.springframework.integration.selector.PayloadTypeSelector;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class PayloadTypeSelectorTests {

	@Test
	public void testAcceptedTypeIsSelected() {
		PayloadTypeSelector selector = new PayloadTypeSelector(String.class);
		assertThat(selector.accept(new GenericMessage<>("test"))).isTrue();
	}

	@Test
	public void testNonAcceptedTypeIsNotSelected() {
		PayloadTypeSelector selector = new PayloadTypeSelector(Integer.class);
		assertThat(selector.accept(new GenericMessage<>("test"))).isFalse();
	}

	@Test
	public void testMultipleAcceptedTypes() {
		PayloadTypeSelector selector = new PayloadTypeSelector(String.class, Integer.class);
		assertThat(selector.accept(new GenericMessage<>("test1"))).isTrue();
		assertThat(selector.accept(new GenericMessage<>(2))).isTrue();
		assertThat(selector.accept(new ErrorMessage(new RuntimeException()))).isFalse();
	}

	@Test
	public void testSubclassOfAcceptedTypeIsSelected() {
		PayloadTypeSelector selector = new PayloadTypeSelector(RuntimeException.class);
		assertThat(selector.accept(new ErrorMessage(new MessagingException("test")))).isTrue();
	}

	@Test
	public void testSuperclassOfAcceptedTypeIsNotSelected() {
		PayloadTypeSelector selector = new PayloadTypeSelector(RuntimeException.class);
		assertThat(selector.accept(new ErrorMessage(new Exception("test")))).isFalse();
	}

}
