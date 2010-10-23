/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter;

//import twitter4j.Status;

//import twitter4j.Status;

import org.springframework.integration.twitter.model.Status;
import org.springframework.integration.twitter.model.Twitter4jStatusImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Simple base class for the reply and timeline cases (as well as any other {@link twitter4j.Status} implementations of
 * {@link twitter4j.TwitterResponse}.
 *
 * @author Josh Long
 */
abstract public class AbstractInboundTwitterStatusEndpointSupport
		extends AbstractInboundTwitterEndpointSupport<Status> {
	private Comparator<Status> statusComparator = new Comparator<Status>() {
		public int compare(Status status, Status status1) {
			return status.getCreatedAt().compareTo(status1.getCreatedAt());
		}
	};
	protected List<Status> fromTwitter4jStatus(List<twitter4j.Status> stats) { 		
		   List<Status> fwd = new ArrayList<Status>();
		   for (twitter4j.Status s : stats)
			   fwd.add(new Twitter4jStatusImpl(s));
		   return fwd;
	   }

	@Override
	protected void markLastStatusId(Status statusId) {
		this.markerId = statusId.getId();
	}

	@Override
	protected List<Status> sort(List<Status> rl) {
		List<Status> statusArrayList = new ArrayList<Status>();
		statusArrayList.addAll(rl);
		Collections.sort(statusArrayList, statusComparator);

		return statusArrayList;
	}
}
