/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.elasticsearch.outbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReactiveMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A reactive {@link org.springframework.messaging.MessageHandler} that indexes the payload of a
 * message into Elasticsearch, or deletes the document it refers to, with
 * {@link ReactiveElasticsearchOperations}.
 * <p>
 * With the {@link Type#INDEX} type (the default), the payload is saved as a document:
 * <ul>
 * <li>an entity is saved with its mapping, into the index of its
 * {@link org.springframework.data.elasticsearch.annotations.Document} annotation,
 * unless an index name is configured;</li>
 * <li>a {@link Map}, or a {@link String} with the JSON source of the document, is saved into the
 * configured index, and Elasticsearch generates its id, so a redelivered message is indexed again;</li>
 * <li>the elements of a {@link Collection} are saved like single payloads, with one bulk request.
 * Without an index name, they must be entities of the same class.</li>
 * </ul>
 * With the {@link Type#DELETE} type, the document with the id of the
 * {@link #setIdExpression(Expression) id expression} is deleted. Without an id expression, a
 * {@link String} payload is the id of the document to delete, and any other payload is the entity
 * to delete.
 * <p>
 * In an imperative flow, the framework subscribes to the returned {@link Mono} without waiting
 * for it, as for any {@link org.springframework.messaging.ReactiveMessageHandler}.
 *
 * @author Sharang Gupta
 *
 * @since 7.3
 */
public class ElasticsearchMessageHandler extends AbstractReactiveMessageHandler {

	private final ReactiveElasticsearchOperations elasticsearchOperations;

	private final Type type;

	private @Nullable Expression indexNameExpression;

	private @Nullable Expression idExpression;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	/**
	 * Create a handler that indexes the payload of each message.
	 * @param elasticsearchOperations the operations to save the documents with
	 */
	public ElasticsearchMessageHandler(ReactiveElasticsearchOperations elasticsearchOperations) {
		this(elasticsearchOperations, Type.INDEX);
	}

	/**
	 * Create a handler that performs the given operation for each message.
	 * @param elasticsearchOperations the operations to save or delete the documents with
	 * @param type the operation to perform
	 */
	public ElasticsearchMessageHandler(ReactiveElasticsearchOperations elasticsearchOperations, Type type) {
		Assert.notNull(elasticsearchOperations, "'elasticsearchOperations' must not be null");
		Assert.notNull(type, "'type' must not be null");
		this.elasticsearchOperations = elasticsearchOperations;
		this.type = type;
	}

	/**
	 * Set the name of the index for the documents. It overrides the index of the
	 * {@link org.springframework.data.elasticsearch.annotations.Document} annotation of an entity.
	 * @param indexName the name of the index
	 */
	public void setIndexName(String indexName) {
		setIndexNameExpression(new LiteralExpression(indexName));
	}

	/**
	 * Set an expression that evaluates the name of the index for each message. It overrides the
	 * index of the {@link org.springframework.data.elasticsearch.annotations.Document} annotation of
	 * an entity.
	 * @param indexNameExpression the expression for the name of the index
	 */
	public void setIndexNameExpression(Expression indexNameExpression) {
		Assert.notNull(indexNameExpression, "'indexNameExpression' must not be null");
		this.indexNameExpression = indexNameExpression;
	}

	/**
	 * Set an expression that evaluates the id of the document to delete for each message. It only
	 * applies to the {@link Type#DELETE} type.
	 * @param idExpression the expression for the id of the document to delete
	 */
	public void setIdExpression(Expression idExpression) {
		Assert.notNull(idExpression, "'idExpression' must not be null");
		this.idExpression = idExpression;
	}

	@Override
	public String getComponentType() {
		return "elasticsearch:outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(this.idExpression == null || this.type == Type.DELETE,
				"The 'idExpression' only applies to the DELETE type");
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected Mono<Void> handleMessageInternal(Message<?> message) {
		return Mono.defer(() -> switch (this.type) {
			case INDEX -> index(message);
			case DELETE -> delete(message);
		});
	}

	private Mono<Void> index(Message<?> message) {
		IndexCoordinates index = evaluateIndex(message);
		Object payload = message.getPayload();
		if (payload instanceof Collection<?> payloads) {
			return indexAll(payloads, index);
		}
		Object document = toDocument(payload, index);
		Mono<Object> saved = (index != null)
				? this.elasticsearchOperations.save(document, index)
				: this.elasticsearchOperations.save(document);
		return saved.then();
	}

	private Mono<Void> indexAll(Collection<?> payloads, @Nullable IndexCoordinates index) {
		if (payloads.isEmpty()) {
			return Mono.empty();
		}
		List<Object> documents = new ArrayList<>(payloads.size());
		for (Object payload : payloads) {
			Assert.state(payload != null, "A Collection payload must not contain null elements");
			documents.add(toDocument(payload, index));
		}
		if (index != null) {
			return this.elasticsearchOperations.saveAll(documents, index).then();
		}
		return this.elasticsearchOperations.saveAll(documents, entityClassOf(documents)).then();
	}

	@SuppressWarnings("unchecked")
	private static Class<Object> entityClassOf(List<Object> entities) {
		Class<?> entityClass = entities.get(0).getClass();
		Assert.state(entities.stream().allMatch(entity -> entity.getClass() == entityClass),
				"Without an index name, the elements of a Collection payload must be entities of the same class");
		return (Class<Object>) entityClass;
	}

	private static Object toDocument(Object payload, @Nullable IndexCoordinates index) {
		if (payload instanceof String json) {
			assertIndexForDocument(index);
			return Document.parse(json);
		}
		if (payload instanceof Map<?, ?> map) {
			assertIndexForDocument(index);
			return Document.from(asStringKeyedMap(map));
		}
		return payload;
	}

	private static void assertIndexForDocument(@Nullable IndexCoordinates index) {
		Assert.state(index != null, "An index name is required to index a Map or a JSON String payload");
	}

	private Mono<Void> delete(Message<?> message) {
		IndexCoordinates index = evaluateIndex(message);
		Object payload = message.getPayload();
		Assert.state(!(payload instanceof Collection<?>), "Deleting the documents of a Collection payload is not supported");
		String id = evaluateId(message);
		if (id == null && payload instanceof String payloadId) {
			id = payloadId;
		}
		if (id != null) {
			Assert.state(index != null, "An index name is required to delete a document by its id");
			return this.elasticsearchOperations.delete(id, index).then();
		}
		Mono<String> deleted = (index != null)
				? this.elasticsearchOperations.delete(payload, index)
				: this.elasticsearchOperations.delete(payload);
		return deleted.then();
	}

	private @Nullable IndexCoordinates evaluateIndex(Message<?> message) {
		if (this.indexNameExpression == null) {
			return null;
		}
		String indexName = this.indexNameExpression.getValue(this.evaluationContext, message, String.class);
		Assert.state(indexName != null, "'indexNameExpression' must not evaluate to null");
		return IndexCoordinates.of(indexName);
	}

	private @Nullable String evaluateId(Message<?> message) {
		if (this.idExpression == null) {
			return null;
		}
		String id = this.idExpression.getValue(this.evaluationContext, message, String.class);
		Assert.state(id != null, "'idExpression' must not evaluate to null");
		return id;
	}

	private static Map<String, Object> asStringKeyedMap(Map<?, ?> map) {
		Map<String, Object> result = new LinkedHashMap<>();
		map.forEach((key, value) -> result.put(String.valueOf(key), value));
		return result;
	}

	/**
	 * The operation to perform for each message.
	 */
	public enum Type {

		/**
		 * Save the payload as a document.
		 */
		INDEX,

		/**
		 * Delete the document of the payload.
		 */
		DELETE

	}

}
