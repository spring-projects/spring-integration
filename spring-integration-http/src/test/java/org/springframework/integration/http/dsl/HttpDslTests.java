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

package org.springframework.integration.http.dsl;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.SecuredChannel;
import org.springframework.messaging.MessageChannel;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Artem Bilan
 * @author Shiliang Li
 * @author Gary Russell
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@DirtiesContext
public class HttpDslTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private HttpRequestExecutingMessageHandler serviceInternalGatewayHandler;

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc =
				MockMvcBuilders.webAppContextSetup(this.wac)
						.apply(springSecurity())
						.build();
	}


	@Test
	public void testHttpProxyFlow() throws Exception {
		RestTemplate mockMvcRestTemplate = new RestTemplate(new MockMvcClientHttpRequestFactory(this.mockMvc));
		new DirectFieldAccessor(this.serviceInternalGatewayHandler)
				.setPropertyValue("restTemplate", mockMvcRestTemplate);

		this.mockMvc.perform(
				get("/service")
						.with(httpBasic("admin", "admin"))
						.param("name", "foo"))
				.andExpect(
						content()
								.string("FOO"));

		this.mockMvc.perform(
				get("/service")
						.with(httpBasic("user", "user"))
						.param("name", "name"))
				.andExpect(
						status()
								.isForbidden());
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
						.with(httpBasic("admin", "admin"))
						.param("name", "BAR"))
				.andExpect(
						content()
								.string("bar"));

		flowRegistration.destroy();

		this.mockMvc.perform(
				get("/dynamic")
						.with(httpBasic("admin", "admin"))
						.param("name", "BAZ"))
				.andExpect(
						status()
								.isNotFound());
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
		public IntegrationFlow httpProxyFlow() {
			return IntegrationFlows
					.from(Http.inboundGateway("/service")
							.requestMapping(r -> r.params("name"))
							.errorChannel("httpProxyErrorFlow.input"))
					.handle(Http.outboundGateway("/service/internal?{params}")
									.uriVariable("params", "payload")
									.expectedResponseType(String.class),
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

	}

}
