/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.mongodb.store;

import java.io.Serializable;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.integration.mongodb.support.BinaryToMessageConverter;
import org.springframework.integration.mongodb.support.MessageToBinaryConverter;
import org.springframework.integration.store.MessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Amol Nayak
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class ConfigurableMongoDbMessageStoreTests extends AbstractMongoDbMessageStoreTests {

	private static final String[] MESSAGE_PATTERNS = {
			"org.springframework.messaging.support.GenericMessage",
			"org.springframework.messaging.MessageHeaders",
			"java.util.UUID",
			"java.util.HashMap",
			TrustedPayload.class.getName()
	};

	static final String[] CONFIGURABLE_STORE_PATTERNS = {
			"org.springframework.messaging.support.GenericMessage",
			"org.springframework.messaging.MessageHeaders",
			"java.util.UUID",
			"java.util.Collections$UnmodifiableList",
			"java.util.Collections$UnmodifiableCollection",
			"java.util.ArrayList",
			"java.util.Arrays$ArrayList",
			"org.springframework.integration.history.MessageHistory",
			"org.springframework.integration.history.MessageHistory$Entry",
			"java.util.Properties",
			"java.util.Hashtable",
			"org.springframework.integration.message.AdviceMessage",
			"org.springframework.integration.mongodb.store.AbstractMongoDbMessageStoreTests$*",
			"org.springframework.integration.mongodb.store.MongoDbMessageStoreClaimCheckIntegrationTests$Beverage",
			"org.springframework.integration.support.MutableMessage",
			"org.springframework.integration.support.MutableMessageHeaders",
			"org.springframework.messaging.support.ErrorMessage",
			"org.springframework.messaging.MessagingException",
			"org.springframework.core.NestedRuntimeException",
			"java.lang.RuntimeException",
			"java.lang.Exception",
			"java.lang.Throwable",
			"java.lang.StackTraceElement",
			"java.util.Collections$EmptyList",
			"java.util.HashMap"
	};

	@Override
	protected MessageStore getMessageStore() {
		ConfigurableMongoDbMessageStore mongoDbMessageStore =
				new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY,
						ConfigurableMongoDbMessageStore.DEFAULT_COLLECTION_NAME, CONFIGURABLE_STORE_PATTERNS);
		mongoDbMessageStore.setApplicationContext(this.testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

	@Test
	void allowedPatternsWithCustomMappingMongoConverter() {
		MappingMongoConverter mappingMongoConverter =
				new MappingMongoConverter(new DefaultDbRefResolver(MONGO_DATABASE_FACTORY), new MongoMappingContext());
		mappingMongoConverter.setApplicationContext(this.testApplicationContext);
		BinaryToMessageConverter binaryToMessageConverter =
				new BinaryToMessageConverter(
						"org.springframework.messaging.support.GenericMessage",
						"org.springframework.messaging.MessageHeaders",
						"java.util.UUID",
						"java.util.HashMap",
						TrustedPayload.class.getName());
		mappingMongoConverter.setCustomConversions(
				new MongoCustomConversions(List.of(new MessageToBinaryConverter(), binaryToMessageConverter)));
		mappingMongoConverter.afterPropertiesSet();
		ConfigurableMongoDbMessageStore store =
				new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY, mappingMongoConverter);
		store.setApplicationContext(this.testApplicationContext);
		store.afterPropertiesSet();

		Message<?> trusted = store.addMessage(new GenericMessage<>(new TrustedPayload()));
		assertThat(store.getMessage(trusted.getHeaders().getId()))
				.extracting(Message::getPayload)
				.isInstanceOf(TrustedPayload.class);

		Message<?> untrusted = store.addMessage(new GenericMessage<>(new UntrustedPayload()));
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> store.getMessage(untrusted.getHeaders().getId()))
				.havingCause()
				.isInstanceOf(SerializationFailedException.class)
				.havingCause()
				.isInstanceOf(SecurityException.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyFactoryConstructorIsUnrestricted() {
		ConfigurableMongoDbMessageStore store = new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY);
		initialize(store);
		Message<?> untrusted = store.addMessage(new GenericMessage<>(new UntrustedPayload()));
		assertThat(store.getMessage(untrusted.getHeaders().getId()))
				.extracting(Message::getPayload)
				.isInstanceOf(UntrustedPayload.class);
	}

	@Test
	void patternsConstructorEnforcesAllowList() {
		ConfigurableMongoDbMessageStore store =
				new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY,
						ConfigurableMongoDbMessageStore.DEFAULT_COLLECTION_NAME, MESSAGE_PATTERNS);
		initialize(store);

		Message<?> trusted = store.addMessage(new GenericMessage<>(new TrustedPayload()));
		assertThat(store.getMessage(trusted.getHeaders().getId()))
				.extracting(Message::getPayload)
				.isInstanceOf(TrustedPayload.class);

		Message<?> untrusted = store.addMessage(new GenericMessage<>(new UntrustedPayload()));
		assertUnauthorized(() -> store.getMessage(untrusted.getHeaders().getId()));
	}

	@Test
	void channelMessageStorePatternsConstructorEnforcesAllowList() {
		MongoDbChannelMessageStore store =
				new MongoDbChannelMessageStore(MONGO_DATABASE_FACTORY,
						MongoDbChannelMessageStore.DEFAULT_COLLECTION_NAME, MESSAGE_PATTERNS);
		initialize(store);
		store.removeMessageGroup("allowListGroup");

		try {
			store.addMessageToGroup("allowListGroup", new GenericMessage<>(new TrustedPayload()));
			assertThat(store.pollMessageFromGroup("allowListGroup"))
					.extracting(Message::getPayload)
					.isInstanceOf(TrustedPayload.class);

			store.addMessageToGroup("allowListGroup", new GenericMessage<>(new UntrustedPayload()));
			assertUnauthorized(() -> store.pollMessageFromGroup("allowListGroup"));
		}
		finally {
			store.removeMessageGroup("allowListGroup");
		}
	}

	@Test
	void patternsConstructorsRejectInvalidPatterns() {
		String collection = ConfigurableMongoDbMessageStore.DEFAULT_COLLECTION_NAME;
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY, collection,
						(String[]) null))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY, collection,
						new String[0]))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbChannelMessageStore(MONGO_DATABASE_FACTORY, collection,
						"java.util.*", " "))
				.withMessageContaining("whitespace-only");
	}

	private void initialize(AbstractConfigurableMongoDbMessageStore store) {
		store.setApplicationContext(this.testApplicationContext);
		store.afterPropertiesSet();
	}

	private static void assertUnauthorized(ThrowingCallable callable) {
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(callable)
				.havingCause()
				.isInstanceOf(SerializationFailedException.class)
				.havingCause()
				.isInstanceOf(SecurityException.class);
	}

	@SuppressWarnings("serial")
	private static final class TrustedPayload implements Serializable {

	}

	@SuppressWarnings("serial")
	private static final class UntrustedPayload implements Serializable {

	}

}
