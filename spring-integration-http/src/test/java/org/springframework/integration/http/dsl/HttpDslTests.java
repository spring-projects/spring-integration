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

package org.springframework.integration.http.dsl;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.SecuredChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Artem Bilan
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
						.with(httpBasic("guest", "guest"))
						.param("name", "foo"))
				.andExpect(
						content()
								.string("FOO"));
	}


	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	@EnableIntegration
	public static class ContextConfiguration extends WebSecurityConfigurerAdapter {

		@Bean
		@Override
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication()
					.withUser("guest")
					.password("guest")
					.roles("ADMIN");
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
							.payloadFunction(httpEntity ->
									((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
											.getRequest()
											.getQueryString()))
					.handle(Http.outboundGateway(m -> "/service/internal?" + m.getPayload())
									.expectedResponseType(String.class),
							e -> e.id("serviceInternalGateway"))
					.get();
		}

		@Bean
		public AccessDecisionManager accessDecisionManager() {
			return new AffirmativeBased(Collections.singletonList(new RoleVoter()));
		}

		@Bean
		public ChannelSecurityInterceptor channelSecurityInterceptor(AuthenticationManager authenticationManager,
				AccessDecisionManager accessDecisionManager) {
			ChannelSecurityInterceptor channelSecurityInterceptor = new ChannelSecurityInterceptor();
			channelSecurityInterceptor.setAuthenticationManager(authenticationManager);
			channelSecurityInterceptor.setAccessDecisionManager(accessDecisionManager);
			return channelSecurityInterceptor;
		}

	}

}
