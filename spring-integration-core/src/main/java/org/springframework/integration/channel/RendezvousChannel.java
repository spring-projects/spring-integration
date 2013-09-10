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

package org.springframework.integration.channel;

import java.util.concurrent.SynchronousQueue;

import org.springframework.messaging.Message;

/**
 * A zero-capacity version of {@link QueueChannel} that delegates to a
 * {@link SynchronousQueue} internally. This accommodates "handoff" scenarios
 * (i.e. blocking while waiting for another party to send or receive).
 * 
 * @author Mark Fisher
 */
public class RendezvousChannel extends QueueChannel {

	public RendezvousChannel() {
		super(new SynchronousQueue<Message<?>>());
	}

}
