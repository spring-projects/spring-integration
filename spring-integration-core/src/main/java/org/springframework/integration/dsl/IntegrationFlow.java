/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.integration.dsl;

import org.springframework.messaging.MessageChannel;

/**
 * The main Integration DSL abstraction.
 * <p>
 * The {@link StandardIntegrationFlow} implementation (produced by {@link IntegrationFlowBuilder})
 * represents a container for the integration components, which will be registered
 * in the application context. Typically is used as a {@code @Bean} definition:
 * <pre class="code">
 *  &#64;Bean
 *  public IntegrationFlow fileReadingFlow() {
 *      return IntegrationFlows
 *             .from(Files.inboundAdapter(tmpDir.getRoot()), e -&gt; e.poller(Pollers.fixedDelay(100)))
 *             .transform(Files.fileToString())
 *             .channel(MessageChannels.queue("fileReadingResultChannel"))
 *             .get();
 *  }
 * </pre>
 * <p>
 * Can be used as a Lambda for top level definition as well as sub-flow definitions:
 * <pre class="code">
 * &#64;Bean
 * public IntegrationFlow routerTwoSubFlows() {
 *     return f -&gt; f
 *               .split()
 *               .&lt;Integer, Boolean&gt;route(p -&gt; p % 2 == 0, m -&gt; m
 *                              .subFlowMapping(true, sf -&gt; sf.&lt;Integer&gt;handle((p, h) -&gt; p * 2))
 *                              .subFlowMapping(false, sf -&gt; sf.&lt;Integer&gt;handle((p, h) -&gt; p * 3)))
 *               .aggregate()
 *               .channel(MessageChannels..queue("routerTwoSubFlowsOutput"));
 * }
 *
 * </pre>
 * <p>
 * Also this interface can be implemented directly to encapsulate the integration logic
 * in the target service:
 * <pre class="code">
 *  &#64;Component
 *  public class MyFlow implements IntegrationFlow {
 *
 *        &#64;Override
 *        public void configure(IntegrationFlowDefinition&lt;?&gt; f) {
 *                f.&lt;String, String&gt;transform(String::toUpperCase);
 *        }
 *
 *  }
 * </pre>
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see IntegrationFlowBuilder
 * @see StandardIntegrationFlow
 * @see IntegrationFlowAdapter
 */
@FunctionalInterface
public interface IntegrationFlow {

	/**
	 * The callback-based function to declare the chain of EIP-methods to
	 * configure an integration flow with the provided {@link IntegrationFlowDefinition}.
	 * @param flow the {@link IntegrationFlowDefinition} to configure
	 */
	void configure(IntegrationFlowDefinition<?> flow);

	/**
	 * Return the first {@link MessageChannel} component
	 * which is essential a flow input channel.
	 * @return the channel.
	 * @since 5.0.4
	 */
	default MessageChannel getInputChannel() {
		return null;
	}

}
