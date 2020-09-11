/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.integration.webflux.inbound;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Flux;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class WebFluxInboundEndpointTests {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private WebFluxInboundEndpoint simpleInboundEndpoint;

	@Test
	public void testSimpleGet() {
		this.webTestClient.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");

		this.simpleInboundEndpoint.stop();

		this.webTestClient.get().uri("/test")
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	public void testJsonResult() {
		this.webTestClient.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].name").isEqualTo("Jane")
				.jsonPath("$[1].name").isEqualTo("Jason")
				.jsonPath("$[2].name").isEqualTo("John");
	}

	@Test
	public void testServerInternalErrorRequest() {
		this.webTestClient
				.get()
				.uri("/error")
				.accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus()
				.is5xxServerError();
	}

	@Test
	public void testPostWithEmptyBody() {
		this.webTestClient
				.post()
				.uri("/post?foo=foo")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("{foo=[foo]}");
	}

	@Configuration
	@EnableWebFlux
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public WebTestClient webTestClient(ApplicationContext applicationContext) {
			return WebTestClient.bindToApplicationContext(applicationContext).build();
		}

		@Bean
		public WebFluxInboundEndpoint simpleInboundEndpoint() {
			WebFluxInboundEndpoint endpoint = new WebFluxInboundEndpoint();
			RequestMapping requestMapping = new RequestMapping();
			requestMapping.setPathPatterns("/test");
			endpoint.setRequestMapping(requestMapping);
			endpoint.setRequestChannelName("serviceChannel");
			return endpoint;
		}

		@ServiceActivator(inputChannel = "serviceChannel")
		String service() {
			return "It works!";
		}

		@Bean
		public WebFluxInboundEndpoint jsonInboundEndpoint() {
			WebFluxInboundEndpoint endpoint = new WebFluxInboundEndpoint();
			RequestMapping requestMapping = new RequestMapping();
			requestMapping.setPathPatterns("/persons");
			endpoint.setRequestMapping(requestMapping);
			endpoint.setRequestChannel(fluxResultChannel());
			return endpoint;
		}

		@Bean
		public MessageChannel fluxResultChannel() {
			return new FluxMessageChannel();
		}

		@ServiceActivator(inputChannel = "fluxResultChannel")
		Flux<Person> getPersons() {
			return Flux.just(new Person("Jane"), new Person("Jason"), new Person("John"));
		}


		@Bean
		public WebFluxInboundEndpoint errorInboundEndpoint() {
			WebFluxInboundEndpoint endpoint = new WebFluxInboundEndpoint();
			RequestMapping requestMapping = new RequestMapping();
			requestMapping.setPathPatterns("/error");
			endpoint.setRequestMapping(requestMapping);
			endpoint.setRequestChannelName("errorServiceChannel");
			return endpoint;
		}

		@ServiceActivator(inputChannel = "errorServiceChannel")
		public ResponseEntity<String> processHttpRequest() {
			return new ResponseEntity<>("<500 Internal Server Error,{}>", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		@Bean
		public WebFluxInboundEndpoint postInboundEndpoint() {
			WebFluxInboundEndpoint endpoint = new WebFluxInboundEndpoint();
			RequestMapping requestMapping = new RequestMapping();
			requestMapping.setPathPatterns("/post");
			requestMapping.setMethods(HttpMethod.POST);
			endpoint.setRequestMapping(requestMapping);
			endpoint.setRequestChannelName("postServiceChannel");
			return endpoint;
		}

		@ServiceActivator(inputChannel = "postServiceChannel")
		String service(Object payload) {
			return payload.toString();
		}

	}


	static class Person {

		private final String name;

		@JsonCreator
		Person(@JsonProperty("name") String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Person person = (Person) o;
			return Objects.equals(this.name, person.name);
		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		@Override
		public String toString() {
			return "Person[name='" + this.name + "']";
		}

	}

}
