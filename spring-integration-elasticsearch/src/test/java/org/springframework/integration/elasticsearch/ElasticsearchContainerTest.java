/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.elasticsearch;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

/**
 * The base contract for JUnit tests based on the container for Elasticsearch.
 * The container is started once for the test suite, without the security of
 * Elasticsearch, and the Ryuk container stops it after the JVM exits.
 *
 * @author Sharang Gupta
 *
 * @since 7.3
 */
@Testcontainers(disabledWithoutDocker = true)
public interface ElasticsearchContainerTest {

	ElasticsearchContainer ELASTICSEARCH_CONTAINER =
			new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.5.5")
					.withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
					.withEnv("xpack.security.enabled", "false")
					.withEnv("cluster.routing.allocation.disk.threshold_enabled", "false")
					.waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200));

	@BeforeAll
	static void startContainer() {
		ELASTICSEARCH_CONTAINER.start();
	}

	static ReactiveElasticsearchOperations reactiveElasticsearchOperations() {
		ClientConfiguration configuration = ClientConfiguration.builder()
				.connectedTo(ELASTICSEARCH_CONTAINER.getHttpHostAddress())
				.build();
		Rest5Client restClient = Rest5Clients.getRest5Client(configuration);
		ReactiveElasticsearchClient client =
				new ReactiveElasticsearchClient(new Rest5ClientTransport(restClient, new JacksonJsonpMapper()));
		return new ReactiveElasticsearchTemplate(client,
				new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()));
	}

}
