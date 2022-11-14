/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 */
class MongoDbMessageStoreClaimCheckIntegrationTests implements MongoDbContainerTest {

	static MongoDatabaseFactory MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		MONGO_DATABASE_FACTORY = MongoDbContainerTest.createMongoDbFactory();
	}

	private final GenericApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();

	@BeforeEach
	public void setup() {
		this.testApplicationContext.refresh();
	}

	@AfterEach
	public void tearDown() {
		this.testApplicationContext.close();
	}

	@Test
	void stringPayload() {
		MongoDbMessageStore messageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		messageStore.afterPropertiesSet();
		ClaimCheckInTransformer checkin = new ClaimCheckInTransformer(messageStore);
		ClaimCheckOutTransformer checkout = new ClaimCheckOutTransformer(messageStore);
		Message<?> originalMessage = MessageBuilder.withPayload("test1").build();
		Message<?> claimCheckMessage = checkin.transform(originalMessage);
		assertThat(claimCheckMessage.getPayload()).isEqualTo(originalMessage.getHeaders().getId());
		Message<?> checkedOutMessage = checkout.transform(claimCheckMessage);
		assertThat(checkedOutMessage.getHeaders().getId()).isEqualTo(claimCheckMessage.getPayload());
		assertThat(checkedOutMessage.getPayload()).isEqualTo(originalMessage.getPayload());
		assertThat(checkedOutMessage).isEqualTo(originalMessage);
	}

	@Test
	void objectPayload() {
		MongoDbMessageStore messageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		messageStore.afterPropertiesSet();
		ClaimCheckInTransformer checkin = new ClaimCheckInTransformer(messageStore);
		ClaimCheckOutTransformer checkout = new ClaimCheckOutTransformer(messageStore);
		Beverage payload = new Beverage();
		payload.setName("latte");
		payload.setShots(3);
		payload.setIced(false);
		Message<?> originalMessage = MessageBuilder.withPayload(payload).build();
		Message<?> claimCheckMessage = checkin.transform(originalMessage);
		assertThat(claimCheckMessage.getPayload()).isEqualTo(originalMessage.getHeaders().getId());
		Message<?> checkedOutMessage = checkout.transform(claimCheckMessage);
		assertThat(checkedOutMessage.getPayload()).isEqualTo(originalMessage.getPayload());
		assertThat(checkedOutMessage.getHeaders().getId()).isEqualTo(claimCheckMessage.getPayload());
		assertThat(checkedOutMessage).isEqualTo(originalMessage);
	}

	@Test
	void stringPayloadConfigurable() {
		ConfigurableMongoDbMessageStore messageStore = new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY);
		messageStore.setApplicationContext(this.testApplicationContext);
		messageStore.afterPropertiesSet();
		ClaimCheckInTransformer checkin = new ClaimCheckInTransformer(messageStore);
		ClaimCheckOutTransformer checkout = new ClaimCheckOutTransformer(messageStore);
		Message<?> originalMessage = MessageBuilder.withPayload("test1").build();
		Message<?> claimCheckMessage = checkin.transform(originalMessage);
		assertThat(claimCheckMessage.getPayload()).isEqualTo(originalMessage.getHeaders().getId());
		Message<?> checkedOutMessage = checkout.transform(claimCheckMessage);
		assertThat(checkedOutMessage.getHeaders().getId()).isEqualTo(claimCheckMessage.getPayload());
		assertThat(checkedOutMessage.getPayload()).isEqualTo(originalMessage.getPayload());
		assertThat(checkedOutMessage).isEqualTo(originalMessage);
	}

	@Test
	void objectPayloadConfigurable() {
		ConfigurableMongoDbMessageStore messageStore = new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY);
		messageStore.setApplicationContext(this.testApplicationContext);
		messageStore.afterPropertiesSet();
		ClaimCheckInTransformer checkin = new ClaimCheckInTransformer(messageStore);
		ClaimCheckOutTransformer checkout = new ClaimCheckOutTransformer(messageStore);
		Beverage payload = new Beverage();
		payload.setName("latte");
		payload.setShots(3);
		payload.setIced(false);
		Message<?> originalMessage = MessageBuilder.withPayload(payload).build();
		Message<?> claimCheckMessage = checkin.transform(originalMessage);
		assertThat(claimCheckMessage.getPayload()).isEqualTo(originalMessage.getHeaders().getId());
		Message<?> checkedOutMessage = checkout.transform(claimCheckMessage);
		assertThat(checkedOutMessage.getPayload()).isEqualTo(originalMessage.getPayload());
		assertThat(checkedOutMessage.getHeaders().getId()).isEqualTo(claimCheckMessage.getPayload());
		assertThat(checkedOutMessage).isEqualTo(originalMessage);
	}

	@SuppressWarnings("serial")
	static class Beverage implements Serializable {

		private String name;

		private int shots;

		private boolean iced;

		@SuppressWarnings("unused")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public int getShots() {
			return shots;
		}

		public void setShots(int shots) {
			this.shots = shots;
		}

		@SuppressWarnings("unused")
		public boolean isIced() {
			return iced;
		}

		public void setIced(boolean iced) {
			this.iced = iced;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (iced ? 1231 : 1237);
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + shots;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Beverage other = (Beverage) obj;
			if (iced != other.iced) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			}
			else if (!name.equals(other.name)) {
				return false;
			}
			return shots == other.shots;
		}

	}

}
