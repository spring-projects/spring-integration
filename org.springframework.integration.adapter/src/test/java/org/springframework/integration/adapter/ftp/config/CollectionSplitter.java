package org.springframework.integration.adapter.ftp.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.message.Message;

public class CollectionSplitter {

	@SuppressWarnings("unchecked")
	@Splitter public List split(Message<? extends Collection> message) {
		return new ArrayList(message.getPayload());
	}
}
