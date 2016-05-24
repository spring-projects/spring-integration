/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.http.management;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.http.config.EnableIntegrationGraphController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * @author Artem Bilan
 * @since 4.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
@TestPropertySource(properties = "spring.application.name:testApplication")
public class IntegrationGraphControllerTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void testIntegrationGraphGet() throws Exception {
		this.mockMvc.perform(get("/testIntegration")
				.header(HttpHeaders.ORIGIN, "http://foo.bar.com")
				.accept(MediaType.parseMediaType("application/json;charset=UTF-8")))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(handler().handlerType(IntegrationGraphController.class))
				.andExpect(handler().methodName("getGraph"))
				.andExpect(jsonPath("$.nodes..name")
						.value(Matchers.containsInAnyOrder("nullChannel", "errorChannel",
								"_org.springframework.integration.errorLogger")))
//				.andDo(print())
				.andExpect(jsonPath("$.contentDescriptor.name").value("testApplication"))
				.andExpect(jsonPath("$.links").exists());
	}

	@Test
	public void testIntegrationGraphControllerParser() throws Exception {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"IntegrationGraphControllerParserTests-context.xml", getClass());


		HandlerMapping handlerMapping =
				context.getBean(RequestMappingHandlerMapping.class.getName(), HandlerMapping.class);

		HandlerAdapter handlerAdapter = context.getBean(RequestMappingHandlerAdapter.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/foo");
		request.addHeader(HttpHeaders.ORIGIN, "http://foo.bar.com");
		MockHttpServletResponse response = new MockHttpServletResponse();

		HandlerExecutionChain executionChain = handlerMapping.getHandler(request);
		assertNotNull(executionChain);

		Object handler = executionChain.getHandler();

		handlerAdapter.handle(request, response, handler);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertThat(response.getContentAsString(), containsString("\"name\":\"nullChannel\","));
		assertThat(response.getContentAsString(), not(containsString("\"name\":\"myChannel\",")));

		context.getBeanFactory().registerSingleton("myChannel", new DirectChannel());

		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/foo/refresh");
		response = new MockHttpServletResponse();

		executionChain = handlerMapping.getHandler(request);
		assertNotNull(executionChain);

		handler = executionChain.getHandler();

		handlerAdapter.handle(request, response, handler);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertThat(response.getContentAsString(), containsString("\"name\":\"myChannel\","));

		context.close();
	}

	@Configuration
	@EnableWebMvc
	@EnableIntegration
	@EnableIntegrationManagement(statsEnabled = "_org.springframework.integration.errorLogger.handler",
			countsEnabled = "!*",
			defaultLoggingEnabled = "false")
	@EnableIntegrationGraphController(path = "/testIntegration")
	public static class ContextConfiguration extends WebMvcConfigurerAdapter {

		@Override
		public void addCorsMappings(CorsRegistry registry) {
			registry.addMapping("/testIntegration/**")
					.allowedOrigins("http://foo.bar.com")
					.allowedMethods(HttpMethod.GET.name());

		}

	}

}
