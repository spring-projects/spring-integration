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
