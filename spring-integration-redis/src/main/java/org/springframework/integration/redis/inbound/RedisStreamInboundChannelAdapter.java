package org.springframework.integration.redis.inbound;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.hash.ObjectHashMapper;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.integration.endpoint.MessageProducerSupport;
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

    private volatile StreamMessageListenerContainer                                       container;

    private volatile StreamMessageListenerContainer.StreamMessageListenerContainerOptions containerOptions       = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
            .builder()
            .serializer( StringRedisSerializer.UTF_8 )
            .pollTimeout( Duration.ZERO )
            .executor( new SimpleAsyncTaskExecutor() )
            .batchSize( 1 )
            .objectMapper( new ObjectHashMapper() )
            .build();

    private final String streamKey;

    private volatile @Nullable String consumerGroupName;

    private volatile @Nullable String consumerName;

    private volatile boolean createGroupeIfNotExist = false;

    private volatile ReadOffset    readOffset = ReadOffset.lastConsumed();

    private final RedisTemplate<String, ?> template;

    private final MessageConverter messageConverter = new SimpleMessageConverter();

    private final RedisConnectionFactory connectionFactory;

    public RedisStreamInboundChannelAdapter( String streamKey, RedisConnectionFactory connectionFactory ) {
        this.streamKey = streamKey;
        this.connectionFactory = connectionFactory;
        this.template = new RedisTemplate<>();
        this.template.setConnectionFactory( this.connectionFactory );
        this.template.setKeySerializer( new StringRedisSerializer() );
        this.template.afterPropertiesSet();
    }

    @Override
    protected void onInit() {
        super.onInit();
        listenMessage();
    }

    @Override
    protected void doStart() {
        super.doStart();
        createGroup();
        this.container.start();
    }

    @Override
    protected void doStop() {
        super.doStop();
        this.container.stop();
    }

    @Override
    public String getComponentType() {
        return "redis:stream-inbound-channel-adapter";
    }

    private Message<?> convertMessage( Record<Object, Object> message ) {
        Map<String, Object> headers = new HashMap<>();
        headers.put( "streamKey", message.getStream() );
        headers.put( "Message-ID", message.getId() );
        return this.messageConverter.toMessage( message.getValue(), new MessageHeaders( headers ) );
    }

    private void listenMessage() {
        this.container = StreamMessageListenerContainer.create( connectionFactory, this.containerOptions );

        StreamListener<Object, Record<Object, Object>> streamListener = message -> sendMessage(
                convertMessage( message ) );

        StreamOffset offset = StreamOffset.create( this.streamKey, this.readOffset );

        if ( StringUtils.isEmpty( this.consumerGroupName ) ) {
            container.receive( offset, streamListener );
        } else {
            Assert.hasText( consumerName, "'consumerName' must be set" );
            Consumer consumer = Consumer.from( this.consumerGroupName, this.consumerName );
            container.receiveAutoAck( consumer, offset, streamListener );
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
        if ( createGroupeIfNotExist ) {
            Assert.hasText( consumerGroupName, "'consumerGroupName' must be set" );
            Assert.hasText( consumerName, "'consumerName' must be set" );
            try {
                template.execute( (RedisCallback<Object>) connection -> connection.execute( "XGROUP",
                        "CREATE".getBytes(), streamKey.getBytes(), consumerGroupName.getBytes(),
                        ReadOffset.latest().getOffset().getBytes(), "MKSTREAM".getBytes() ) );
            } catch ( Exception e ) {
                // An exception is thrown when the group already exists
               e.printStackTrace();
            }
        }
    }

    public void setConsumerGroupName( @Nullable String consumerGroupName ) {
        this.consumerGroupName = consumerGroupName;
    }

    public void setConsumerName( @Nullable String consumerName ) {
        this.consumerName = consumerName;
    }

    public void setCreateGroupeIfNotExist( boolean createGroupeIfNotExist ) {
        this.createGroupeIfNotExist = createGroupeIfNotExist;
    }

    public void setReadOffset( ReadOffset readOffset ) {
        this.readOffset = readOffset;
    }
}
