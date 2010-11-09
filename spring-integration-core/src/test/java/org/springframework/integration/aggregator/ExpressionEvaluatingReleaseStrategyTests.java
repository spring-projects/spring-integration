package org.springframework.integration.aggregator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.store.SimpleMessageGroup;

/**
 * @author Alex Peters
 * @author Dave Syer
 * 
 */
public class ExpressionEvaluatingReleaseStrategyTests {

	private ExpressionEvaluatingReleaseStrategy strategy;

	private SimpleMessageGroup messages = new SimpleMessageGroup("foo");

	@Before
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setup() {
		for (int i = 0; i < 5; i++) {
			messages.add(new GenericMessage(i + 1));
		}
	}

	@Test
	public void testCompletedWithSizeSpelEvaluated() throws Exception {
		strategy = new ExpressionEvaluatingReleaseStrategy("#root.size()==5");
		assertThat(strategy.canRelease(messages), is(true));
	}

	@Test
	public void testCompletedWithFilterSpelEvaluated() throws Exception {
		strategy = new ExpressionEvaluatingReleaseStrategy("!?[payload==5].empty");
		assertThat(strategy.canRelease(messages), is(true));
	}

	@Test
	public void testCompletedWithFilterSpelReturnsNotCompleted() throws Exception {
		strategy = new ExpressionEvaluatingReleaseStrategy("!?[payload==6].empty");
		assertThat(strategy.canRelease(messages), is(false));
	}

}
