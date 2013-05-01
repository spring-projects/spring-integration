package org.springframework.integration.channel.registry;

import org.springframework.integration.MessageChannel;

/**
 * A strategy Interface used to bind a {@link MessageChannel} to a logical name. The name is intended to identify a logical consumer or producer of messages.
 * This may be a queue, a channel adapter, another message channel, a Spring bean, etc.    
 * @author Mark Fisher
 * @author David Turanski
 * @since 3.0
 */
public interface ChannelRegistry {
	/**
	 * Register a message consumer
	 * @param name the logical identity of the message consumer
	 * @param channel the channel bound to the consumer
	 */
	void inbound(String name, MessageChannel channel);

	/**
	 * Register a message producer 
	 * @param name the logical identity of the message producer
	 * @param channel the channel bound to the producer
	 */
	void outbound(String name, MessageChannel channel);

	/**
	 * Create a tap on an already registered channel
	 * @param the registered name
	 * @param channel the output channel of the tap
	 */
	void tap(String name, MessageChannel channel);

}
