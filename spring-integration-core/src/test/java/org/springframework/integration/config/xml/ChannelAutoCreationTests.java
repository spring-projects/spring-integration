/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class ChannelAutoCreationTests {

	@Test
	public void testEnablingAutoChannelCreationBeforeWithCustom() {
		assertThatNoException()
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"TestEnableChannelAutoCreation-before-context.xml", this.getClass()));
	}

	@Test
	public void testEnablingAutoChannelCreationAfterWithCustom() {
		assertThatNoException()
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"TestEnableChannelAutoCreation-after-context.xml", this.getClass()));
	}

	@Test
	public void testDisablingAutoChannelCreationAfter() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"TestDisableChannelAutoCreation-after-context.xml", getClass()));
	}

	@Test
	public void testDisablingAutoChannelCreationBefore() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"TestDisableChannelAutoCreation-before-context.xml", this.getClass()));
	}

}
