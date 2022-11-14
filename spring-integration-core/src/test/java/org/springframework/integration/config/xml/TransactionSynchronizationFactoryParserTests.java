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

package org.springframework.integration.config.xml;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.TransactionSynchronizationProcessor;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class TransactionSynchronizationFactoryParserTests {

	@Test // nothing to assert. Validates only XSD
	public void validateXsdCombinationOfOrderOfSubelements() {
		new ClassPathXmlApplicationContext("TransactionSynchronizationFactoryParserTests-xsd.xml", this.getClass())
				.close();
	}

	@Test
	public void validateFullConfiguration() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"TransactionSynchronizationFactoryParserTests-config.xml", this.getClass());

		DefaultTransactionSynchronizationFactory syncFactory = context.getBean("syncFactoryComplete",
				DefaultTransactionSynchronizationFactory.class);
		assertThat(syncFactory).isNotNull();
		TransactionSynchronizationProcessor processor = TestUtils.getPropertyValue(syncFactory, "processor",
				ExpressionEvaluatingTransactionSynchronizationProcessor.class);
		assertThat(processor).isNotNull();

		MessageChannel beforeCommitResultChannel = TestUtils.getPropertyValue(processor, "beforeCommitChannel",
				MessageChannel.class);
		assertThat(beforeCommitResultChannel).isNotNull();
		assertThat(context.getBean("beforeCommitChannel")).isEqualTo(beforeCommitResultChannel);
		Object beforeCommitExpression = TestUtils.getPropertyValue(processor, "beforeCommitExpression");
		assertThat(beforeCommitExpression).isNull();

		MessageChannel afterCommitResultChannel = TestUtils.getPropertyValue(processor, "afterCommitChannel",
				MessageChannel.class);
		assertThat(afterCommitResultChannel).isNotNull();
		assertThat(context.getBean("nullChannel")).isEqualTo(afterCommitResultChannel);
		Expression afterCommitExpression = TestUtils.getPropertyValue(processor, "afterCommitExpression",
				Expression.class);
		assertThat(afterCommitExpression).isNotNull();
		assertThat(((SpelExpression) afterCommitExpression).getExpressionString()).isEqualTo("'afterCommit'");

		MessageChannel afterRollbackResultChannel = TestUtils.getPropertyValue(processor, "afterRollbackChannel",
				MessageChannel.class);
		assertThat(afterRollbackResultChannel).isNotNull();
		assertThat(context.getBean("afterRollbackChannel")).isEqualTo(afterRollbackResultChannel);
		Expression afterRollbackExpression = TestUtils.getPropertyValue(processor, "afterRollbackExpression",
				Expression.class);
		assertThat(afterRollbackExpression).isNotNull();
		assertThat(((SpelExpression) afterRollbackExpression).getExpressionString()).isEqualTo("'afterRollback'");
		context.close();
	}

}
