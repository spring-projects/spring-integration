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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnit44Runner;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;

/**
 * 
 * @author Iwein Fuld
 * 
 */
@RunWith(MockitoJUnit44Runner.class)
public class LoadBalancingDispatcherTests {

	private LoadBalancingDispatcher dispatcher = new LoadBalancingDispatcher();

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
}
