/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.aggregator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Button;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.core.Message;

/**
 * @author Alex Peters
 * 
 */
public class DefaultMessageAggregatorTests {

	DefaultMessageAggregator aggregator;

	@Before
	public void setUp() {
		aggregator = new DefaultMessageAggregator();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aggregateMessages_withMultiplePayloads_allAsListInResultMsg() {
		List<Serializable> anyPayloads = Arrays.asList("foo", "bar", 123L, new Button());
		List<Message<?>> messageGroup = new ArrayList<Message<?>>(anyPayloads.size());
		for (Serializable payload : anyPayloads) {
			Message<Serializable> mock = mock(Message.class);
			when(mock.getPayload()).thenReturn(payload);
			messageGroup.add(mock);
		}
		Message<?> result = aggregator.aggregateMessages(messageGroup);
		assertThat((List<Serializable>) result.getPayload(), is(anyPayloads));
	}
}
