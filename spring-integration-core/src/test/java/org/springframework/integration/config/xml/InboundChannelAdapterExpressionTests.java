/*
 * Copyright 2002-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class InboundChannelAdapterExpressionTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void fixedDelay() {
		SourcePollingChannelAdapter adapter =
				this.context.getBean("fixedDelayProducer", SourcePollingChannelAdapter.class);
		assertThat(adapter.isAutoStartup()).isFalse();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Trigger trigger = TestUtils.getPropertyValue(adapter, "trigger", Trigger.class);
		assertThat(trigger.getClass()).isEqualTo(PeriodicTrigger.class);
		DirectFieldAccessor triggerAccessor = new DirectFieldAccessor(trigger);
		assertThat(triggerAccessor.getPropertyValue("period")).isEqualTo(1234L);
		assertThat(triggerAccessor.getPropertyValue("fixedRate")).isEqualTo(Boolean.FALSE);
		assertThat(adapterAccessor.getPropertyValue("outputChannel"))
				.isEqualTo(this.context.getBean("fixedDelayChannel"));
		Expression expression = TestUtils.getPropertyValue(adapter, "source.expression", Expression.class);
		assertThat(expression.getExpressionString()).isEqualTo("'fixedDelayTest'");
	}

	@Test
	public void fixedRate() {
		SourcePollingChannelAdapter adapter =
				this.context.getBean("fixedRateProducer", SourcePollingChannelAdapter.class);
		assertThat(adapter.isAutoStartup()).isFalse();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Trigger trigger = TestUtils.getPropertyValue(adapter, "trigger", Trigger.class);
		assertThat(trigger.getClass()).isEqualTo(PeriodicTrigger.class);
		DirectFieldAccessor triggerAccessor = new DirectFieldAccessor(trigger);
		assertThat(triggerAccessor.getPropertyValue("period")).isEqualTo(5678L);
		assertThat(triggerAccessor.getPropertyValue("fixedRate")).isEqualTo(Boolean.TRUE);
		assertThat(adapterAccessor.getPropertyValue("outputChannel"))
				.isEqualTo(this.context.getBean("fixedRateChannel"));
		Expression expression = TestUtils.getPropertyValue(adapter, "source.expression", Expression.class);
		assertThat(expression.getExpressionString()).isEqualTo("'fixedRateTest'");
	}

	@Test
	public void cron() {
		SourcePollingChannelAdapter adapter =
				this.context.getBean("cronProducer", SourcePollingChannelAdapter.class);
		assertThat(adapter.isAutoStartup()).isFalse();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Trigger trigger = TestUtils.getPropertyValue(adapter, "trigger", Trigger.class);
		assertThat(trigger.getClass()).isEqualTo(CronTrigger.class);
		assertThat(TestUtils.getPropertyValue(trigger, "expression.expression"))
				.isEqualTo("7 6 5 4 3 ?");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isEqualTo(this.context.getBean("cronChannel"));
		Expression expression = TestUtils.getPropertyValue(adapter, "source.expression", Expression.class);
		assertThat(expression.getExpressionString()).isEqualTo("'cronTest'");
	}

	@Test
	public void triggerRef() {
		SourcePollingChannelAdapter adapter =
				this.context.getBean("triggerRefProducer", SourcePollingChannelAdapter.class);
		assertThat(adapter.isAutoStartup()).isTrue();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Trigger trigger = TestUtils.getPropertyValue(adapter, "trigger", Trigger.class);
		assertThat(trigger).isEqualTo(this.context.getBean("customTrigger"));
		assertThat(adapterAccessor.getPropertyValue("outputChannel"))
				.isEqualTo(this.context.getBean("triggerRefChannel"));
		Expression expression = TestUtils.getPropertyValue(adapter, "source.expression", Expression.class);
		assertThat(expression.getExpressionString()).isEqualTo("'triggerRefTest'");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void headerExpressions() {
		SourcePollingChannelAdapter adapter =
				this.context.getBean("headerExpressionsProducer", SourcePollingChannelAdapter.class);
		assertThat(adapter.isAutoStartup()).isFalse();
		Map<String, Expression> headerExpressions =
				TestUtils.getPropertyValue(adapter, "source.headerExpressions", Map.class);
		assertThat(headerExpressions.size()).isEqualTo(2);
		assertThat(headerExpressions.get("foo").getExpressionString()).isEqualTo("6 * 7");
		assertThat(headerExpressions.get("bar").getExpressionString()).isEqualTo("x");
		assertThat(headerExpressions.get("foo").getValue()).isEqualTo(42);
		assertThat(headerExpressions.get("bar").getValue()).isEqualTo("x");
	}

	@Test
	public void testInt2867InnerExpression() {
		SourcePollingChannelAdapter adapter =
				this.context.getBean("expressionElement", SourcePollingChannelAdapter.class);
		Expression expression = TestUtils.getPropertyValue(adapter, "source.expression", Expression.class);
		assertThat(expression.getExpressionString()).isEqualTo("'Hello World!'");
	}

}
