/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 ( the "License" );
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

package org.springframework.integration.redis.inbound;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Read Message from a Redis Stream and publish it to the indicated output channel.
 */
public class RedisStreamInboundChannelAdapter extends MessageProducerSupport {

	private final ReactiveRedisConnectionFactory reactiveConnectionFactory;

	private final MessageConverter messageConverter = new SimpleMessageConverter();

	private final String streamKey;

	private StreamReceiver receiver;

	private StreamReceiver.StreamReceiverOptions receiverOptions;

	private volatile String consumerGroupName;

	private volatile String consumerName;

	private volatile boolean createGroupIfNotExist = false;

	private  ReadOffset readOffset = ReadOffset.lastConsumed();

	private RedisTemplate template;

	public RedisStreamInboundChannelAdapter( String streamKey,
			ReactiveRedisConnectionFactory reactiveConnectionFactory ) {
		this.streamKey = streamKey;
		this.reactiveConnectionFactory = reactiveConnectionFactory;
	}

	public void setConsumerGroupName( @Nullable String consumerGroupName ) {
		this.consumerGroupName = consumerGroupName;
	}

	public void setConsumerName( @Nullable String consumerName ) {
		this.consumerName = consumerName;
	}

	public void setCreateGroupIfNotExist( boolean createGroupIfNotExist ) {
		this.createGroupIfNotExist = createGroupIfNotExist;
	}

	public void setReceiver( StreamReceiver receiver ) {
		this.receiver = receiver;
	}

	public void setReceiverOptions( StreamReceiver.StreamReceiverOptions receiverOptions ) {
		this.receiverOptions = receiverOptions;
	}

	public void setReadOffset( ReadOffset readOffset ) {
		this.readOffset = readOffset;
	}

	void setTemplate( RedisTemplate template ) {
		this.template = template;
	}

	@Override
	protected void onInit() {
		super.onInit();
		registerListener();
	}

	@Override
	protected void doStart() {
		super.doStart();
		createGroup();
	}
	
	@Override
	public String getComponentType() {
		return "redis:stream-inbound-channel-adapter";
	}

	private Message<?> convertMessage( Record<Object, Object> record ) {
		Map<String, Object> headers = new HashMap<>();
		headers.put( RedisHeaders.STREAM_KEY, record.getStream() );
		headers.put( RedisHeaders.STREAM_MESSAGE_ID, record.getId() );
		return this.messageConverter.toMessage( record.getValue(), new MessageHeaders( headers ) );
	}

	private void registerListener() {
		if ( this.receiverOptions != null ) {
			this.receiver = StreamReceiver.create( reactiveConnectionFactory, this.receiverOptions );
		}
		else {
			this.receiver = StreamReceiver.create( reactiveConnectionFactory );
		}

		StreamOffset offset = StreamOffset.create( this.streamKey, this.readOffset );

		if ( StringUtils.isEmpty( this.consumerGroupName ) ) {
			this.receiver
					.receive( offset )
					.subscribe( record -> sendMessage( convertMessage( ( Record<Object, Object> ) record ) ) );
		}
		else {
			Assert.hasText( consumerName, "'consumerName' must be set" );
			Consumer consumer = Consumer.from( this.consumerGroupName, this.consumerName );
			this.receiver
					.receiveAutoAck( consumer, offset )
					.subscribe( record -> sendMessage( convertMessage( ( Record<Object, Object> ) record ) ) );
		}
	}

	// TODO : follow the resolution of this Spring Data Redis issue:
	// https://jira.spring.io/projects/DATAREDIS/issues/DATAREDIS-1119
	// And improve this method when it will be solved.
	/**
	 * Create the Consumer Group if and only if it does not exist. During the
	 * creation we can also create the stream ie {@code MKSTREAM}, that does not
	 * have effect if the stream already exists.
	 */
	private void createGroup() {
		if ( createGroupIfNotExist ) {
			Assert.notNull( template, "'template' should not be null" );
			Assert.hasText( consumerGroupName, "'consumerGroupName' must be set" );
			Assert.hasText( consumerName, "'consumerName' must be set" );
			try {
				this.template.execute( ( RedisCallback<Object> ) connection -> connection.execute( "XGROUP",
						"CREATE".getBytes(), streamKey.getBytes(), consumerGroupName.getBytes(),
						ReadOffset.latest().getOffset().getBytes(), "MKSTREAM".getBytes() ) );
			}
			catch ( Exception e ) {
				// An exception is thrown when the group already exists
				e.printStackTrace();
			}
		}
	}
}
