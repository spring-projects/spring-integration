/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.r2dbc.config;


import static org.mockito.Mockito.mock;

import org.mockito.Answers;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.integration.r2dbc.entity.Person;
import org.springframework.integration.r2dbc.inbound.R2dbcMessageSource;
import org.springframework.integration.r2dbc.outbound.R2dbcMessageHandler;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;

/**
 *  @author Rohan Mukesh
 *
 *  @since 5.4
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "org.springframework.integration.r2dbc.repository")
public class R2dbcIntegrationTestConfiguration extends AbstractR2dbcConfiguration {

	@Bean
	@Override
	public ConnectionFactory connectionFactory() {
		return createConnectionFactory();
	}

	public static ConnectionFactory createConnectionFactory() {

		return new H2ConnectionFactory(H2ConnectionConfiguration.builder()
				.inMemory("r2dbc")
				.username("sa")
				.password("")
				.option("DB_CLOSE_DELAY=-1").build());
	}

	@Bean
	public DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
		return DatabaseClient.create(connectionFactory);
	}

	@Bean
	public R2dbcMessageHandler r2dbcMessageHandler(DatabaseClient databaseClient) {
		R2dbcMessageHandler r2dbcMessageHandler = new R2dbcMessageHandler(new R2dbcEntityTemplate(databaseClient));
		r2dbcMessageHandler.setBeanFactory(mock(BeanFactory.class));
		r2dbcMessageHandler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		r2dbcMessageHandler.afterPropertiesSet();
		return r2dbcMessageHandler;
	}

	@Bean
	public R2dbcMessageSource r2dbcMessageSource(DatabaseClient databaseClient) {
		R2dbcMessageSource r2dbcMessageSource = new R2dbcMessageSource(databaseClient, "");
		r2dbcMessageSource.setBeanFactory(mock(BeanFactory.class));
		r2dbcMessageSource.afterPropertiesSet();
		r2dbcMessageSource.setPayloadType(Person.class);
		return r2dbcMessageSource;
	}

}
