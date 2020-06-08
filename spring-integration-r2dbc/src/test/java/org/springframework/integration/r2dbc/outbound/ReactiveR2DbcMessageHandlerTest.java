package org.springframework.integration.r2dbc.outbound;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class ReactiveR2DbcMessageHandlerTest {

	@Autowired
	DatabaseClient client;

	@Autowired
	H2ConnectionFactory factory;

	R2dbcEntityTemplate entityTemplate;

	@Autowired
	PersonRepository personRepository;

	@Configuration
	@EnableR2dbcRepositories(considerNestedRepositories = true,
			includeFilters = @ComponentScan.Filter(classes = PersonRepository.class, type = FilterType.ASSIGNABLE_TYPE))
	static class IntegrationTestConfiguration extends AbstractR2dbcConfiguration {

		@Bean
		@Override
		public ConnectionFactory connectionFactory() {
			return createConnectionFactory();
		}
	}

	public static ConnectionFactory createConnectionFactory() {

		return new H2ConnectionFactory(H2ConnectionConfiguration.builder()
				.inMemory("r2dbc")
				.username("sa")
				.password("")
				.option("DB_CLOSE_DELAY=-1").build());
	}

	@Before
	public void setup() {
		entityTemplate = new R2dbcEntityTemplate(client);
		Hooks.onOperatorDebug();

		List<String> statements = Arrays.asList(
				"DROP TABLE IF EXISTS person;",
				"CREATE table person (id INT AUTO_INCREMENT NOT NULL, name VARCHAR2, age INT NOT NULL);");

		statements.forEach(it -> client.execute(it)
				.fetch()
				.rowsUpdated()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete());

	}

	@Test
	public void validateMessageHandlingWithDefaultCollection() {
		ReactiveR2dbcMessageHandler handler = new ReactiveR2dbcMessageHandler(this.entityTemplate);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setApplicationContext(mock(ApplicationContext.class, Answers.RETURNS_MOCKS));
		handler.afterPropertiesSet();
		Message<Person> message = MessageBuilder.withPayload(this.createPerson("Bob", 35)).build();
		waitFor(handler.handleMessage(message));

		personRepository.findAll()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();
	}

	private Person createPerson(String bob, Integer age) {
		return new Person(bob, age);
	}

	private static <T> T waitFor(Mono<T> mono) {
		return mono.block(Duration.ofSeconds(10));
	}

	interface PersonRepository extends ReactiveCrudRepository<Person, Integer> {
	}
}


