/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.TransactionSynchronizationProcessor;
/**
 * @author Oleg Zhurakousky
 */
public class TransactionSynchronizationFactoryParserTests {

	@Test // nothing to assert. Validates only XSD
	public void validateXsdCombinationOfOrderOfSubelements(){
		new ClassPathXmlApplicationContext("TransactionSynchronizationFactoryParserTests-xsd.xml", this.getClass());
	}

	@Test
	public void validateFullConfiguration(){
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("TransactionSynchronizationFactoryParserTests-config.xml", this.getClass());

		DefaultTransactionSynchronizationFactory syncFactory =
				context.getBean("syncFactoryComplete", DefaultTransactionSynchronizationFactory.class);
		assertNotNull(syncFactory);
		TransactionSynchronizationProcessor processor =
				TestUtils.getPropertyValue(syncFactory, "processor", ExpressionEvaluatingTransactionSynchronizationProcessor.class);
		assertNotNull(processor);

		MessageChannel beforeCommitResultChannel = TestUtils.getPropertyValue(processor, "beforeCommitChannel", MessageChannel.class);
		assertNotNull(beforeCommitResultChannel);
		assertEquals(beforeCommitResultChannel, context.getBean("beforeCommitChannel"));
		Object beforeCommitExpression = TestUtils.getPropertyValue(processor, "beforeCommitExpression");
		assertNull(beforeCommitExpression);

		MessageChannel afterCommitResultChannel = TestUtils.getPropertyValue(processor, "afterCommitChannel", MessageChannel.class);
		assertNotNull(afterCommitResultChannel);
		assertEquals(afterCommitResultChannel, context.getBean("nullChannel"));
		Expression afterCommitExpression = TestUtils.getPropertyValue(processor, "afterCommitExpression", Expression.class);
		assertNotNull(afterCommitExpression);
		assertEquals("'afterCommit'", ((SpelExpression)afterCommitExpression).getExpressionString());

		MessageChannel afterRollbackResultChannel = TestUtils.getPropertyValue(processor, "afterRollbackChannel", MessageChannel.class);
		assertNotNull(afterRollbackResultChannel);
		assertEquals(afterRollbackResultChannel, context.getBean("afterRollbackChannel"));
		Expression afterRollbackExpression = TestUtils.getPropertyValue(processor, "afterRollbackExpression", Expression.class);
		assertNotNull(afterRollbackExpression);
		assertEquals("'afterRollback'", ((SpelExpression)afterRollbackExpression).getExpressionString());
	}

}
