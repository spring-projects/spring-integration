package org.springframework.integration.aggregator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.integration.message.GenericMessage;

/**
 * @author Alex Peters
 * 
 */
public class ExpressionEvaluatingCorrelationStrategyTests {

	private ExpressionEvaluatingCorrelationStrategy strategy;

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInstanceWithEmptyExpressionFails() throws Exception {
		strategy = new ExpressionEvaluatingCorrelationStrategy("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInstanceWithNullExpressionFails() throws Exception {
		strategy = new ExpressionEvaluatingCorrelationStrategy(null);
	}

	@Test
	public void testCorrelationKeyWithMethodInvokingExpression() throws Exception {
		strategy = new ExpressionEvaluatingCorrelationStrategy("payload.substring(0,1)");
		Object correlationKey = strategy.getCorrelationKey(new GenericMessage<String>("bla"));
		assertThat(correlationKey, is(String.class));
		assertThat((String) correlationKey, is("b"));
	}
}
