/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.message.selector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.selector.PayloadTypeSelector;

/**
 * @author Mark Fisher
 */
public class PayloadTypeSelectorTests {

	@Test
	public void testAcceptedTypeIsSelected() {
		PayloadTypeSelector selector = new PayloadTypeSelector(String.class);
		assertTrue(selector.accept(new GenericMessage<String>("test")));
	}

	@Test
	public void testNonAcceptedTypeIsNotSelected() {
		PayloadTypeSelector selector = new PayloadTypeSelector(Integer.class);
		assertFalse(selector.accept(new GenericMessage<String>("test")));
	}

	@Test
	public void testMultipleAcceptedTypes() {
		PayloadTypeSelector selector = new PayloadTypeSelector(String.class, Integer.class);
		assertTrue(selector.accept(new GenericMessage<String>("test1")));
		assertTrue(selector.accept(new GenericMessage<Integer>(2)));
		assertFalse(selector.accept(new ErrorMessage(new RuntimeException())));
	}

	@Test
	public void testSubclassOfAcceptedTypeIsSelected() {
		PayloadTypeSelector selector = new PayloadTypeSelector(RuntimeException.class);
		assertTrue(selector.accept(new ErrorMessage(new MessagingException("test"))));
	}

	@Test
	public void testSuperclassOfAcceptedTypeIsNotSelected() {
		PayloadTypeSelector selector = new PayloadTypeSelector(RuntimeException.class);
		assertFalse(selector.accept(new ErrorMessage(new Exception("test"))));
	}

}
