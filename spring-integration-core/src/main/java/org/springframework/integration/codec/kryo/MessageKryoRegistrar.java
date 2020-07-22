/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.codec.kryo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;

/**
 * Registers common MessageHeader types and Serializers.
 *
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class MessageKryoRegistrar extends AbstractKryoRegistrar {

	private int genericMessageRegistrationId = RegistrationIds.DEFAULT_GENERIC_MESSAGE_ID;

	private int errorMessageRegistrationId = RegistrationIds.DEFAULT_ERROR_MESSAGE_ID;

	private int adviceMessageRegistrationId = RegistrationIds.DEFAULT_ADVICE_MESSAGE_ID;

	private int mutableMessageRegistrationId = RegistrationIds.DEFAULT_MUTABLE_MESSAGE_ID;

	private int messageHeadersRegistrationId = RegistrationIds.DEFAULT_MESSAGEHEADERS_ID;

	private int mutableMessageHeadersRegistrationId = RegistrationIds.DEFAULT_MUTABLE_MESSAGEHEADERS_ID;

	private int hashMapRegistrationId = RegistrationIds.DEFAULT_HASH_MAP_ID;

	private int uuidRegistrationId = RegistrationIds.DEFAULT_UUID_ID;

	/**
	 * Set the registration id for {@link MessageHeaders}.
	 * @param messageHeadersRegistrationId the id, default 41.
	 */
	public void setMessageHeadersRegistrationId(int messageHeadersRegistrationId) {
		this.messageHeadersRegistrationId = messageHeadersRegistrationId;
	}

	/**
	 * Set the registration id for {@link MutableMessageHeaders}.
	 * @param mutableMessageHeadersRegistrationId the id, default 42.
	 */
	public void setMutableMessageHeadersRegistrationId(int mutableMessageHeadersRegistrationId) {
		this.mutableMessageHeadersRegistrationId = mutableMessageHeadersRegistrationId;
	}

	/**
	 * Set the registration id for {@link GenericMessage}.
	 * @param genericMessageRegistrationId the id, default 43.
	 * @since 4.3.23
	 */
	public void setGenericMessageRegistrationId(int genericMessageRegistrationId) {
		this.genericMessageRegistrationId = genericMessageRegistrationId;
	}

	/**
	 * Set the registration id for {@link ErrorMessage}.
	 * @param errorMessageRegistrationId the id, default 44.
	 * @since 4.3.23
	 */
	public void setErrorMessageRegistrationId(int errorMessageRegistrationId) {
		this.errorMessageRegistrationId = errorMessageRegistrationId;
	}

	/**
	 * Set the registration id for {@link AdviceMessage}.
	 * @param adviceMessageRegistrationId the id, default 45.
	 * @since 4.3.23
	 */
	public void setAdviceMessageRegistrationId(int adviceMessageRegistrationId) {
		this.adviceMessageRegistrationId = adviceMessageRegistrationId;
	}

	/**
	 * Set the registration id for {@link MutableMessage}.
	 * @param mutableMessageRegistrationId the id, default 46.
	 * @since 4.3.23
	 */
	public void setMutableMessageRegistrationId(int mutableMessageRegistrationId) {
		this.mutableMessageRegistrationId = mutableMessageRegistrationId;
	}

	/**
	 * Set the registration id for {@link HashMap}.
	 * @param hashMapRegistrationId the id, default 47.
	 * @since 4.3.23
	 */
	public void setHashMapRegistrationId(int hashMapRegistrationId) {
		this.hashMapRegistrationId = hashMapRegistrationId;
	}

	/**
	 * Set the registration id for {@link UUID}.
	 * @param uuidRegistrationId the id, default 48.
	 * @since 4.3.23
	 */
	public void setUuidRegistrationId(int uuidRegistrationId) {
		this.uuidRegistrationId = uuidRegistrationId;
	}

	@Override
	public void registerTypes(Kryo kryo) {
		super.registerTypes(kryo);
		kryo.register(GenericMessage.class, this.genericMessageRegistrationId);
		kryo.register(ErrorMessage.class, this.errorMessageRegistrationId);
		kryo.register(AdviceMessage.class, this.adviceMessageRegistrationId);
		kryo.register(MutableMessage.class, this.mutableMessageRegistrationId);
		kryo.register(HashMap.class, this.hashMapRegistrationId);
		kryo.register(UUID.class, this.uuidRegistrationId);
	}

	@Override
	public List<Registration> getRegistrations() {
		return Arrays.asList(
				new Registration(MessageHeaders.class, new MessageHeadersSerializer(),
						this.messageHeadersRegistrationId),
				new Registration(MutableMessageHeaders.class, new MutableMessageHeadersSerializer(),
						this.mutableMessageHeadersRegistrationId));
	}

}
