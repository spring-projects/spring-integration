/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ContextHierarchyTests {

	@Test
	public void inputChannelInParentContext() {
		String prefix = "/org/springframework/integration/config/xml/ContextHierarchyTests-";
		ConfigurableApplicationContext parentContext = new ClassPathXmlApplicationContext(prefix + "parent.xml");
		ConfigurableApplicationContext childContext = new ClassPathXmlApplicationContext(
				new String[] {prefix + "child.xml"}, parentContext);

		Object parentInput = parentContext.getBean("input");
		Object childInput = childContext.getBean("input");
		Object endpoint = childContext.getBean("chain");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		Object endpointInput = accessor.getPropertyValue("inputChannel");
		assertThat(childInput).isEqualTo(parentInput);
		assertThat(endpointInput).isEqualTo(parentInput);

		parentContext.close();
		childContext.close();
	}

}
