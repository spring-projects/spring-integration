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

package org.springframework.integration.bus;


/**
 * No-op implementation of a {@link MessageBusInterceptor}. Subclasses shall
 * override only the methods for which they intend to provide behaviour.
 * 
 * @author Marius Bogoevici
 */
public class MessageBusInterceptorAdapter implements MessageBusInterceptor {

	public void preStart(MessageBus bus) {
	}
	
	public void postStart(MessageBus bus) {
	}
	
	public void preStop(MessageBus bus) {
	}
	
	public void postStop(MessageBus bus) {
	}
	
}
