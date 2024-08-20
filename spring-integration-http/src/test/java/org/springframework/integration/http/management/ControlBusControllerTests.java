/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.integration.http.management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.http.config.EnableControlBusController;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Artem Bilan
 *
 * @since 6.4
 */
@SpringJUnitWebConfig
@DirtiesContext
public class ControlBusControllerTests {

	@Autowired
	WebApplicationContext wac;

	MockMvc mockMvc;

	@Autowired
	TestManagementComponent testManagementComponent;

	@BeforeEach
	void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	void allCommandsAreRegistered() throws Exception {
		this.mockMvc.perform(get("/control-bus")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(handler().handlerType(ControlBusController.class))
				.andExpect(handler().methodName("getCommands"))
				.andExpect(content().string(Matchers.containsString("testManagementComponent.operation")))
				.andExpect(content().string(Matchers.containsString("testManagementComponent.operation2")))
				.andExpect(content().string(Matchers.containsString("taskScheduler.setPoolSize")))
				.andExpect(content().string(Matchers.containsString("integrationHeaderChannelRegistry.runReaper")))
				.andExpect(content().string(Matchers.containsString("_org.springframework.integration.errorLogger.isRunning")))
				.andExpect(content().string(Matchers.containsString("The overloaded operation with int argument")))
				.andExpect(content().string(Matchers.containsString("The overloaded operation with two arguments")));
	}

	@Test
	void controlBusCommandIsPerformedOverRestCall() throws Exception {
		this.mockMvc.perform(post("/control-bus/testManagementComponent.operation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								[
									{
										"value": "1",
										"parameterType": "int"
									}
								]
								"""))
				.andExpect(status().isOk())
				.andExpect(handler().handlerType(ControlBusController.class))
				.andExpect(handler().methodName("invokeCommand"));

		verify(this.testManagementComponent).operation(eq(1));

		this.mockMvc.perform(post("/control-bus/testManagementComponent.operation2"))
				.andExpect(status().isOk())
				.andExpect(handler().handlerType(ControlBusController.class))
				.andExpect(handler().methodName("invokeCommand"))
				.andExpect(content().string("123"));

		verify(this.testManagementComponent).operation2();
	}

	@Configuration
	@EnableWebMvc
	@EnableIntegration
	@EnableControlBusController
	static class ContextConfiguration {

		@Bean
		TestManagementComponent testManagementComponent() {
			return spy(new TestManagementComponent());
		}

	}

	@ManagedResource
	private static class TestManagementComponent {

		@ManagedOperation
		public void operation() {

		}

		@ManagedOperation(description = "The overloaded operation with int argument")
		public void operation(int input) {

		}

		@ManagedOperation(description = "The overloaded operation with two arguments")
		public void operation(int input1, String input2) {

		}

		@ManagedOperation
		public int operation2() {
			return 123;
		}

	}

}
