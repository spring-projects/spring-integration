/* Copyright 2002-2008 the original author or authors.
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

import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnit44Runner;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;

/**
 * 
 * @author Iwein Fuld
 * 
 */
@RunWith(MockitoJUnit44Runner.class)
public class RoundRobinDispatcherTests {

	private AbstractUnicastDispatcher dispatcher = new RoundRobinDispatcher();

	@Mock
	private MessageHandler handler;

	@Mock
	private Message<?> message;

	@Mock
	private MessageHandler differentHandler;

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
	public void overFlowCurrentHandlerIndex() throws Exception {
		dispatcher.addHandler(handler);
		dispatcher.addHandler(differentHandler);
		DirectFieldAccessor accessor = new DirectFieldAccessor(dispatcher);
		((AtomicInteger)accessor.getPropertyValue("currentHandlerIndex")).set(Integer.MAX_VALUE-5);
		for(long i=0; i < 40; i++){
			dispatcher.dispatch(message);
		}
		verify(handler, atLeast(18)).handleMessage(message);
		verify(differentHandler, atLeast(18)).handleMessage(message);		
	}
}
