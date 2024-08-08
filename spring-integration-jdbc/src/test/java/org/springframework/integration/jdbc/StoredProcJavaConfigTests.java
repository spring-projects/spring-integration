/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.jdbc;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ControlBusFactoryBean;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.jdbc.storedproc.PrimeMapper;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equivalent to {@link StoredProcPollingChannelAdapterWithNamespaceIntegrationTests}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class StoredProcJavaConfigTests {

	@Autowired
	private PollableChannel fooChannel;

	@Autowired
	private MessageChannel control;

	@Test
	public void test() {
		Message<?> received = fooChannel.receive(10000);
		assertThat(received).isNotNull();
		@SuppressWarnings("unchecked")
		Collection<Integer> primes = (Collection<Integer>) received.getPayload();
		assertThat(primes).containsExactly(2, 3, 5, 7);
		received = fooChannel.receive(100);
		// verify maxMessagesPerPoll == 1
		assertThat(received).isNull();
		MessagingTemplate template = new MessagingTemplate(this.control);
		template.convertAndSend("'storedProc.inboundChannelAdapter'.stop");
		assertThat(template.convertSendAndReceive(
				"'storedProc.inboundChannelAdapter'.isRunning", Boolean.class))
				.isFalse();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public PollableChannel fooChannel() {
			return new QueueChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "control")
		public ControlBusFactoryBean controlBus() {
			return new ControlBusFactoryBean();
		}

		@Bean
		@InboundChannelAdapter(value = "fooChannel", poller = @Poller(fixedDelay = "5000"))
		public MessageSource<?> storedProc() {
			StoredProcPollingChannelAdapter source = new StoredProcPollingChannelAdapter(storedProcExecutor());
			source.setExpectSingleResult(true);
			return source;
		}

		@Bean
		public StoredProcExecutor storedProcExecutor() {
			StoredProcExecutor executor = new StoredProcExecutor(dataSource());
			executor.setIgnoreColumnMetaData(true);
			executor.setStoredProcedureName("GET_PRIME_NUMBERS");
			List<ProcedureParameter> procedureParameters = new ArrayList<>();
			procedureParameters.add(new ProcedureParameter("beginRange", 1, null));
			procedureParameters.add(new ProcedureParameter("endRange", 10, null));
			executor.setProcedureParameters(procedureParameters);
			List<SqlParameter> sqlParameters = new ArrayList<>();
			sqlParameters.add(new SqlParameter("beginRange", Types.INTEGER));
			sqlParameters.add(new SqlParameter("endRange", Types.INTEGER));
			executor.setSqlParameters(sqlParameters);
			executor.setReturningResultSetRowMappers(Collections.singletonMap("out", new PrimeMapper()));
			return executor;
		}

		@Bean(destroyMethod = "shutdown")
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2)
					.addScript("classpath:h2-stored-procedures.sql")
					.build();
		}

	}

}
