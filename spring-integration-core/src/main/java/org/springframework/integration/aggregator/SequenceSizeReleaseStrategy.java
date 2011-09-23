/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;

/**
 * An implementation of {@link ReleaseStrategy} that simply compares the current size of the message list to the
 * expected 'sequenceSize'.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 */
public class SequenceSizeReleaseStrategy implements ReleaseStrategy {

	private static final Log logger = LogFactory.getLog(SequenceSizeReleaseStrategy.class);

	private volatile Comparator<Message<?>> comparator = new SequenceNumberComparator();

	private volatile boolean releasePartialSequences;

	public SequenceSizeReleaseStrategy() {
		this(false);
	}

	public SequenceSizeReleaseStrategy(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	/**
	 * Flag that determines if partial sequences are allowed. If true then as soon as enough messages arrive that can be
	 * ordered they will be released, provided they all have sequence numbers greater than those already released.
	 * 
	 * @param releasePartialSequences
	 */
	public void setReleasePartialSequences(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	public boolean canRelease(MessageGroup messageGroup) {
		if (releasePartialSequences) {
			Collection<Message<?>> unmarked = messageGroup.getUnmarked();
			if (!unmarked.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Considering partial release of group [" + messageGroup + "]");
				}
				List<Message<?>> sorted = new ArrayList<Message<?>>(unmarked);
				Collections.sort(sorted, comparator);
				int tail = sorted.get(0).getHeaders().getSequenceNumber() - 1;
				boolean release = tail == messageGroup.getMarked().size();
				if (logger.isTraceEnabled() && release) {
					logger.trace("Release imminent because tail [" + tail + "] is next in line.");
				}
				return release;
			}
		}
		
		int size = messageGroup.getUnmarked().size() + messageGroup.getMarked().size();
		
		if (size == 0){
			messageGroup.complete();
		}
		else {
			int sequenceSize = messageGroup.getOne().getHeaders().getSequenceSize();
			// If there is no sequence then it must be incomplete....
			if (sequenceSize > 0 && sequenceSize == size){
				messageGroup.complete();
			}
		}
		
		return messageGroup.isComplete();
	}

}
