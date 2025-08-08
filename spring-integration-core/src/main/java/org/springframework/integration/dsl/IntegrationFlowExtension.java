/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.util.Map;

import org.springframework.integration.channel.DirectChannel;

/**
 * An {@link IntegrationFlowDefinition} extension for custom Java DSL operators
 * and reusable solutions.
 * For supporting method flow chain an implementation of this class has to return
 * an extension class from new methods, e.g.:
 * <pre class="code">
 * {@code
 * 	public class MyIntegrationFlowDefinition
 * 			extends IntegrationFlowExtension<MyIntegrationFlowDefinition> {
 *
 * 		public MyIntegrationFlowDefinition upperCaseAfterSplit() {
 * 			return split()
 * 					.transform("payload.toUpperCase()");
 *      }
 * }
 * }
 * </pre>
 * This way it will be used in the target configuration as natural DSL definition:
 * <pre class="code">
 * {@code
 *  &#064;Bean
 *  public IntegrationFlow myFlowDefinition() {
 * 		return
 * 				new MyIntegrationFlowDefinition()
 * 			            .log()
 * 						.upperCaseAfterSplit()
 * 						.aggregate()
 * 						.get();
 *  }
 * }
 * </pre>
 * This {@link IntegrationFlowExtension} can also be used for overriding
 * existing operators with extensions to any {@link IntegrationComponentSpec} extensions,
 * e.g. adding new options for target component configuration.
 *
 * @param <B> the {@link IntegrationFlowDefinition} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public abstract class IntegrationFlowExtension<B extends IntegrationFlowExtension<B>>
		extends IntegrationFlowDefinition<B> {

	private final DirectChannel inputChannel = new DirectChannel();

	protected IntegrationFlowExtension() {
		channel(this.inputChannel);
	}

	@Override
	public StandardIntegrationFlow get() {
		StandardIntegrationFlow targetIntegrationFlow = super.get();
		return new StandardIntegrationFlowExtension(targetIntegrationFlow.getIntegrationComponents(),
				this.inputChannel);
	}

	private static class StandardIntegrationFlowExtension extends StandardIntegrationFlow {

		private final DirectChannel inputChannel;

		StandardIntegrationFlowExtension(Map<Object, String> integrationComponents, DirectChannel inputChannel) {
			super(integrationComponents);
			this.inputChannel = inputChannel;
		}

		@Override
		public void setBeanName(String name) {
			super.setBeanName(name);
			this.inputChannel.setBeanName(name + ".input");
		}

	}

}
