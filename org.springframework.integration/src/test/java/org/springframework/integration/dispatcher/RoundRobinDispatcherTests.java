/* Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.dispatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
@RunWith(MockitoJUnitRunner.class)
public class RoundRobinDispatcherTests {

	private UnicastingDispatcher dispatcher = new UnicastingDispatcher();

	@Mock
	private MessageHandler handler;

	@Mock
	private Message<?> message;

	@Mock
	private MessageHandler differentHandler;

	@Before
	public void setupDispatcher() {
		this.dispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
	}


	@Test
	public void dispatchMessageWithSingleHandler() throws Exception {
		dispatcher.addHandler(handler);
		dispatcher.dispatch(message);
	}

	@Test
	public void differentHandlerInvokedOnSecondMessage() throws Exception {
		dispatcher.addHandler(handler);
		dispatcher.addHandler(differentHandler);
		dispatcher.dispatch(message);
		dispatcher.dispatch(message);
		verify(handler).handleMessage(message);
		verify(differentHandler).handleMessage(message);
	}

	@Test
	public void multipleCyclesThroughHandlers() throws Exception {
		dispatcher.addHandler(handler);
		dispatcher.addHandler(differentHandler);
		for (int i = 0; i < 7; i++) {
			dispatcher.dispatch(message);
		}
		verify(handler, times(4)).handleMessage(message);
		verify(differentHandler, times(3)).handleMessage(message);		
	}

	@Test
	public void currentHandlerIndexOverFlow() throws Exception {
		dispatcher.addHandler(handler);
		dispatcher.addHandler(differentHandler);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(dispatcher).getPropertyValue("loadBalancingStrategy"));
		((AtomicInteger) accessor.getPropertyValue("currentHandlerIndex")).set(Integer.MAX_VALUE-5);
		for(int i = 0; i < 40; i++) {
			dispatcher.dispatch(message);
		}
		verify(handler, atLeast(18)).handleMessage(message);
		verify(differentHandler, atLeast(18)).handleMessage(message);
	}

}
