/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.http.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.http.multipart.UploadedMultipartFile;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.SecuredChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.mock.web.MockPart;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Artem Bilan
 * @author Shiliang Li
 * @author Gary Russell
 * @author Oleksii Komlyk
 *
 * @since 5.0
 */
@SpringJUnitWebConfig
@DirtiesContext
public class HttpDslTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private HttpRequestExecutingMessageHandler serviceInternalGatewayHandler;

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() {
		this.mockMvc =
				MockMvcBuilders.webAppContextSetup(this.wac)
						.apply(springSecurity())
						.build();
	}


	@Test
	public void testHttpProxyFlow() throws Exception {
		ClientHttpRequestFactory mockRequestFactory = new MockMvcClientHttpRequestFactory(this.mockMvc);
		this.serviceInternalGatewayHandler.setRequestFactory(mockRequestFactory);

		this.mockMvc.perform(
				get("/service")
						.with(httpBasic("admin", "admin"))
						.param("name", "foo"))
				.andExpect(content().string("FOO"));

		this.mockMvc.perform(
				get("/service")
						.with(httpBasic("user", "user"))
						.param("name", "name"))
				.andExpect(status().isForbidden())
				.andExpect(content().string("Error"));
	}

	@Test
	public void testDynamicHttpEndpoint() throws Exception {
		IntegrationFlow flow =
				IntegrationFlows.from(Http.inboundGateway("/dynamic")
						.requestMapping(r -> r.params("name"))
						.payloadExpression("#requestParams.name[0]"))
						.<String, String>transform(String::toLowerCase)
						.get();

		IntegrationFlowContext.IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(flow).register();

		this.mockMvc.perform(
				get("/dynamic")
						.with(httpBasic("user", "user"))
						.param("name", "BAR"))
				.andExpect(content().string("bar"));

		flowRegistration.destroy();

		this.mockMvc.perform(
				get("/dynamic")
						.with(httpBasic("user", "user"))
						.param("name", "BAZ"))
				.andExpect(status().isNotFound());
	}

	@Autowired
	@Qualifier("multiPartFilesChannel")
	private PollableChannel multiPartFilesChannel;

	@Test
	@SuppressWarnings("unchecked")
	public void testMultiPartFiles() throws Exception {
		MockPart mockPart1 = new MockPart("a1", "file1", "ABC".getBytes(StandardCharsets.UTF_8));
		mockPart1.getHeaders().setContentType(MediaType.TEXT_PLAIN);
		MockPart mockPart2 = new MockPart("a1", "file2", "DEF".getBytes(StandardCharsets.UTF_8));
		mockPart2.getHeaders().setContentType(MediaType.TEXT_PLAIN);
		this.mockMvc.perform(
				multipart("/multiPartFiles")
						.part(mockPart1, mockPart2)
						.with(httpBasic("user", "user")))
				.andExpect(status().isOk());

		Message<?> result = this.multiPartFilesChannel.receive(10_000);

		assertThat(result).isNotNull();
		assertThat(result.getHeaders()).containsEntry("contentLength", -1L);
		assertThat(result)
				.extracting(Message::getPayload)
				.satisfies((payload) ->
						assertThat((Map<String, ?>) payload)
								.hasSize(1)
								.extracting((map) -> map.get("a1"))
								.asList()
								.hasSize(2)
								.satisfies((list) -> {
									assertThat(list)
											.element(0)
											.extracting((file) ->
													((UploadedMultipartFile) file).getOriginalFilename())
											.isEqualTo("file1");
									assertThat(list)
											.element(1)
											.extracting((file) ->
													((UploadedMultipartFile) file).getOriginalFilename())
											.isEqualTo("file2");
								}));
	}

	@Autowired
	private Validator validator;

	@Test
	public void testValidation() throws Exception {
		IntegrationFlow flow =
				IntegrationFlows.from(
						Http.inboundChannelAdapter("/validation")
								.requestMapping((mapping) -> mapping
										.methods(HttpMethod.POST)
										.consumes(MediaType.APPLICATION_JSON_VALUE))
								.requestPayloadType(TestModel.class)
								.validator(this.validator))
						.bridge()
						.get();

		IntegrationFlowContext.IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(flow).register();

		this.mockMvc.perform(
				post("/validation")
						.with(httpBasic("user", "user"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(status().reason("Validation failure"));

		flowRegistration.destroy();
	}

	@Test
	public void testBadRequest() throws Exception {
		IntegrationFlow flow =
				IntegrationFlows.from(
						Http.inboundGateway("/badRequest")
								.errorChannel((message, timeout) -> {
									throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
											"Not valid request param", ((ErrorMessage) message).getPayload());
								})
								.payloadExpression("#requestParams.p1"))
						.get();

		IntegrationFlowContext.IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(flow).register();

		this.mockMvc.perform(
				get("/badRequest")
						.with(httpBasic("user", "user"))
						.param("p2", "P2"))
				.andExpect(status().isBadRequest())
				.andExpect(status().reason("Not valid request param"));

		flowRegistration.destroy();
	}

	@Test
	public void testErrorChannelFlow() throws Exception {
		IntegrationFlow flow =
				IntegrationFlows.from(
						Http.inboundGateway("/errorFlow")
								.errorChannel(new FixedSubscriberChannel(
										new AbstractReplyProducingMessageHandler() {

											@Override
											protected Object handleRequestMessage(Message<?> requestMessage) {
												return "Error Response";
											}

										})))
						.transform((payload) -> {
							throw new RuntimeException("Error!");
						})
						.get();

		IntegrationFlowContext.IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(flow).register();

		this.mockMvc.perform(
				get("/errorFlow")
						.with(httpBasic("user", "user")))
				.andExpect(status().isOk())
				.andExpect(content().string("Error Response"));

		flowRegistration.destroy();
	}

	@Configuration
	@EnableWebSecurity
	@EnableIntegration
	public static class ContextConfiguration extends WebSecurityConfigurerAdapter {

		@Override
		@Bean
		public UserDetailsService userDetailsService() {
			InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

			manager.createUser(
					User.withUsername("admin")
							.passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder()::encode)
							.password("admin")
							.roles("ADMIN")
							.build());

			manager.createUser(
					User.withUsername("user")
							.passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder()::encode)
							.password("user")
							.roles("USER")
							.build());

			return manager;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests()
					.antMatchers("/service/internal/**").hasRole("ADMIN")
					.anyRequest().permitAll()
					.and()
					.httpBasic()
					.and()
					.csrf().disable()
					.anonymous().disable();
		}

		@Bean
		@SecuredChannel(interceptor = "channelSecurityInterceptor", sendAccess = "ROLE_ADMIN")
		public MessageChannel transformSecuredChannel() {
			return new DirectChannel();
		}

		@Bean
		public IntegrationFlow httpInternalServiceFlow() {
			return IntegrationFlows
					.from(Http.inboundGateway("/service/internal")
							.requestMapping(r -> r.params("name"))
							.payloadExpression("#requestParams.name"))
					.channel(transformSecuredChannel())
					.<List<String>, String>transform(p -> p.get(0).toUpperCase())
					.get();
		}

		@Bean
		public HttpRequestHandlerEndpointSpec httpService() {
			return Http.inboundGateway("/service")
					.requestMapping(r -> r.params("name"))
					.errorChannel("httpProxyErrorFlow.input");
		}

		@Bean
		public IntegrationFlow httpProxyFlow() {
			return IntegrationFlows
					.from(httpService())
					.handle(Http.outboundGateway("/service/internal?{params}")
									.uriVariable("params", "payload")
									.expectedResponseType(String.class)
									.extractResponseBody(false)
									.errorHandler(new HttpProxyResponseErrorHandler()),
							e -> e.id("serviceInternalGateway"))
					.get();
		}

		@Bean
		public IntegrationFlow httpProxyErrorFlow() {
			return f -> f
					.transform(Throwable::getCause)
					.<HttpClientErrorException>handle((p, h) ->
							new ResponseEntity<>(p.getResponseBodyAsString(), p.getStatusCode()));
		}

		@Bean
		public IntegrationFlow multiPartFilesFlow() {
			return IntegrationFlows
					.from(Http.inboundChannelAdapter("/multiPartFiles")
							.headerFunction("contentLength", (entity) -> entity.getHeaders().getContentLength()))
					.channel((c) -> c.queue("multiPartFilesChannel"))
					.get();
		}

		@Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
		public MultipartResolver multipartResolver() {
			return new StandardServletMultipartResolver();
		}

		@Bean
		public AccessDecisionManager accessDecisionManager() {
			return new AffirmativeBased(Collections.singletonList(new RoleVoter()));
		}

		@Bean
		public ChannelSecurityInterceptor channelSecurityInterceptor(AccessDecisionManager accessDecisionManager)
				throws Exception {
			ChannelSecurityInterceptor channelSecurityInterceptor = new ChannelSecurityInterceptor();
			channelSecurityInterceptor.setAuthenticationManager(authenticationManager());
			channelSecurityInterceptor.setAccessDecisionManager(accessDecisionManager);
			return channelSecurityInterceptor;
		}

		@Bean
		public Validator customValidator() {
			return new TestModelValidator();
		}

	}

	public static class HttpProxyResponseErrorHandler extends DefaultResponseErrorHandler {

		@Override
		protected byte[] getResponseBody(ClientHttpResponse response) {
			Charset charset = getCharset(response);
			String content = "Error";
			return charset != null ? content.getBytes(charset) : content.getBytes();
		}

	}

	public static class TestModel {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	private static class TestModelValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return TestModel.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object target, Errors errors) {
			TestModel testModel = (TestModel) target;
			if (!StringUtils.hasText(testModel.getName())) {
				errors.rejectValue("name", "Must not be empty");
			}
		}

	}

}
