/*
 * Copyright 2002-2022 the original author or authors.
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
