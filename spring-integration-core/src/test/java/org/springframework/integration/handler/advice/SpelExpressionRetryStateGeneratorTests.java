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

package org.springframework.integration.handler.advice;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.classify.ClassifierSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.retry.RetryState;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class SpelExpressionRetryStateGeneratorTests {

	@Autowired
	private RetryStateGenerator configGenerator;

	private Message<String> message =
			MessageBuilder.withPayload("Hello, world!")
					.setHeader("foo", "bar")
					.setHeader("trueHeader", true)
					.setHeader("falseHeader", false)
					.build();

	@Test
	public void testBasic() {
		SpelExpressionRetryStateGenerator generator =
				new SpelExpressionRetryStateGenerator("headers['foo']");
		RetryState state = generator.determineRetryState(message);
		assertThat(state.getKey()).isEqualTo("bar");
		assertThat(state.isForceRefresh()).isFalse();
		assertThat(state.rollbackFor(new RuntimeException())).isTrue();
	}

	@Test
	public void testBasicConfig() {
		RetryState state = configGenerator.determineRetryState(message);
		assertThat(state.getKey()).isEqualTo("bar");
		assertThat(state.isForceRefresh()).isFalse();
		assertThat(state.rollbackFor(new RuntimeException())).isTrue();
	}

	@Test
	public void testForceRefreshTrue() {
		SpelExpressionRetryStateGenerator generator =
				new SpelExpressionRetryStateGenerator("headers['foo']", "headers['trueHeader']");
		RetryState state = generator.determineRetryState(message);
		assertThat(state.getKey()).isEqualTo("bar");
		assertThat(state.isForceRefresh()).isTrue();
		assertThat(state.rollbackFor(new RuntimeException())).isTrue();
	}

	@Test
	public void testForceRefreshFalse() {
		SpelExpressionRetryStateGenerator generator =
				new SpelExpressionRetryStateGenerator("headers['foo']", "headers['falseHeader']");
		RetryState state = generator.determineRetryState(message);
		assertThat(state.getKey()).isEqualTo("bar");
		assertThat(state.isForceRefresh()).isFalse();
		assertThat(state.rollbackFor(new RuntimeException())).isTrue();
	}

	@Test
	public void testForceRefreshElvis() {
		SpelExpressionRetryStateGenerator generator =
				new SpelExpressionRetryStateGenerator("headers['foo']", "headers['noHeader']?:true");
		RetryState state = generator.determineRetryState(message);
		assertThat(state.getKey()).isEqualTo("bar");
		assertThat(state.isForceRefresh()).isTrue();
		assertThat(state.rollbackFor(new RuntimeException())).isTrue();
	}

	@Test
	public void testClassifier() {
		SpelExpressionRetryStateGenerator generator =
				new SpelExpressionRetryStateGenerator("headers['foo']");
		generator.setClassifier(new ClassifierSupport<>(false));
		RetryState state = generator.determineRetryState(message);
		assertThat(state.getKey()).isEqualTo("bar");
		assertThat(state.isForceRefresh()).isFalse();
		assertThat(state.rollbackFor(new RuntimeException())).isFalse();
	}

}
