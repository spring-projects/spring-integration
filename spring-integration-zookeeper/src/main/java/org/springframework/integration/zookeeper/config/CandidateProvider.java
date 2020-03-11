/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.zookeeper.config;

import org.springframework.integration.leader.Candidate;

/**
 * Provider that can be used to customize the identifier used by the
 * {@link org.springframework.integration.leader.Candidate Leadership Candidate}.
 * This can be used to customize the data that is stored in Zookeeper for the specific zknode.
 *
 * @since 5.2.5
 */
@FunctionalInterface
public interface CandidateProvider {
	Candidate generate(String role);
}
