/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.webflux.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.security.Principal;
import java.time.Duration;
import java.util.Collections;

import javax.annotation.Resource;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.webflux.outbound.WebFluxRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 * @author Shiliang Li
 * @author Abhijit Sarkar
 * @author Gary Russell
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@DirtiesContext
public class WebFluxDslTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Autowired
	@Qualifier("webFluxWithReplyPayloadToFlux.handler")
	private WebFluxRequestExecutingMessageHandler webFluxWithReplyPayloadToFlux;

	@Resource(name = "httpReactiveProxyFlow.webflux:outbound-gateway#0")
	private WebFluxRequestExecutingMessageHandler httpReactiveProxyFlow;

	@Autowired
	@Qualifier("webFluxFlowWithReplyPayloadToFlux.input")
	private MessageChannel webFluxFlowWithReplyPayloadToFluxInput;

	private MockMvc mockMvc;

	private WebTestClient webTestClient;

	@Before
	public void setup() {
		this.mockMvc =
				MockMvcBuilders.webAppContextSetup(this.wac)
						.apply(SecurityMockMvcConfigurers.springSecurity())
						.build();

		this.webTestClient =
				WebTestClient.bindToApplicationContext(this.wac)
						.apply(SecurityMockServerConfigurers.springSecurity())
						.configureClient()
						.responseTimeout(Duration.ofSeconds(600))
						.build();
	}

	@Test
	public void testWebFluxFlowWithReplyPayloadToFlux() {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().setContentType(MediaType.TEXT_PLAIN);

			DataBufferFactory bufferFactory = response.bufferFactory();
			return response.writeWith(Mono.just(bufferFactory.wrap("FOO\nBAR\n".getBytes())))
					.then(Mono.defer(response::setComplete));
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		new DirectFieldAccessor(this.webFluxWithReplyPayloadToFlux)
				.setPropertyValue("webClient", webClient);

		QueueChannel replyChannel = new QueueChannel();

		Message<String> testMessage =
				MessageBuilder.withPayload("test")
						.setReplyChannel(replyChannel)
						.build();

		this.webFluxFlowWithReplyPayloadToFluxInput.send(testMessage);

		Message<?> receive = replyChannel.receive(10_000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<String> response = (Flux<String>) receive.getPayload();

		StepVerifier.create(response)
				.expectNext("FOO", "BAR")
				.verifyComplete();
	}

	@Test
	public void testHttpReactiveProxyFlow() throws Exception {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().setContentType(MediaType.TEXT_PLAIN);

			return response.writeWith(Mono.just(response.bufferFactory().wrap("FOO".getBytes())))
					.then(Mono.defer(response::setComplete));
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		new DirectFieldAccessor(this.httpReactiveProxyFlow)
				.setPropertyValue("webClient", webClient);

		this.mockMvc.perform(
				get("/service2")
						.with(httpBasic("guest", "guest"))
						.param("name", "foo"))
				.andExpect(
						content()
								.string("FOO"));
	}

	@Autowired
	private PollableChannel storeChannel;


	@Test
	@SuppressWarnings("unchecked")
	public void testHttpReactivePost() {
		this.webTestClient.post().uri("/reactivePost")
				.headers(headers -> headers.setBasicAuth("guest", "guest"))
				.body(Mono.just("foo\nbar\nbaz"), String.class)
				.exchange()
				.expectStatus().isAccepted();

		Message<?> store = this.storeChannel.receive(10_000);
		assertThat(store).isNotNull();
		assertThat(store.getPayload()).isInstanceOf(Flux.class);

		assertThat(store.getHeaders().get(HttpHeaders.USER_PRINCIPAL, Principal.class).getName()).isEqualTo("guest");

		StepVerifier
				.create((Publisher<String>) store.getPayload())
				.expectNext("foo", "bar", "baz")
				.verifyComplete();

	}

	@Test
	public void testHttpReactivePostWithError() {
		this.webTestClient.post().uri("/reactivePostErrors")
				.headers(headers -> headers.setBasicAuth("guest", "guest"))
				.body(Mono.just("foo\nbar\nbaz"), String.class)
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
				.expectBody(String.class)
				.value(Matchers.containsString("errorTest"));
	}

	@Test
	public void testSse() {
		Flux<String> responseBody =
				this.webTestClient.get().uri("/sse")
						.headers(headers -> headers.setBasicAuth("guest", "guest"))
						.exchange()
						.returnResult(String.class)
						.getResponseBody();

		StepVerifier
				.create(responseBody)
				.expectNext("foo", "bar", "baz")
				.verifyComplete();
	}

	@Test
	public void testDynamicHttpEndpoint() {
		IntegrationFlow flow =
				IntegrationFlows.from(WebFlux.inboundGateway("/dynamic")
						.requestMapping(r -> r.params("name"))
						.payloadExpression("#requestParams.name[0]"))
						.<String, String>transform(String::toLowerCase)
						.get();

		IntegrationFlowContext.IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(flow).register();

		this.webTestClient.get().uri("/dynamic?name=BAR")
				.headers(headers -> headers.setBasicAuth("guest", "guest"))
				.exchange()
				.expectBody(String.class)
				.isEqualTo("bar");

		flowRegistration.destroy();

		this.webTestClient.get().uri("/dynamic?name=BAZ")
				.headers(headers -> headers.setBasicAuth("guest", "guest"))
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Autowired
	private Validator validator;

	@Test
	public void testValidation() {
		IntegrationFlow flow =
				IntegrationFlows.from(
						WebFlux.inboundGateway("/validation")
								.requestMapping((mapping) -> mapping
										.methods(HttpMethod.POST)
										.consumes(MediaType.APPLICATION_JSON_VALUE))
								.requestPayloadType(
										ResolvableType.forClassWithGenerics(Flux.class, TestModel.class))
								.validator(this.validator))
						.bridge()
						.get();

		IntegrationFlowContext.IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(flow).register();

		this.webTestClient.post().uri("/validation")
				.headers(headers -> headers.setBasicAuth("guest", "guest"))
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\": \"\"}")
				.exchange()
				.expectStatus().isBadRequest();

		flowRegistration.destroy();
	}


	@Configuration
	@EnableWebFlux
	@EnableWebSecurity
	@EnableWebFluxSecurity
	@EnableIntegration
	public static class ContextConfiguration extends WebSecurityConfigurerAdapter implements WebFluxConfigurer {

		@Override
		public Validator getValidator() {
			return new TestModelValidator();
		}

		@Bean
		public UserDetails userDetails() {
			return User.withUsername("guest")
					.passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder()::encode)
					.password("guest")
					.roles("ADMIN")
					.build();
		}

		@Override
		@Bean
		public UserDetailsService userDetailsService() {
			return new InMemoryUserDetailsManager(userDetails());
		}

		@Bean
		public ReactiveUserDetailsService reactiveUserDetailsService() {
			return new MapReactiveUserDetailsService(userDetails());
		}


		@Bean
		public SecurityWebFilterChain reactiveSpringSecurityFilterChain(ServerHttpSecurity http) {
			return http.authorizeExchange()
					.anyExchange().hasRole("ADMIN")
					.and()
					.httpBasic()
					.and()
					.csrf().disable()
					.build();
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests()
					.anyRequest().hasRole("ADMIN")
					.and()
					.httpBasic()
					.and()
					.csrf().disable()
					.anonymous().disable();
		}

		@Bean
		public IntegrationFlow webFluxFlowWithReplyPayloadToFlux() {
			return f -> f
					.handle(WebFlux.outboundGateway("https://www.springsource.org/spring-integration")
									.httpMethod(HttpMethod.GET)
									.replyPayloadToFlux(true)
									.expectedResponseType(String.class),
							e -> e
									.id("webFluxWithReplyPayloadToFlux")
									.customizeMonoReply(
											(message, mono) ->
													mono.timeout(Duration.ofMillis(100))
															.retry()));
		}

		@Bean
		public IntegrationFlow httpReactiveProxyFlow() {
			return IntegrationFlows
					.from(Http.inboundGateway("/service2")
							.requestMapping(r -> r.params("name")))
					.handle(WebFlux.<MultiValueMap<String, String>>outboundGateway(m ->
							UriComponentsBuilder.fromUriString("https://www.springsource.org/spring-integration")
									.queryParams(m.getPayload())
									.build()
									.toUri())
							.httpMethod(HttpMethod.GET)
							.expectedResponseType(String.class))
					.get();
		}

		@Bean
		public IntegrationFlow httpReactiveInboundChannelAdapterFlow() {
			return IntegrationFlows
					.from(WebFlux.inboundChannelAdapter("/reactivePost")
							.requestMapping(m -> m.methods(HttpMethod.POST))
							.requestPayloadType(ResolvableType.forClassWithGenerics(Flux.class, String.class))
							.statusCodeFunction(e ->
									HttpMethod.POST.equals(e.getMethod())
											? HttpStatus.ACCEPTED
											: HttpStatus.BAD_REQUEST))
					.channel(c -> c.queue("storeChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow httpReactiveInboundGatewayFlowWithErrors() {
			return IntegrationFlows
					.from(WebFlux.inboundGateway("/reactivePostErrors")
							.requestMapping(m -> m.methods(HttpMethod.POST))
							.requestPayloadType(ResolvableType.forClassWithGenerics(Flux.class, String.class))
							.statusCodeFunction(e ->
									HttpMethod.POST.equals(e.getMethod())
											? HttpStatus.ACCEPTED
											: HttpStatus.BAD_REQUEST)
							.errorChannel(errorFlow().getInputChannel()))
					.channel(MessageChannels.flux())
					.handle((p, h) -> {
						throw new RuntimeException("errorTest");
					})
					.get();
		}

		@Bean
		public IntegrationFlow errorFlow() {
			return f -> f
					.enrichHeaders(h -> h.header(HttpHeaders.STATUS_CODE, HttpStatus.BAD_GATEWAY));
		}

		@Bean
		public IntegrationFlow sseFlow() {
			return IntegrationFlows
					.from(WebFlux.inboundGateway("/sse")
							.requestMapping(m -> m.produces(MediaType.TEXT_EVENT_STREAM_VALUE))
							.mappedResponseHeaders("*"))
					.enrichHeaders(Collections.singletonMap("aHeader", new String[] { "foo", "bar", "baz" }))
					.handle((p, h) -> Flux.fromArray(h.get("aHeader", String[].class)))
					.get();
		}

		@Bean
		public AccessDecisionManager accessDecisionManager() {
			return new AffirmativeBased(Collections.singletonList(new RoleVoter()));
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
