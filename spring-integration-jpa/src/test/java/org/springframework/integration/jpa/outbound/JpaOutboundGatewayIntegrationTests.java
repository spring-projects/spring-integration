/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jpa.outbound;

import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * The test cases for testing out a complete flow of the JPA gateways/adapters with all
 * the components integrated.
 *
 * @author Amol Nayak
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class JpaOutboundGatewayIntegrationTests {

	@Autowired
	@Qualifier("in")
	private SubscribableChannel requestChannel;

	@Autowired
	@Qualifier("out")
	private SubscribableChannel responseChannel;

	@Autowired
	@Qualifier("findByEntityClass")
	private SubscribableChannel findByEntityClassChannel;

	@Autowired
	@Qualifier("findByPayloadType")
	private SubscribableChannel findByPayloadTypeChannel;

	@Autowired
	@Qualifier("findAndDelete")
	private SubscribableChannel findAndDeleteChannel;

	@Autowired
	@Qualifier("findResultChannel")
	private PollableChannel findResultChannel;

	@Autowired
	private EntityManager entityManager;

	private volatile MessageHandler handler;

	@AfterEach
	public void tearDown() {
		if (this.handler != null) {
			responseChannel.unsubscribe(this.handler);
		}
	}

	/**
	 * Sends a message with the payload as a integer representing the start number in the result
	 * set and a header with value maxResults to get the max number of results
	 */
	@Test
	public void retrieveFromSecondRecordAndMaximumOneRecord() {
		this.handler = message -> {
			assertThat(((List<?>) message.getPayload()).size()).isEqualTo(2);
			assertThat(entityManager.createQuery("from Student").getResultList().size()).isEqualTo(1);
			assertThat(message.getHeaders()).containsEntry("maxResults", "10");
		};
		this.responseChannel.subscribe(this.handler);

		Message<Integer> message =
				MessageBuilder.withPayload(1)
						.setHeader("maxResults", "10")
						.build();
		this.requestChannel.send(message);
	}

	@Test
	public void testFindByEntityClass() {
		this.handler = message -> {
			assertThat(message.getPayload()).isInstanceOf(StudentDomain.class);
			StudentDomain student = (StudentDomain) message.getPayload();
			assertThat(student.getFirstName()).isEqualTo("First One");
		};
		this.responseChannel.subscribe(this.handler);

		Message<Long> message = MessageBuilder.withPayload(1001L).build();
		this.findByEntityClassChannel.send(message);
	}

	@Test
	public void testFindByPayloadType() {
		this.handler = message -> {
			assertThat(message.getPayload()).isInstanceOf(StudentDomain.class);
			StudentDomain student = (StudentDomain) message.getPayload();
			assertThat(student.getFirstName()).isEqualTo("First Two");
		};
		this.responseChannel.subscribe(this.handler);

		StudentDomain payload = new StudentDomain();
		payload.setRollNumber(1002L);
		Message<StudentDomain> message = MessageBuilder.withPayload(payload).build();
		this.findByPayloadTypeChannel.send(message);
	}

	@Test
	public void testFindAndDelete() {
		Message<Long> message = MessageBuilder.withPayload(1001L).build();
		this.findAndDeleteChannel.send(message);

		Message<?> receive = this.findResultChannel.receive(2000);
		assertThat(receive).isNotNull();

		assertThatExceptionOfType(ReplyRequiredException.class)
				.isThrownBy(() -> this.findAndDeleteChannel.send(message));
	}

}
