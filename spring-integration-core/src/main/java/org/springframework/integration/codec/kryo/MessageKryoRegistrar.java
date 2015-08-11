/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.codec.kryo;

import java.util.Arrays;
import java.util.List;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.MessageHeaders;

import com.esotericsoftware.kryo.Registration;

/**
 * Registers common MessageHeader types and Serializers.
 * @author David Turanski
 * @since 4.2
 */
public class MessageKryoRegistrar extends AbstractKryoRegistrar {

	private static int MESSAGEHEADERS_ID = 41;

	private static int MUTABLE_MESSAGEHEADERS_ID = 42;

	@Override
	public List<Registration> getRegistrations() {
		return Arrays.asList(new Registration[] {
				new Registration(MessageHeaders.class, new MessageHeadersSerializer(), MESSAGEHEADERS_ID),
				new Registration(MutableMessageHeaders.class, new MutableMessageHeadersSerializer(),
						MUTABLE_MESSAGEHEADERS_ID)
		});

	}

}
