/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.jpa.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.jpa.test.entity.Gender;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class JpaTests {

	private static EmbeddedDatabase dataSource;

	@Autowired
	private PollableChannel pollingResults;

	@Autowired
	@Qualifier("outboundAdapterFlow.input")
	private MessageChannel outboundAdapterFlowInput;

	@Autowired
	@Qualifier("updatingGatewayFlow.input")
	private MessageChannel updatingGatewayFlowInput;

	@Autowired
	private PollableChannel persistResults;

	@Autowired
	@Qualifier("retrievingGatewayFlow.input")
	private MessageChannel retrievingGatewayFlowInput;


	@Autowired
	private PollableChannel retrieveResults;

	@BeforeClass
	public static void init() {
		dataSource = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:H2-DropTables.sql")
				.addScript("classpath:H2-CreateTables.sql")
				.addScript("classpath:H2-PopulateData.sql")
				.ignoreFailedDrops(true)
				.build();
	}

	@AfterClass
	public static void destroy() {
		dataSource.shutdown();
	}

	@Test
	public void testInboundAdapterFlow() {
		Message<?> message = this.pollingResults.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(StudentDomain.class);
		StudentDomain student = (StudentDomain) message.getPayload();
		assertThat(student.getFirstName()).isEqualTo("First One");
	}

	@Test
	public void testOutboundAdapterFlow() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		List<?> results1 = jdbcTemplate.queryForList("Select * from Student");
		assertThat(results1).isNotNull();
		assertThat(results1.size() == 3).isTrue();

		Calendar dateOfBirth = Calendar.getInstance();
		dateOfBirth.set(1981, 9, 27);

		StudentDomain student = new StudentDomain()
				.withFirstName("Artem")
				.withLastName("Bilan")
				.withGender(Gender.MALE)
				.withDateOfBirth(dateOfBirth.getTime())
				.withLastUpdated(new Date());

		assertThat(student.getRollNumber()).isNull();

		this.outboundAdapterFlowInput.send(MessageBuilder.withPayload(student).build());

		List<?> results2 = jdbcTemplate.queryForList("Select * from Student");
		assertThat(results2).isNotNull();
		assertThat(results2.size() == 4).isTrue();

		assertThat(student.getRollNumber()).isNotNull();
	}

	@Test
	public void testUpdatingGatewayFlow() {
		Calendar dateOfBirth = Calendar.getInstance();
		dateOfBirth.set(1981, 9, 27);

		StudentDomain student = new StudentDomain()
				.withFirstName("Artem")
				.withLastName("Bilan")
				.withGender(Gender.MALE)
				.withDateOfBirth(dateOfBirth.getTime())
				.withLastUpdated(new Date());

		assertThat(student.getRollNumber()).isNull();

		this.updatingGatewayFlowInput.send(MessageBuilder.withPayload(student).build());

		Message<?> receive = this.persistResults.receive(10_000);
		assertThat(receive).isNotNull();

		StudentDomain mergedStudent = (StudentDomain) receive.getPayload();
		assertThat(mergedStudent.getFirstName()).isEqualTo(student.getFirstName());
		assertThat(mergedStudent.getRollNumber()).isNotNull();
		assertThat(student.getRollNumber()).isNull();
	}

	@Test
	public void testRetrievingGatewayFlow() {
		this.retrievingGatewayFlowInput.send(MessageBuilder.withPayload("foo").setHeader("payloadId", 1002L).build());
		Message<?> receive = this.retrieveResults.receive(10_000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(StudentDomain.class);
		StudentDomain student = (StudentDomain) receive.getPayload();
		assertThat(student.getFirstName()).isEqualTo("First Two");
		assertThat(student.getGender()).isEqualTo(Gender.FEMALE);
	}


	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public JpaVendorAdapter jpaVendorAdapter() {
			AbstractJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
			adapter.setShowSql(true);
			adapter.setDatabase(Database.H2);
			adapter.setGenerateDdl(true);
			return adapter;
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean =
					new LocalContainerEntityManagerFactoryBean();
			entityManagerFactoryBean.setDataSource(dataSource);
			entityManagerFactoryBean.setPersistenceUnitName("persistenceUnit");
			entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter());
			return entityManagerFactoryBean;
		}

		@Bean
		public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
			JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
			jpaTransactionManager.setDataSource(dataSource);
			return jpaTransactionManager;
		}

		@Bean
		public IntegrationFlow pollingAdapterFlow(EntityManagerFactory entityManagerFactory) {
			return IntegrationFlows
					.from(Jpa.inboundAdapter(entityManagerFactory)
									.entityClass(StudentDomain.class)
									.maxResults(1)
									.expectSingleResult(true),
							e -> e.poller(p -> p.trigger(new OnlyOnceTrigger())))
					.channel(c -> c.queue("pollingResults"))
					.get();
		}

		@Bean
		public IntegrationFlow outboundAdapterFlow(EntityManagerFactory entityManagerFactory) {
			return f -> f
					.handle(Jpa.outboundAdapter(entityManagerFactory)
									.entityClass(StudentDomain.class)
									.persistMode(PersistMode.PERSIST),
							e -> e.handleMessageAdvice(transactionInterceptor()));
		}

		@Bean
		public TransactionInterceptor transactionInterceptor() {
			return new TransactionInterceptorBuilder().build();
		}

		@Bean
		public IntegrationFlow updatingGatewayFlow(EntityManagerFactory entityManagerFactory) {
			return f -> f
					.handle(Jpa.updatingGateway(entityManagerFactory),
							e -> e.transactional(true))
					.channel(c -> c.queue("persistResults"));
		}

		@Bean
		public IntegrationFlow retrievingGatewayFlow(EntityManagerFactory entityManagerFactory) {
			return f -> f
					.handle(Jpa.retrievingGateway(entityManagerFactory)
							.jpaQuery("from Student s where s.id = :id")
							.expectSingleResult(true)
							.parameterExpression("id", "headers[payloadId]"))
					.channel(c -> c.queue("retrieveResults"));
		}

	}

}
