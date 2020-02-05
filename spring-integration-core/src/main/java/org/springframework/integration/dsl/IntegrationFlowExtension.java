/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Map;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

/**
 * @param <B> the {@link IntegrationFlowDefinition} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public abstract class IntegrationFlowExtension<B extends IntegrationFlowExtension<B>>
		extends IntegrationFlowDefinition<B> {

	protected IntegrationFlowExtension() {
		channel(new DirectChannel());
	}

	@Override
	public StandardIntegrationFlow get() {
		StandardIntegrationFlow targetIntegrationFlow = super.get();
		return new StandardIntegrationFlowExtension(targetIntegrationFlow.getIntegrationComponents());
	}

	private static class StandardIntegrationFlowExtension extends StandardIntegrationFlow
			implements BeanNameAware {

		StandardIntegrationFlowExtension(Map<Object, String> integrationComponents) {
			super(integrationComponents);
		}

		@Override
		public void setBeanName(String name) {
			MessageChannel inputChannel = getInputChannel();
			((BeanNameAware) inputChannel).setBeanName(name + ".input");
		}

	}

}
