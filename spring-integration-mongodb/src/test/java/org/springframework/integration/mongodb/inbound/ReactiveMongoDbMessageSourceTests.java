/*
 * Copyright 2020-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bson.conversions.Bson;
import org.junit.Test;
import org.mockito.Mockito;

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
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mongodb.dsl.MongoDb;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;

import com.mongodb.BasicDBObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveMongoDbMessageSourceTests extends MongoDbAvailableTests {

	@Test
	public void withNullMongoDBFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbMessageSource((ReactiveMongoDatabaseFactory) null,
						mock(Expression.class)));
	}

	@Test
	public void withNullMongoTemplate() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbMessageSource((ReactiveMongoTemplate) null,
						mock(Expression.class)));
	}

	@Test
	public void withNullQueryExpression() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveMongoDbMessageSource(mock(ReactiveMongoDatabaseFactory.class),
						null));
	}

	@Test
	@MongoDbAvailable
	@SuppressWarnings("unchecked")
	public void validateSuccessfulQueryWithSingleElementFluxOfDbObject() {
		ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory = prepareReactiveMongoFactory();

		ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoDatabaseFactory);
		waitFor(template.save(createPerson(), "data"));

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		ReactiveMongoDbMessageSource messageSource = new ReactiveMongoDbMessageSource(reactiveMongoDatabaseFactory,
				queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();

		StepVerifier.create((Flux<BasicDBObject>) messageSource.receive().getPayload())
				.assertNext(basicDBObject -> assertThat(basicDBObject.get("name")).isEqualTo("Oleg"))
				.verifyComplete();
	}

	@Test
	@MongoDbAvailable
	@SuppressWarnings("unchecked")
	public void validateSuccessfulQueryWithSingleElementFluxOfPerson() {
		ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory = prepareReactiveMongoFactory();

		ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoDatabaseFactory);
		waitFor(template.save(createPerson(), "data"));

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		ReactiveMongoDbMessageSource messageSource = new ReactiveMongoDbMessageSource(reactiveMongoDatabaseFactory,
				queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		messageSource.setEntityClass(Person.class);

		StepVerifier.create((Flux<Person>) messageSource.receive().getPayload())
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Oleg"))
				.verifyComplete();
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfulQueryWithMultipleElements() {
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
	@MongoDbAvailable
	public void validateSuccessfulQueryWithEmptyReturn() {
		StepVerifier.create(queryMultipleElements(new LiteralExpression("{'address.state' : 'NJ'}")))
				.verifyComplete();
	}

	@Test
	@MongoDbAvailable
	@SuppressWarnings("unchecked")
	public void validateSuccessfulQueryWithCustomConverter() {
		MappingMongoConverter converter = new ReactiveTestMongoConverter(prepareReactiveMongoFactory(),
				new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);
		StepVerifier.create(queryMultipleElements(new LiteralExpression("{'address.state' : 'PA'}"),
				Optional.of(converter))).expectNextCount(3)
				.verifyComplete();

		verify(converter, times(3)).read((Class<Person>) Mockito.any(), Mockito.any(Bson.class));
	}

	@Test
	@MongoDbAvailable
	public void validateWithConfiguredPollerFlow() {
		ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory = prepareReactiveMongoFactory();
		ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoDatabaseFactory);

		waitFor(template.save(createPerson(), "data"));

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
	@MongoDbAvailable
	@SuppressWarnings("unchecked")
	public void validatePipelineInModifyOut() {
		ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory = prepareReactiveMongoFactory();
		ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoDatabaseFactory);

		waitFor(template.save(BasicDBObject.parse("{'name' : 'Manny', 'id' : 1}"), "data"));

		Expression queryExpression = new LiteralExpression("{'name' : 'Manny'}");
		ReactiveMongoDbMessageSource messageSource = new ReactiveMongoDbMessageSource(reactiveMongoDatabaseFactory,
				queryExpression);
		messageSource.setExpectSingleResult(true);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		BasicDBObject result = waitFor((Mono<BasicDBObject>) messageSource.receive().getPayload());
		Object id = result.get("_id");
		result.put("company", "PepBoys");
		waitFor(template.save(result, "data"));
		result = waitFor((Mono<BasicDBObject>) messageSource.receive().getPayload());
		assertThat(result.get("_id")).isEqualTo(id);
	}

	private Flux<Person> queryMultipleElements(Expression queryExpression) {
		return queryMultipleElements(queryExpression, Optional.empty());
	}

	@SuppressWarnings("unchecked")
	private Flux<Person> queryMultipleElements(Expression queryExpression, Optional<MappingMongoConverter> converter) {
		ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory = this.prepareReactiveMongoFactory();

		ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoDatabaseFactory);
		waitFor(template.save(createPerson("Manny"), "data"));
		waitFor(template.save(createPerson("Moe"), "data"));
		waitFor(template.save(createPerson("Jack"), "data"));

		ReactiveMongoDbMessageSource messageSource = new ReactiveMongoDbMessageSource(reactiveMongoDatabaseFactory,
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
			return IntegrationFlows
					.from(MongoDb.reactiveInboundChannelAdapter(
							REACTIVE_MONGO_DATABASE_FACTORY, "{'name' : 'Oleg'}")
									.update(Update.update("name", "DONE"))
									.entityClass(Person.class),
							c -> c.poller(Pollers.fixedDelay(100)))
					.split()
					.channel(c -> c.flux("output"))
					.get();
		}

	}

}
