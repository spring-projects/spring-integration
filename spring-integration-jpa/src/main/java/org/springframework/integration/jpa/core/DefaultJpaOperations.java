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
import java.util.Set;

import javax.persistence.Parameter;
import javax.persistence.Query;

import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.jpa.support.parametersource.PositionSupportingParameterSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Class similar to JPA template limited to the operations required for the JPA adapters/gateway
 * not using JpaTemplate as the class is deprecated since Spring 3.1
 * 
 * @author Amol Nayak
 * @author Gunnar Hillert
 * 
 * @since 2.2
 *
 */
public class DefaultJpaOperations extends AbstractJpaOperations {	
	
	/** {@inheritDoc} */
	@Transactional
	public void delete(Object entity) {
		Assert.notNull(entity, "The entity must not be null!");
		entityManager.remove(entity);
	}

	/** {@inheritDoc} */
	@Transactional
	public void deleteInBatch(Iterable<?> entities) {
		Assert.notNull(entities, "The entity must not be null!");
				
		QueryUtils.applyAndBind(QueryUtils.getQueryString(QueryUtils.DELETE_ALL_QUERY_STRING, entities.iterator().next().getClass().getName()), entities, entityManager)
				.executeUpdate();
	}

	/** {@inheritDoc} */
	@Transactional
	public int executeUpdate(String updateQuery,  ParameterSource source) {
		Query query = entityManager.createQuery(updateQuery);
		setParametersIfRequired(updateQuery, source, query);		
		return query.executeUpdate();
	}
	
	/** {@inheritDoc} */
	@Transactional
	public int executeUpdateWithNamedQuery(String updateQuery, ParameterSource source) {
		Query query = entityManager.createNamedQuery(updateQuery);
		setParametersIfRequired(updateQuery, source, query);		
		return query.executeUpdate();
	}	

	/** {@inheritDoc} */
	@Transactional
	public int executeUpdateWithNativeQuery(String updateQuery, ParameterSource source) {
		Query query = entityManager.createNativeQuery(updateQuery);
		setParametersIfRequired(updateQuery, source, query);		
		return query.executeUpdate();
	}

	/** {@inheritDoc} */
	@Transactional(readOnly=true)
	public <T> T find(Class<T> entityType, Object id) {		
		return entityManager.find(entityType, id);
	}	

	/** {@inheritDoc} */
	@Transactional(readOnly=true)
	public List<Object> findAll(Class<?> entityClass) {
		return entityManager.createQuery("from " + entityClass.getName(), Object.class).getResultList();
	}

	/**
	 * @param query
	 * @param source
	 * @return
	 */
	private Query getQuery(String queryString, ParameterSource source) {
		Query query = entityManager.createQuery(queryString);
		setParametersIfRequired(queryString, source, query);
		return query;
	}
	
	/** {@inheritDoc} */
	@Transactional(readOnly=true)
	public List<?> getResultListForClass(Class<?> entityClass, int i,
			int maxRowsPerPoll) {
		return entityManager.createQuery("from " + entityClass.getName(), entityClass).getResultList();
	}
	
	/** {@inheritDoc} */
	@Transactional(readOnly=true)
	public List<?> getResultListForNamedQuery(String selectNamedQuery, 
			ParameterSource parameterSource, int fromRecord, int maxNumberOfRecord) {
		
		final Query query = entityManager.createNamedQuery(selectNamedQuery);
		setParametersIfRequired(selectNamedQuery, parameterSource, query);	
		
		if(fromRecord >= 0) {
			query.setFirstResult(fromRecord);
		}
		
		if(maxNumberOfRecord > 0) {
			query.setMaxResults(maxNumberOfRecord);	
		}
		
		return query.getResultList();
		
	}

	/** {@inheritDoc} */
	@Transactional(readOnly=true)
	public List<?> getResultListForNativeQuery(String selectQuery, Class<?> entityClass,
			ParameterSource parameterSource, int fromRecord, int maxNumberOfRecord) {
		
		final Query query;
		
		if (entityClass == null) {
			query = entityManager.createNativeQuery(selectQuery);
		} else {
			query = entityManager.createNativeQuery(selectQuery, entityClass);
		}
		
		setParametersIfRequired(selectQuery, parameterSource, query);	
		
		if(fromRecord >= 0) {
			query.setFirstResult(fromRecord);
		}
		
		if(maxNumberOfRecord > 0) {
			query.setMaxResults(maxNumberOfRecord);	
		}
		
		return query.getResultList();
	}

	/** {@inheritDoc} */
	@Transactional(readOnly=true)
	public List<?> getResultListForQuery(String query, ParameterSource source) {
			return getResultListForQuery(query,source,-1,-1);
	}
	
	/** {@inheritDoc} */
	@Transactional(readOnly=true)
	public List<?> getResultListForQuery(String queryString, ParameterSource source,
			int fromRecord, int maxNumberOfRecord) {
		
		Query query = getQuery(queryString,source);
		
		if(fromRecord >= 0) {
			query.setFirstResult(fromRecord);
		}
		
		if(maxNumberOfRecord > 0) {
			query.setMaxResults(maxNumberOfRecord);	
		}
			
		return query.getResultList();
	}

	/** {@inheritDoc} */
	@Transactional(readOnly=true)
	public Object getSingleResultForQuery(String queryString, ParameterSource source) {
		Query query = getQuery(queryString,source);
		return query.getSingleResult();
	}

	/** {@inheritDoc} */
	@Transactional
	public Object merge(Object entity) {
		return entityManager.merge(entity);
	}

	/** {@inheritDoc} */
	@Transactional
	public void persist(Object entity) {
		entityManager.persist(entity);		//entityManager.flush()
	}

	/** Given a JPA QL, this methods gets all the parameters defined in this query and 
	 * use the {@link JPAQLParameterSource} to find their values and set them
	 * 
	 * @param queryString
	 * @param source
	 * @param query
	 */
	private void setParametersIfRequired(String queryString,
			 ParameterSource source, Query query) {
		Set<Parameter<?>> parameters = query.getParameters();
		
		if(parameters != null && !parameters.isEmpty()) {
			if(source != null) {
				for(Parameter<?> param:parameters) {
					String  paramName = param.getName();
					Integer position = param.getPosition();
					
					final Object paramValue;
					
					if (position != null) {
						
						if (source instanceof PositionSupportingParameterSource) {
							paramValue = ((PositionSupportingParameterSource) source).getValueByPosition(position - 1);
							query.setParameter(position, paramValue);
						} else {
							throw new JpaOperationFailedException("Positional Parameters are only support "
									+ "for PositionSupportingParameterSources.")
							.withOffendingJPAQl(queryString);
						}

					} else {
						
						if(StringUtils.hasText(paramName)) {
							paramValue = source.getValue(paramName);
							query.setParameter(paramName, paramValue);
						} else {
							throw new JpaOperationFailedException(
									"This parameter does not contain a parameter name. " +
									"Additionally it is not a postitional parameter, neither.")
							.withOffendingJPAQl(queryString);
						}
					}
										
				}
			} else {
				throw new IllegalArgumentException("Query has parameters but no parameter source provided");
			}
			
		}
	}

}
