package org.springframework.integration.twitter;

import org.springframework.integration.twitter.model.DirectMessage;
import org.springframework.integration.twitter.model.Status;
import org.springframework.stereotype.Component;

<<<<<<< HEAD
=======
import twitter4j.DirectMessage;
import twitter4j.Status;
>>>>>>> 64fec64d4095a6e793739607816d63d600e1ed0c


@Component
public class TwitterAnnouncer {
	public void dm(DirectMessage directMessage) {
		System.out.println("A direct message has been received from " +
				directMessage.getSender().getScreenName() + " with text " + directMessage.getText());
	}

	public void mention(Status s) {
		System.out.println("A tweet mentioning (or replying) to " + "you was received having text " + s.getText() + " from " + s.getSource());
	}

	public void friendsTimelineUpdated(Status t) {
		System.out.println("Received " + t.getText() + " from " + t.getSource());
	}
}
