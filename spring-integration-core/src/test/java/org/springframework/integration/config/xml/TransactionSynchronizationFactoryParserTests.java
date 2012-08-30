package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;

public class TransactionSynchronizationFactoryParserTests {

	@Test // nothing to assert. Validates only XSD
	public void validateXsdCombinationOfOrderOfSubelements(){
		new ClassPathXmlApplicationContext("TransactioinSynchronizationFactoryParserTests-xsd.xml", this.getClass());
	}

	@Test
	public void validateFullConfiguration(){
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("TransactioinSynchronizationFactoryParserTests-config.xml", this.getClass());

		ExpressionEvaluatingTransactionSynchronizationFactory syncFactory =
				context.getBean("syncFactoryComplete", ExpressionEvaluatingTransactionSynchronizationFactory.class);
		assertNotNull(syncFactory);
		ExpressionEvaluatingTransactionSynchronizationProcessor processor =
				TestUtils.getPropertyValue(syncFactory, "processor", ExpressionEvaluatingTransactionSynchronizationProcessor.class);
		assertNotNull(processor);

		MessageChannel beforeCommitResultChannel = TestUtils.getPropertyValue(processor, "beforeCommitResultChannel", MessageChannel.class);
		assertNotNull(beforeCommitResultChannel);
		assertEquals(beforeCommitResultChannel, context.getBean("beforeCommitChannel"));
		Object beforeCommitExpression = TestUtils.getPropertyValue(processor, "beforeCommitExpression");
		assertNull(beforeCommitExpression);

		MessageChannel afterCommitResultChannel = TestUtils.getPropertyValue(processor, "afterCommitResultChannel", MessageChannel.class);
		assertNotNull(afterCommitResultChannel);
		assertEquals(afterCommitResultChannel, context.getBean("nullChannel"));
		Expression afterCommitExpression = TestUtils.getPropertyValue(processor, "afterCommitExpression", Expression.class);
		assertNotNull(afterCommitExpression);
		assertEquals("'afterCommit'", ((SpelExpression)afterCommitExpression).getExpressionString());

		MessageChannel afterRollbackResultChannel = TestUtils.getPropertyValue(processor, "afterRollbackResultChannel", MessageChannel.class);
		assertNotNull(afterRollbackResultChannel);
		assertEquals(afterRollbackResultChannel, context.getBean("afterRollbackChannel"));
		Expression afterRollbackExpression = TestUtils.getPropertyValue(processor, "afterRollbackExpression", Expression.class);
		assertNotNull(afterRollbackExpression);
		assertEquals("'afterRollback'", ((SpelExpression)afterRollbackExpression).getExpressionString());
	}

}
