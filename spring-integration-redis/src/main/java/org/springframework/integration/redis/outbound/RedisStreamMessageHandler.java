package org.springframework.integration.redis.outbound;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Take a message from a Channel and publish it to the given Redis Stream
 */
public class RedisStreamMessageHandler extends AbstractMessageHandler {

	private final StreamOperations streamOperations;

	private volatile RedisSerializer<?> serializer = StringRedisSerializer.UTF_8;

	private volatile Expression streamKeyExpression;

	private volatile boolean extractPayload = true;

	public RedisStreamMessageHandler( String streamKey, RedisConnectionFactory connectionFactory ) {
		Assert.notNull( streamKey, "'streamName' must not be null" );
		Assert.notNull( connectionFactory, "'connectionFactory' must not be null" );
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory( connectionFactory );
		template.setValueSerializer( this.serializer );
		template.setHashValueSerializer( this.serializer );
		template.setKeySerializer( StringRedisSerializer.UTF_8 );
		template.setHashKeySerializer( StringRedisSerializer.UTF_8 );
		template.afterPropertiesSet();
		this.streamOperations = template.opsForStream();
		this.setStreamKey( streamKey );
	}

	public void setSerializer( RedisSerializer<?> serializer ) {
		Assert.notNull( serializer, "'serializer' must not be null" );
		this.serializer = serializer;
	}

	public void setStreamKey( String streamKey ) {
		Assert.hasText( streamKey, "'streamKey' must not be an empty string." );
		this.streamKeyExpression = new LiteralExpression( streamKey );
	}

	public void setStreamKeyExpression( Expression streamKeyExpression ) {
		this.streamKeyExpression = streamKeyExpression;
	}

	public void setExtractPayload( boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public String getComponentType() {
		return "redis:stream-outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull( streamKeyExpression, "'streamKeyExpression' must not be null" );
	}

	@Override
	protected void handleMessageInternal( Message<?> message ) {

		String streamKey = this.streamKeyExpression.getExpressionString();

		Object value = message;

		if ( this.extractPayload) {
			value = message.getPayload();
		}

		ObjectRecord<String, Object> record = StreamRecords
				.<String, Object> objectBacked( value )
				.withStreamKey( streamKey );

		this.streamOperations.add( record );
	}

}
