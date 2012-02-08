/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.core;

import java.util.List;

import org.springframework.integration.jpa.support.parametersource.ParameterSource;

/**
 * The Interface containing all the JpaOperations those will be executed by
 * the Jpa Spring Integration components.
 * 
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @since 2.1
 * 
 */
public interface JpaOperations {

	/**
	 * 
	 * @param entity
	 */
	void delete(Object entity);
	
	/**
	 * 
	 * @param entities
	 */
	void deleteInBatch(Iterable<?> entities);
	
	/**
	 * Executes the given update statement and uses the given parameter source to 
	 * set the required query parameters.
	 * 
	 * @param updateQuery Must Not be empty.
	 * @param source Must Not be null.
	 * @return The number of entities updated
	 */
	int executeUpdate(String updateQuery, ParameterSource source);
	
	
	/**
	 * 
	 * @param updateQuery
	 * @param source
	 * @return The number of entities updated
	 */
	int executeUpdateWithNamedQuery(String updateQuery,  ParameterSource source);
	
	/**
	 * 
	 * @param updateQuery
	 * @param source
	 * @return The number of entities updated
	 */
	int executeUpdateWithNativeQuery(String updateQuery,  ParameterSource source); 
	
	
	/**
	 * Find an Entity of given type with the given primary key type.
	 * 
	 * @param <T>
	 * @param entityType
	 * @param id
	 * @return The entity if it exist, null is returned otherwise
	 */
	<T> T find(Class<T> entityType, Object id);
	
	/**
	 * 
	 * @param entityClass
	 * @return List of found entities
	 */
	List<Object> findAll(Class<?> entityClass);

	/**
	 * 
	 * @param entityClass
	 * @param i
	 * @param maxRowsPerPoll
	 * @return List of found entities
	 */
	List<?> getResultListForClass(Class<?> entityClass, int i,
			int maxRowsPerPoll);

	/**
	 * 
	 * @param selectNamedQuery
	 * @param jpaQLParameterSource
	 * @param i
	 * @param maxRowsPerPoll
	 * @return List of found entities
	 */
	List<?> getResultListForNamedQuery(String selectNamedQuery, ParameterSource jpaQLParameterSource, int i,
			int maxRowsPerPoll);
	
	/**
	 * 
	 * @param selectQuery
	 * @param entityClass
	 * @param jpaQLParameterSource
	 * @param i
	 * @param maxRowsPerPoll
	 * @return List of found entities
	 */
	List<?> getResultListForNativeQuery(String selectQuery,
			Class<?> entityClass,  ParameterSource jpaQLParameterSource, int i, int maxRowsPerPoll);
	
	/**
	 * Executes the provided query to return a list of results
	 * @param query
	 * @param source the Parameter source for this query to be executed, if none then set as null
	 * @return List of found entities
	 */
	List<?> getResultListForQuery(String query, ParameterSource source);
	
	/**
	 * Executes the provided query to return a list of results. 
	 * 
	 * @param query Must not be null or empty
	 * @param fromRecord Must be a non-negative value
	 * @param maxNumberOfRecord Must be a non-negative value
	 * @param source the Parameter source for this query to be executed, if none then set null
	 * @return List of found entities
	 */
	List<?> getResultListForQuery(String query, ParameterSource source,int fromRecord,int maxNumberOfRecord);
	
	/**
	 * Executes the provided query to return a single element
	 * 
	 * @param query Must not be empty
	 * @param source the Parameter source for this query to be executed, if none then set as null
	 * @return Will always return a result. If no object was found in the database an exception is raised.
	 */
	Object getSingleResultForQuery(String query, ParameterSource source);

	/**
	 * The entity to be merged with the entity manager
	 * 
	 * @param entity Must not be null.
	 * @return The merged managed instance of the entity. 
	 */
	Object merge(Object entity);

	/**
	 * Persists the entity 
	 * @param entity Must not be null
	 * 
	 */
	void persist(Object entity);

}
