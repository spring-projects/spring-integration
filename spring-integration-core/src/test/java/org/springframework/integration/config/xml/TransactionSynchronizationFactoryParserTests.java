/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.TransactionSynchronizationProcessor;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Glenn Renfro
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
		TransactionSynchronizationProcessor processor = TestUtils.getPropertyValue(syncFactory, "processor");
		assertThat(processor).isNotNull();

		MessageChannel beforeCommitResultChannel = TestUtils.getPropertyValue(processor, "beforeCommitChannel");
		assertThat(beforeCommitResultChannel).isNotNull();
		assertThat(context.getBean("beforeCommitChannel")).isEqualTo(beforeCommitResultChannel);
		Object beforeCommitExpression = TestUtils.getPropertyValue(processor, "beforeCommitExpression");
		assertThat(beforeCommitExpression).isNull();

		MessageChannel afterCommitResultChannel = TestUtils.getPropertyValue(processor, "afterCommitChannel");
		assertThat(afterCommitResultChannel).isNotNull();
		assertThat(context.getBean("nullChannel")).isEqualTo(afterCommitResultChannel);
		Expression afterCommitExpression = TestUtils.getPropertyValue(processor, "afterCommitExpression");
		assertThat(afterCommitExpression).isNotNull();
		assertThat(((SpelExpression) afterCommitExpression).getExpressionString()).isEqualTo("'afterCommit'");

		MessageChannel afterRollbackResultChannel = TestUtils.getPropertyValue(processor, "afterRollbackChannel");
		assertThat(afterRollbackResultChannel).isNotNull();
		assertThat(context.getBean("afterRollbackChannel")).isEqualTo(afterRollbackResultChannel);
		Expression afterRollbackExpression = TestUtils.getPropertyValue(processor, "afterRollbackExpression");
		assertThat(afterRollbackExpression).isNotNull();
		assertThat(((SpelExpression) afterRollbackExpression).getExpressionString()).isEqualTo("'afterRollback'");
		context.close();
	}

}
