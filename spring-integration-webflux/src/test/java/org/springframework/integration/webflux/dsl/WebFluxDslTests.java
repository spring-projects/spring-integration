/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.webflux.dsl;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.webflux.outbound.WebFluxRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 * @author Shiliang Li
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
	private WebFluxRequestExecutingMessageHandler serviceInternalReactiveGatewayHandler;

	private MockMvc mockMvc;

	private WebTestClient webTestClient;

	@Before
	public void setup() {
		this.mockMvc =
				MockMvcBuilders.webAppContextSetup(this.wac)
						.apply(springSecurity())
						.build();

		this.webTestClient =
				WebTestClient.bindToApplicationContext(this.wac)
						.build();
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

		new DirectFieldAccessor(this.serviceInternalReactiveGatewayHandler)
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
				.body(Flux.just("foo", "bar", "baz"), String.class)
				.exchange()
				.expectStatus().isAccepted();

		Message<?> store = this.storeChannel.receive(10_000);
		assertNotNull(store);
		assertThat(store.getPayload(), instanceOf(Flux.class));

		StepVerifier
				.create((Publisher<String>) store.getPayload())
				.expectNext("foo", "bar", "baz")
				.verifyComplete();

	}

	@Test
	public void testSse() {
		Flux<String> responseBody =
				this.webTestClient.get().uri("/sse")
						.exchange()
						.returnResult(String.class)
						.getResponseBody();

		StepVerifier
				.create(responseBody)
				.expectNext("foo", "bar", "baz")
				.verifyComplete();
	}

	@Configuration
	@EnableWebFlux
	@EnableWebSecurity
	@EnableIntegration
	public static class ContextConfiguration extends WebSecurityConfigurerAdapter {

		@Bean
		public UserDetailsService userDetailsService() {
			InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

			manager.createUser(
					User.withDefaultPasswordEncoder()
							.username("guest")
							.password("guest")
							.roles("ADMIN")
							.build());

			return manager;
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
		public IntegrationFlow httpReactiveProxyFlow() {
			return IntegrationFlows
					.from(Http.inboundGateway("/service2")
							.requestMapping(r -> r.params("name")))
					.handle(WebFlux.<MultiValueMap<String, String>>outboundGateway(m ->
							UriComponentsBuilder.fromUriString("http://www.springsource.org/spring-integration")
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
							.statusCodeFunction(m -> HttpStatus.ACCEPTED))
					.channel(c -> c.queue("storeChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow sseFlow() {
			return IntegrationFlows
					.from(WebFlux.inboundGateway("/sse")
							.requestMapping(m -> m.produces(MediaType.TEXT_EVENT_STREAM_VALUE)))
					.handle((p, h) -> Flux.just("foo", "bar", "baz"))
					.get();
		}

		@Bean
		public AccessDecisionManager accessDecisionManager() {
			return new AffirmativeBased(Collections.singletonList(new RoleVoter()));
		}

	}

}
