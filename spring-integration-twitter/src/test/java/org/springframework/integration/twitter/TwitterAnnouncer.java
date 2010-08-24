package org.springframework.integration.twitter;

import org.springframework.stereotype.Component;
import twitter4j.DirectMessage;
import twitter4j.Status;


@Component
public class TwitterAnnouncer {
	public void dm(DirectMessage directMessage) {
		System.out.println("A direct message has been received from " + directMessage.getSenderScreenName() + " with text " + directMessage.getText());
	}

	public void mention(Status s) {
		System.out.println("A tweet mentioning (or replying) to " + "you was received having text " + s.getText() + " from " + s.getSource());
	}

	public void friendsTimelineUpdated(Status t) {
		System.out.println("Received " + t.getText() + " from " + t.getSource());
	}
}
