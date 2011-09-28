/**
 * 
 */
package org.springframework.integration.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.integration.Message;
import org.springframework.util.Assert;

/**
 * @author ozhurakousky
 *
 */
public class MessageGroupWrapper implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private final Object groupId;
	
	private final List<UUID> markedMessageIds;

	private final List<UUID> unmarkedMessageIds;
	
	private final boolean complete;
	
	private final long timestamp;

	public MessageGroupWrapper(MessageGroup messageGroup){
		Assert.notNull(messageGroup, "'messageGroup' must not be null");
		this.groupId = messageGroup.getGroupId();
		
		this.markedMessageIds = new ArrayList<UUID>();
		for (Message<?> message : messageGroup.getMarked()) {
			markedMessageIds.add(message.getHeaders().getId());
		}
		
		this.unmarkedMessageIds = new ArrayList<UUID>();
		for (Message<?> message : messageGroup.getUnmarked()) {
			unmarkedMessageIds.add(message.getHeaders().getId());
		}
		
		this.complete = messageGroup.isComplete();
		
		this.timestamp = messageGroup.getTimestamp();
	}
	
	public Object getGroupId() {
		return groupId;
	}

	public List<UUID> getMarkedMessageIds() {
		return markedMessageIds;
	}

	public List<UUID> getUnmarkedMessageIds() {
		return unmarkedMessageIds;
	}

	public boolean isComplete() {
		return complete;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
