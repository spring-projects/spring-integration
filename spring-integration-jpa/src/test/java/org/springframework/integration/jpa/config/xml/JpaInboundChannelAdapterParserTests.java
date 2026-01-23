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

package org.springframework.integration.jpa.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class JpaInboundChannelAdapterParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private SourcePollingChannelAdapter jpaInboundChannelAdapter1;

	@Autowired
	private SourcePollingChannelAdapter jpaInboundChannelAdapter2;

	@Autowired
	private SourcePollingChannelAdapter jpaInboundChannelAdapter3;

	@Test
	public void testJpaInboundChannelAdapterParser() {
		AbstractMessageChannel outputChannel =
				TestUtils.getPropertyValue(this.jpaInboundChannelAdapter1, "outputChannel");

		assertThat(outputChannel.getComponentName()).isEqualTo("out");

		JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.jpaInboundChannelAdapter1, "source.jpaExecutor");

		assertThat(jpaExecutor).isNotNull();

		Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass");

		assertThat(entityClass.getName()).isEqualTo("org.springframework.integration.jpa.test.entity.StudentDomain");

		JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations");

		assertThat(jpaOperations).isNotNull();

		assertThat(TestUtils.<Boolean>getPropertyValue(jpaExecutor, "expectSingleResult")).isTrue();
		ParameterSource parameterSource = this.context.getBean(ParameterSource.class);
		assertThat(TestUtils.<Object>getPropertyValue(jpaExecutor, "parameterSource")).isSameAs(parameterSource);
	}

	@Test
	public void testJpaInboundChannelAdapterParserWithMaxResults() {
		AbstractMessageChannel outputChannel =
				TestUtils.getPropertyValue(this.jpaInboundChannelAdapter2, "outputChannel");

		assertThat(outputChannel.getComponentName()).isEqualTo("out");

		JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.jpaInboundChannelAdapter2, "source.jpaExecutor");

		assertThat(jpaExecutor).isNotNull();

		Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass");

		assertThat(entityClass.getName()).isEqualTo("org.springframework.integration.jpa.test.entity.StudentDomain");

		JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations");

		assertThat(jpaOperations).isNotNull();

		LiteralExpression expression = TestUtils.getPropertyValue(jpaExecutor, "maxResultsExpression");

		assertThat(expression).isNotNull();

		assertThat(TestUtils.<String>getPropertyValue(expression, "literalValue")).isEqualTo("13");

		assertThat(TestUtils.<Boolean>getPropertyValue(jpaExecutor, "deleteAfterPoll")).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(jpaExecutor, "flush")).isTrue();
	}

	@Test
	public void testJpaInboundChannelAdapterParserWithMaxResultsExpression() {
		AbstractMessageChannel outputChannel =
				TestUtils.<AbstractMessageChannel>getPropertyValue(this.jpaInboundChannelAdapter3, "outputChannel");

		assertThat(outputChannel.getComponentName()).isEqualTo("out");

		JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.jpaInboundChannelAdapter3, "source.jpaExecutor");

		assertThat(jpaExecutor).isNotNull();

		Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass");

		assertThat(entityClass.getName()).isEqualTo("org.springframework.integration.jpa.test.entity.StudentDomain");

		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations");

		assertThat(jpaOperations).isNotNull();

		SpelExpression expression = TestUtils.getPropertyValue(jpaExecutor, "maxResultsExpression");

		assertThat(expression).isNotNull();

		assertThat(TestUtils.<String>getPropertyValue(expression, "expression")).isEqualTo("@maxNumberOfResults");
	}

	@Test
	public void testJpaExecutorBeanIdNaming() {
		JpaExecutor jpaExecutor1 = this.context.getBean("jpaInboundChannelAdapter1.jpaExecutor", JpaExecutor.class);
		JpaExecutor jpaExecutor2 = this.context.getBean("jpaInboundChannelAdapter2.jpaExecutor", JpaExecutor.class);

		assertThat(jpaExecutor1).isNotNull();
		assertThat(jpaExecutor2).isNotNull();
		assertThat(jpaExecutor2).isNotSameAs(jpaExecutor1);

		assertThat(this.context.getBeansOfType(JpaExecutor.class).size()).isEqualTo(5);

		JpaExecutor jpaExecutorWithoutId0 =
				this.context.getBean("org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean#0.jpaExecutor",
						JpaExecutor.class);
		JpaExecutor jpaExecutorWithoutId1 =
				this.context.getBean("org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean#1.jpaExecutor",
						JpaExecutor.class);

		assertThat(jpaExecutorWithoutId0).isNotNull();
		assertThat(jpaExecutorWithoutId1).isNotNull();
		assertThat(jpaExecutorWithoutId1).isNotSameAs(jpaExecutorWithoutId0);
	}

}
