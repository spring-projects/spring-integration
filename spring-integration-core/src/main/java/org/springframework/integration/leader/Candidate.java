/*
 * Copyright 2014-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.springframework.integration.leader;

/**
 * Interface that defines the contract for candidates to participate
 * in a leader election. The callback methods {@link #onGranted(Context)}
 * and {@link #onRevoked(Context)} are invoked when leadership is
 * granted and revoked.
 *
 * @author Patrick Peralta
 * @author Janne Valkealahti
 *
 */
public interface Candidate {

	/**
	 * Gets the role.
	 *
	 * @return a string indicating the name of the leadership role
	 *         this candidate is participating in; other candidates
	 *         present in the system with the same name will contend
	 *         for leadership
	 */
	String getRole();

	/**
	 * Gets the identifier.
	 *
	 * @return a unique ID for this candidate; no other candidate for
	 *         leader election should return the same id
	 */
	String getId();

	/**
	 * Callback method invoked when this candidate is elected leader.
	 * Implementations may chose to launch a background thread to
	 * perform leadership roles and return immediately. Another option
	 * is for implementations to perform all leadership work in the
	 * thread invoking this method. In the latter case, the
	 * method <em>must</em> respond to thread interrupts by throwing
	 * {@link java.lang.InterruptedException}. When the thread
	 * is interrupted, this indicates that this candidate is no
	 * longer leader.
	 *
	 * @param ctx leadership context
	 * @throws InterruptedException when this candidate is no longer leader
	 */
	void onGranted(Context ctx) throws InterruptedException;

	/**
	 * Callback method invoked when this candidate is no longer leader.
	 * Implementations should use this to shut down any resources
	 * (threads, network connections, etc) used to perform leadership work.
	 *
	 * @param ctx leadership context
	 */
	void onRevoked(Context ctx);

}
