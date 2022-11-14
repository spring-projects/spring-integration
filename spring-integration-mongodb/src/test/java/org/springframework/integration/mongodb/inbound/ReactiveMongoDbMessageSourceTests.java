/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.integration.mongodb.inbound;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.mongodb.BasicDBObject;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.mongodb.dsl.MongoDb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author David Turanski
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 5.3
 */
class ReactiveMongoDbMessageSourceTests implements MongoDbContainerTest {

	static ReactiveMongoDatabaseFactory REACTIVE_MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		REACTIVE_MONGO_DATABASE_FACTORY = MongoDbContainerTest.createReactiveMongoDbFactory();
	}

	@Test
	void withNullMongoDBFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbMessageSource((ReactiveMongoDatabaseFactory) null,
						mock(Expression.class)));
	}

	@Test
	void withNullMongoTemplate() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbMessageSource((ReactiveMongoTemplate) null,
						mock(Expression.class)));
	}

	@Test
	void withNullQueryExpression() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbMessageSource(mock(ReactiveMongoDatabaseFactory.class),
						null));
	}

	@Test
	@SuppressWarnings("unchecked")
	void validateSuccessfulQueryWithSingleElementFluxOfDbObject() {
		MongoDbContainerTest.prepareReactiveMongoData(REACTIVE_MONGO_DATABASE_FACTORY);

		ReactiveMongoTemplate template = new ReactiveMongoTemplate(REACTIVE_MONGO_DATABASE_FACTORY);
		waitFor(template.save(MongoDbContainerTest.createPerson(), "data"));

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		ReactiveMongoDbMessageSource messageSource = new ReactiveMongoDbMessageSource(REACTIVE_MONGO_DATABASE_FACTORY,
				queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();

		StepVerifier.create((Flux<BasicDBObject>) messageSource.receive().getPayload())
				.assertNext(basicDBObject -> assertThat(basicDBObject).containsEntry("name", "Oleg"))
				.verifyComplete();
	}

	@Test
	@SuppressWarnings("unchecked")
	void validateSuccessfulQueryWithSingleElementFluxOfPerson() {

		MongoDbContainerTest.prepareReactiveMongoData(REACTIVE_MONGO_DATABASE_FACTORY);

		ReactiveMongoTemplate template = new ReactiveMongoTemplate(REACTIVE_MONGO_DATABASE_FACTORY);
		waitFor(template.save(MongoDbContainerTest.createPerson(), "data"));

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		ReactiveMongoDbMessageSource messageSource = new ReactiveMongoDbMessageSource(REACTIVE_MONGO_DATABASE_FACTORY,
				queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);

		StepVerifier.create((Flux<Person>) messageSource.receive().getPayload())
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Oleg"))
				.verifyComplete();
	}

	@Test
	void validateSuccessfulQueryWithMultipleElements() {
		final List<String> names = new ArrayList<>(Arrays.asList("Manny", "Moe", "Jack"));
		StepVerifier.create(queryMultipleElements(new LiteralExpression("{'address.state' : 'PA'}")))
				.expectNextMatches(person -> {
					names.remove(person.getName());
					return names.size() == 2;
				})
				.expectNextMatches(person -> {
					names.remove(person.getName());
					return names.size() == 1;
				})
				.expectNextMatches(person -> {
					names.remove(person.getName());
					return names.size() == 0;
				})
				.verifyComplete();
	}

	@Test
	void validateSuccessfulQueryWithEmptyReturn() {
		StepVerifier.create(queryMultipleElements(new LiteralExpression("{'address.state' : 'NJ'}")))
				.verifyComplete();
	}

	@Test
	@SuppressWarnings("unchecked")
	void validateSuccessfulQueryWithCustomConverter() {
		MongoDbContainerTest.prepareReactiveMongoData(REACTIVE_MONGO_DATABASE_FACTORY);
		MappingMongoConverter converter = new ReactiveTestMongoConverter(
				REACTIVE_MONGO_DATABASE_FACTORY,
				new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		StepVerifier.create(queryMultipleElements(new LiteralExpression("{'address.state' : 'PA'}"),
						Optional.of(converter))).expectNextCount(3)
				.verifyComplete();

		verify(converter, times(3)).read((Class<Person>) Mockito.any(), Mockito.any(Bson.class));
	}

	@Test
	void validateWithConfiguredPollerFlow() {
		MongoDbContainerTest.prepareReactiveMongoData(REACTIVE_MONGO_DATABASE_FACTORY);
		ReactiveMongoTemplate template = new ReactiveMongoTemplate(REACTIVE_MONGO_DATABASE_FACTORY);

		waitFor(template.save(MongoDbContainerTest.createPerson(), "data"));

		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(TestContext.class);
		FluxMessageChannel output = context.getBean(FluxMessageChannel.class);
		StepVerifier.create(output)
				.assertNext(
						message -> assertThat(((Person) message.getPayload()).getName()).isEqualTo("Oleg"))
				.expectNoEvent(Duration.ofMillis(100))
				.thenCancel()
				.verify(Duration.ofSeconds(10));

		context.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	void validatePipelineInModifyOut() {
		MongoDbContainerTest.prepareReactiveMongoData(REACTIVE_MONGO_DATABASE_FACTORY);
		ReactiveMongoTemplate template = new ReactiveMongoTemplate(REACTIVE_MONGO_DATABASE_FACTORY);

		waitFor(template.save(BasicDBObject.parse("{'name' : 'Manny', 'id' : 1}"), "data"));

		Expression queryExpression = new LiteralExpression("{'name' : 'Manny'}");
		ReactiveMongoDbMessageSource messageSource = new ReactiveMongoDbMessageSource(REACTIVE_MONGO_DATABASE_FACTORY,
				queryExpression);
		messageSource.setExpectSingleResult(true);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		BasicDBObject result = waitFor((Mono<BasicDBObject>) messageSource.receive().getPayload());
		Object id = result.get("_id");
		result.put("company", "PepBoys");
		waitFor(template.save(result, "data"));
		result = waitFor((Mono<BasicDBObject>) messageSource.receive().getPayload());
		assertThat(result).containsEntry("_id", id);
	}

	private Flux<Person> queryMultipleElements(Expression queryExpression) {
		return queryMultipleElements(queryExpression, Optional.empty());
	}

	@SuppressWarnings("unchecked")
	private Flux<Person> queryMultipleElements(Expression queryExpression, Optional<MappingMongoConverter> converter) {
		MongoDbContainerTest.prepareReactiveMongoData(REACTIVE_MONGO_DATABASE_FACTORY);

		ReactiveMongoTemplate template = new ReactiveMongoTemplate(REACTIVE_MONGO_DATABASE_FACTORY);
		waitFor(template.save(MongoDbContainerTest.createPerson("Manny"), "data"));
		waitFor(template.save(MongoDbContainerTest.createPerson("Moe"), "data"));
		waitFor(template.save(MongoDbContainerTest.createPerson("Jack"), "data"));

		ReactiveMongoDbMessageSource messageSource = new ReactiveMongoDbMessageSource(REACTIVE_MONGO_DATABASE_FACTORY,
				queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.setEntityClass(Person.class);
		converter.ifPresent(messageSource::setMongoConverter);
		messageSource.afterPropertiesSet();

		return (Flux<Person>) messageSource.receive().getPayload();
	}

	private static <T> T waitFor(Mono<T> mono) {
		return mono.block(Duration.ofSeconds(10));
	}

	@Configuration
	@EnableIntegration
	static class TestContext {

		@Bean
		public IntegrationFlow pollingFlow() {
			return IntegrationFlow
					.from(MongoDb.reactiveInboundChannelAdapter(
											REACTIVE_MONGO_DATABASE_FACTORY, "{'name' : 'Oleg'}")
									.update(Update.update("name", "DONE"))
									.entityClass(Person.class),
							c -> c.poller(pollOnceAfter100ms()))
					.split()
					.channel(c -> c.flux("output"))
					.get();
		}

		@NotNull
		private PollerSpec pollOnceAfter100ms() {
			return Pollers.fixedDelay(Duration.ofMinutes(5), Duration.ofMillis(100));
		}

	}

}
