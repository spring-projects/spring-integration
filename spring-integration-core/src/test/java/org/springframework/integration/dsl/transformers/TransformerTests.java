/*
 * Copyright 2017-2021 the original author or authors.
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

package org.springframework.integration.dsl.transformers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 * @author Ian Bondoc
 * @author Gary Russell
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class TransformerTests {

	@Autowired
	@Qualifier("enricherInput")
	private FixedSubscriberChannel enricherInput;

	@Autowired
	@Qualifier("enricherInput2")
	private FixedSubscriberChannel enricherInput2;

	@Autowired
	@Qualifier("enricherInput3")
	private FixedSubscriberChannel enricherInput3;

	@Autowired
	@Qualifier("enricherErrorChannel")
	private PollableChannel enricherErrorChannel;


	@Test
	public void testContentEnricher() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestPojo("Bar"))
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.enricherInput.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders().get("foo")).isEqualTo("Bar Bar");
		Object payload = receive.getPayload();
		assertThat(payload).isInstanceOf(TestPojo.class);
		TestPojo result = (TestPojo) payload;
		assertThat(result.getName()).isEqualTo("Bar Bar");
		assertThat(result.getDate()).isNotNull();
		assertThat(new Date()).isAfterOrEqualTo(result.getDate());

		this.enricherInput.send(new GenericMessage<>(new TestPojo("junk")));

		Message<?> errorMessage = this.enricherErrorChannel.receive(10_000);
		assertThat(errorMessage).isNotNull();
	}

	@Test
	public void testContentEnricher2() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestPojo("Bar"))
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.enricherInput2.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders().get("foo")).isNull();
		Object payload = receive.getPayload();
		assertThat(payload).isInstanceOf(TestPojo.class);
		TestPojo result = (TestPojo) payload;
		assertThat(result.getName()).isEqualTo("Bar Bar");
		assertThat(result.getDate()).isNotNull();
		assertThat(new Date()).isAfterOrEqualTo(result.getDate());
	}

	@Test
	public void testContentEnricher3() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestPojo("Bar"))
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.enricherInput3.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders().get("foo")).isEqualTo("Bar Bar");
		Object payload = receive.getPayload();
		assertThat(payload).isInstanceOf(TestPojo.class);
		TestPojo result = (TestPojo) payload;
		assertThat(result.getName()).isEqualTo("Bar");
		assertThat(result.getDate()).isNull();
	}

	@Autowired
	@Qualifier("replyProducingSubFlowEnricher.input")
	private MessageChannel replyProducingSubFlowEnricherInput;

	@Autowired
	@Qualifier("terminatingSubFlowEnricher.input")
	private MessageChannel terminatingSubFlowEnricherInput;

	@Autowired
	@Qualifier("subFlowTestReplyChannel")
	private PollableChannel subFlowTestReplyChannel;

	@Test
	public void testSubFlowContentEnricher() {
		this.replyProducingSubFlowEnricherInput.send(MessageBuilder.withPayload(new TestPojo("Bar")).build());
		Message<?> receive = this.subFlowTestReplyChannel.receive(5000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders().get("foo")).isEqualTo("Foo Bar (Reply Producing)");
		Object payload = receive.getPayload();
		assertThat(payload).isInstanceOf(TestPojo.class);
		TestPojo result = (TestPojo) payload;
		assertThat(result.getName()).isEqualTo("Foo Bar (Reply Producing)");

		this.terminatingSubFlowEnricherInput.send(MessageBuilder.withPayload(new TestPojo("Bar")).build());
		receive = this.subFlowTestReplyChannel.receive(5000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders().get("foo")).isEqualTo("Foo Bar (Terminating)");
		payload = receive.getPayload();
		assertThat(payload).isInstanceOf(TestPojo.class);
		result = (TestPojo) payload;
		assertThat(result.getName()).isEqualTo("Foo Bar (Terminating)");
	}

	@Autowired
	@Qualifier("encodingFlow.input")
	private MessageChannel encodingFlowInput;

	@Autowired
	@Qualifier("decodingFlow.input")
	private MessageChannel decodingFlowInput;

	@Autowired
	@Qualifier("codecReplyChannel")
	private PollableChannel codecReplyChannel;

	@Test
	public void testCodec() {
		this.encodingFlowInput.send(new GenericMessage<>("bar"));
		Message<?> receive = this.codecReplyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(byte[].class);
		byte[] transformed = (byte[]) receive.getPayload();
		assertThat(transformed).isEqualTo("foo".getBytes());

		this.decodingFlowInput.send(new GenericMessage<>(transformed));
		receive = this.codecReplyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(42);
	}


	@Autowired
	@Qualifier("pojoTransformFlow.input")
	private MessageChannel pojoTransformFlowInput;

	@Autowired
	private PollableChannel idempotentDiscardChannel;

	@Autowired
	private PollableChannel adviceChannel;

	@Test
	public void transformWithHeader() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("Foo")
				.setReplyChannel(replyChannel)
				.build();
		this.pojoTransformFlowInput.send(message);
		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("FooBar");


		assertThatExceptionOfType(MessageRejectedException.class)
				.isThrownBy(() -> this.pojoTransformFlowInput.send(message))
				.withMessageContaining("IdempotentReceiver")
				.withMessageContaining("rejected duplicate Message");

		assertThat(this.idempotentDiscardChannel.receive(10000)).isNotNull();
		assertThat(this.adviceChannel.receive(10000)).isNotNull();
	}

	@Autowired
	@Qualifier("convertFlow.input")
	private MessageChannel convertFlowInput;

	@Test
	public void testConvertOperator() {
		QueueChannel replyChannel = new QueueChannel();
		Date date = new Date();
		this.convertFlowInput.send(
				MessageBuilder.withPayload("{\"name\": \"Baz\",\"date\": " + date.getTime() + "}")
						.setHeader(MessageHeaders.CONTENT_TYPE, "application/json")
						.setReplyChannel(replyChannel)
						.build());

		Message<?> receive = replyChannel.receive(10_000);

		assertThat(receive).isNotNull();

		Object payload = receive.getPayload();

		assertThat(payload).isInstanceOf(TestPojo.class);

		TestPojo testPojo = (TestPojo) payload;

		assertThat(testPojo.getName()).isEqualTo("Baz");
		assertThat(testPojo.getDate()).isEqualTo(date);
	}

	@Autowired
	@Qualifier("transformFlowWithError.input")
	private MessageChannel transformFlowWithErrorInput;

	@Test
	public void testFailedTransformWithRequestHeadersCopy() {
		QueueChannel replyChannel = new QueueChannel();
		this.transformFlowWithErrorInput.send(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());

		final Message<?> receive = replyChannel.receive(10_000);

		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("transform failed");
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public PollableChannel enricherErrorChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow enricherFlow() {
			return IntegrationFlows.from("enricherInput", true)
					.enrich(e -> e.requestChannel("enrichChannel")
							.errorChannel(enricherErrorChannel())
							.requestPayloadExpression("payload")
							.shouldClonePayload(false)
							.propertyExpression("name", "payload['name']")
							.propertyFunction("date", m -> new Date())
							.headerExpression("foo", "payload['name']")
					)
					.logAndReply();
		}

		@Bean
		public IntegrationFlow enricherFlow2() {
			return IntegrationFlows.from("enricherInput2", true)
					.enrich(e -> e.requestChannel("enrichChannel")
							.requestPayloadExpression("payload")
							.shouldClonePayload(false)
							.propertyExpression("name", "payload['name']")
							.propertyExpression("date", "new java.util.Date()")
					)
					.get();
		}

		@Bean
		public IntegrationFlow enricherFlow3() {
			return IntegrationFlows.from("enricherInput3", true)
					.enrich(e -> e.requestChannel("enrichChannel")
							.requestPayload(Message::getPayload)
							.shouldClonePayload(false)
							.<Map<String, String>>headerFunction("foo", m -> m.getPayload().get("name")))
					.get();
		}

		@Bean
		public IntegrationFlow enrichFlow() {
			return IntegrationFlows.from("enrichChannel")
					.<TestPojo, Map<?, ?>>transform(p -> {
						if ("junk".equals(p.getName())) {
							throw new RuntimeException("intentional");
						}
						return Collections.singletonMap("name", p.getName() + " Bar");
					})
					.get();
		}

		@Bean
		public PollableChannel receivedChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel codecReplyChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow encodingFlow() {
			return f -> f
					.transform(Transformers.encoding(new MyCodec()))
					.channel("codecReplyChannel");
		}

		@Bean
		public IntegrationFlow decodingFlow() {
			return f -> f
					.transform(Transformers.decoding(new MyCodec(), m -> Integer.class))
					.channel("codecReplyChannel");
		}

		@Bean
		public IntegrationFlow pojoTransformFlow() {
			return f -> f
					.enrichHeaders(h -> h
							.header("Foo", "Bar")
							.advice(idempotentReceiverInterceptor(), requestHandlerAdvice()))
					.transform(new PojoTransformer());
		}

		@Bean
		public PollableChannel idempotentDiscardChannel() {
			return new QueueChannel();
		}

		@Bean
		public IdempotentReceiverInterceptor idempotentReceiverInterceptor() {
			IdempotentReceiverInterceptor idempotentReceiverInterceptor =
					new IdempotentReceiverInterceptor(new MetadataStoreSelector(m -> m.getPayload().toString()));
			idempotentReceiverInterceptor.setDiscardChannelName("idempotentDiscardChannel");
			idempotentReceiverInterceptor.setThrowExceptionOnRejection(true);
			return idempotentReceiverInterceptor;
		}

		@Bean
		public AbstractRequestHandlerAdvice requestHandlerAdvice() {
			return new AbstractRequestHandlerAdvice() {

				@Override
				protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
					adviceChannel().send(message);
					return callback.execute();
				}

			};
		}

		@Bean
		public PollableChannel adviceChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel subFlowTestReplyChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow someServiceFlow() {
			return f -> f
					.<String>handle((p, h) -> someService().someServiceMethod(p));
		}

		@Bean
		public IntegrationFlow replyProducingSubFlowEnricher() {
			return f -> f
					.enrich(e -> e.<TestPojo>requestPayload(p -> p.getPayload().getName())
							.requestSubFlow(someServiceFlow())
							.<String>headerFunction("foo", Message::getPayload)
							.propertyFunction("name", Message::getPayload))
					.channel("subFlowTestReplyChannel");
		}

		@Bean
		public MessageChannel enricherReplyChannel() {
			return MessageChannels.direct().get();
		}

		@Bean
		public IntegrationFlow terminatingSubFlowEnricher(SomeService someService) {
			return f -> f
					.enrich(e -> e.<TestPojo>requestPayload(p -> p.getPayload().getName())
							.requestSubFlow(sf -> sf
									.handle(someService::aTerminatingServiceMethod))
							.replyChannel("enricherReplyChannel")
							.<String>headerFunction("foo", Message::getPayload)
							.propertyFunction("name", Message::getPayload))
					.channel("subFlowTestReplyChannel");
		}

		@Bean
		public SomeService someService() {
			return new SomeService();
		}

		@Bean
		public IntegrationFlow convertFlow() {
			return f -> f
					.convert(TestPojo.class);
		}

		@Bean
		public ExpressionEvaluatingRequestHandlerAdvice expressionAdvice() {
			ExpressionEvaluatingRequestHandlerAdvice handlerAdvice = new ExpressionEvaluatingRequestHandlerAdvice();
			handlerAdvice.setOnFailureExpressionString("'transform failed'");
			handlerAdvice.setReturnFailureExpressionResult(true);
			return handlerAdvice;
		}

		@Bean
		public IntegrationFlow transformFlowWithError() {
			return f -> f
					.transform(p -> {
								throw new RuntimeException("intentional");
							},
							e -> e.advice(expressionAdvice()))
					.logAndReply();
		}

	}

	private static final class TestPojo {

		private String name;

		private Date date;

		private TestPojo() {
		}

		private TestPojo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@SuppressWarnings("unused")
		public void setName(String name) {
			this.name = name;
		}

		public Date getDate() {
			return date;
		}

		@SuppressWarnings("unused")
		public void setDate(Date date) {
			this.date = date;
		}

	}

	public static class MyCodec implements Codec {

		@Override
		public void encode(Object object, OutputStream outputStream) {
		}

		@Override
		public byte[] encode(Object object) {
			return "foo".getBytes();
		}

		@Override
		public <T> T decode(InputStream inputStream, Class<T> type) {
			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T decode(byte[] bytes, Class<T> type) {
			return (T) (String.class.isAssignableFrom(type) ? new String(bytes) :
					Integer.class.isAssignableFrom(type) ? Integer.valueOf(42) : Integer.valueOf(43));
		}

	}

	public static class PojoTransformer {

		@Transformer
		public String transform(String payload, @Header("Foo") String header) {
			return payload + header;
		}

	}

	public static class SomeService {

		@Autowired
		@Qualifier("enricherReplyChannel")
		public MessageChannel enricherReplyChannel;

		public String someServiceMethod(String value) {
			return "Foo ".concat(value).concat(" (Reply Producing)");
		}

		public void aTerminatingServiceMethod(Message<?> message) {
			String payload = "Foo ".concat(message.getPayload().toString()).concat(" (Terminating)");
			enricherReplyChannel.send(MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders()).build());
		}

	}

}
