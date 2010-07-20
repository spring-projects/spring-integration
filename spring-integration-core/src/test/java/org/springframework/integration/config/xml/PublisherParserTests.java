/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.TriggeredMessagePublisher;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PublisherParserTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void fixedDelay() {
		TriggeredMessagePublisher publisher = context.getBean("fixedDelayPublisher", TriggeredMessagePublisher.class);
		assertFalse(publisher.isAutoStartup());
		DirectFieldAccessor publisherAccessor = new DirectFieldAccessor(publisher);
		Trigger trigger = (Trigger) publisherAccessor.getPropertyValue("trigger");
		assertEquals(PeriodicTrigger.class, trigger.getClass());
		DirectFieldAccessor triggerAccessor = new DirectFieldAccessor(trigger);
		assertEquals(1234L, triggerAccessor.getPropertyValue("period"));
		assertEquals(Boolean.FALSE, triggerAccessor.getPropertyValue("fixedRate"));
		assertEquals(context.getBean("fixedDelayChannel"), publisherAccessor.getPropertyValue("outputChannel"));
		Expression payloadExpression = (Expression) new DirectFieldAccessor(
				publisherAccessor.getPropertyValue("task")).getPropertyValue("payloadExpression");
		assertEquals("'fixedDelayTest'", payloadExpression.getExpressionString());
	}

	@Test
	public void fixedRate() {
		TriggeredMessagePublisher publisher = context.getBean("fixedRatePublisher", TriggeredMessagePublisher.class);
		assertFalse(publisher.isAutoStartup());
		DirectFieldAccessor publisherAccessor = new DirectFieldAccessor(publisher);
		Trigger trigger = (Trigger) publisherAccessor.getPropertyValue("trigger");
		assertEquals(PeriodicTrigger.class, trigger.getClass());
		DirectFieldAccessor triggerAccessor = new DirectFieldAccessor(trigger);
		assertEquals(5678L, triggerAccessor.getPropertyValue("period"));
		assertEquals(Boolean.TRUE, triggerAccessor.getPropertyValue("fixedRate"));
		assertEquals(context.getBean("fixedRateChannel"), publisherAccessor.getPropertyValue("outputChannel"));
		Expression payloadExpression = (Expression) new DirectFieldAccessor(
				publisherAccessor.getPropertyValue("task")).getPropertyValue("payloadExpression");
		assertEquals("'fixedRateTest'", payloadExpression.getExpressionString());
	}

	@Test
	public void cron() {
		TriggeredMessagePublisher publisher = context.getBean("cronPublisher", TriggeredMessagePublisher.class);
		assertFalse(publisher.isAutoStartup());
		DirectFieldAccessor publisherAccessor = new DirectFieldAccessor(publisher);
		Trigger trigger = (Trigger) publisherAccessor.getPropertyValue("trigger");
		assertEquals(CronTrigger.class, trigger.getClass());
		assertEquals("7 6 5 4 3 ?", new DirectFieldAccessor(new DirectFieldAccessor(
				trigger).getPropertyValue("sequenceGenerator")).getPropertyValue("expression"));
		assertEquals(context.getBean("cronChannel"), publisherAccessor.getPropertyValue("outputChannel"));
		Expression payloadExpression = (Expression) new DirectFieldAccessor(
				publisherAccessor.getPropertyValue("task")).getPropertyValue("payloadExpression");
		assertEquals("'cronTest'", payloadExpression.getExpressionString());
	}

	@Test
	public void triggerRef() {
		TriggeredMessagePublisher publisher = context.getBean("triggerRefPublisher", TriggeredMessagePublisher.class);
		assertTrue(publisher.isAutoStartup());
		DirectFieldAccessor publisherAccessor = new DirectFieldAccessor(publisher);
		Trigger trigger = (Trigger) publisherAccessor.getPropertyValue("trigger");
		assertEquals(context.getBean("customTrigger"), trigger);
		assertEquals(context.getBean("triggerRefChannel"), publisherAccessor.getPropertyValue("outputChannel"));
		Expression payloadExpression = (Expression) new DirectFieldAccessor(
				publisherAccessor.getPropertyValue("task")).getPropertyValue("payloadExpression");
		assertEquals("'triggerRefTest'", payloadExpression.getExpressionString());
	}

}
