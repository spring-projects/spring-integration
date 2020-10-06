/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.jpa.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;

import org.springframework.core.log.LogAccessor;
import org.springframework.integration.jpa.support.JpaUtils;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.jpa.support.parametersource.PositionSupportingParameterSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Class similar to JPA template limited to the operations required for the JPA adapters/gateway
 * not using JpaTemplate as the class is deprecated since Spring 3.1.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class DefaultJpaOperations extends AbstractJpaOperations {

	private static final LogAccessor LOGGER = new LogAccessor(DefaultJpaOperations.class);

	@Override
	public void delete(Object entity) {
		Assert.notNull(entity, "The entity must not be null!");
		getEntityManager().remove(entity);
	}

	@Override
	public void deleteInBatch(Iterable<?> entities) {
		Assert.notNull(entities, "entities must not be null.");
		Iterator<?> iterator = entities.iterator();
		if (!iterator.hasNext()) {
			return;
		}

		Class<?> entityClass = null;

		for (Object object : entities) {
			if (entityClass == null) {
				entityClass = object.getClass();
			}
			else {
				if (entityClass != object.getClass()) {
					throw new IllegalArgumentException("entities must be of the same type.");
				}
			}
		}

		EntityManager entityManager = getEntityManager();
		final String entityName = JpaUtils.getEntityName(entityManager, entityClass);
		final String queryString = JpaUtils.getQueryString(JpaUtils.DELETE_ALL_QUERY_STRING, entityName);

		JpaUtils.applyAndBind(queryString, entities, entityManager)
				.executeUpdate();

	}

	@Override
	public int executeUpdate(String updateQuery, ParameterSource source) {
		Query query = getEntityManager().createQuery(updateQuery);
		setParametersIfRequired(updateQuery, source, query);
		return query.executeUpdate();
	}

	@Override
	public int executeUpdateWithNamedQuery(String updateQuery, ParameterSource source) {
		Query query = getEntityManager().createNamedQuery(updateQuery);
		setParametersIfRequired(updateQuery, source, query);
		return query.executeUpdate();
	}

	@Override
	public int executeUpdateWithNativeQuery(String updateQuery, ParameterSource source) {
		Query query = getEntityManager().createNativeQuery(updateQuery);
		setParametersIfRequired(updateQuery, source, query);
		return query.executeUpdate();
	}

	@Override
	public <T> T find(Class<T> entityType, Object id) {
		return getEntityManager().find(entityType, id);
	}

	private Query getQuery(String queryString, ParameterSource source) {
		Query query = getEntityManager().createQuery(queryString);
		setParametersIfRequired(queryString, source, query);
		return query;
	}


	@Override
	public List<?> getResultListForClass(Class<?> entityClass, int firstResult, int maxNumberOfResults) {

		final String entityName = JpaUtils.getEntityName(getEntityManager(), entityClass);
		final Query query = getEntityManager().createQuery("select x from " + entityName + " x", entityClass);
		if (firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		if (maxNumberOfResults > 0) {
			query.setMaxResults(maxNumberOfResults);
		}

		return query.getResultList();

	}

	@Override
	public List<?> getResultListForNamedQuery(String selectNamedQuery,
			ParameterSource parameterSource, int firstResult, int maxNumberOfResults) {

		final Query query = getEntityManager().createNamedQuery(selectNamedQuery);
		setParametersIfRequired(selectNamedQuery, parameterSource, query);

		if (firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		if (maxNumberOfResults > 0) {
			query.setMaxResults(maxNumberOfResults);
		}

		return query.getResultList();

	}

	@Override
	public List<?> getResultListForNativeQuery(String selectQuery, @Nullable Class<?> entityClass,
			ParameterSource parameterSource, int firstResult, int maxNumberOfResults) {

		final Query query;

		if (entityClass == null) {
			query = getEntityManager().createNativeQuery(selectQuery);
		}
		else {
			query = getEntityManager().createNativeQuery(selectQuery, entityClass);
		}

		setParametersIfRequired(selectQuery, parameterSource, query);

		if (firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		if (maxNumberOfResults > 0) {
			query.setMaxResults(maxNumberOfResults);
		}

		return query.getResultList();
	}

	@Override
	public List<?> getResultListForQuery(String query, ParameterSource source) {
		return getResultListForQuery(query, source, 0, 0);
	}

	@Override
	public List<?> getResultListForQuery(String queryString, ParameterSource source,
			int firstResult, int maxNumberOfResults) {

		Query query = getQuery(queryString, source);

		if (firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		if (maxNumberOfResults > 0) {
			query.setMaxResults(maxNumberOfResults);
		}

		return query.getResultList();
	}

	@Override
	public Object getSingleResultForQuery(String queryString, ParameterSource source) {
		Query query = getQuery(queryString, source);
		return query.getSingleResult();
	}

	@Override
	@Nullable
	public Object merge(Object entity) {
		return merge(entity, 0, false);
	}

	@Override
	@Nullable
	public Object merge(Object entity, int flushSize, boolean clearOnFlush) {
		Assert.notNull(entity, "The object to merge must not be null.");
		return persistOrMerge(entity, true, flushSize, clearOnFlush);
	}

	@Override
	public void persist(Object entity) {
		persist(entity, 0, false);
	}

	@Override
	public void persist(Object entity, int flushSize, boolean clearOnFlush) {
		Assert.notNull(entity, "The object to persist must not be null.");
		persistOrMerge(entity, false, flushSize, clearOnFlush);
	}

	@Nullable
	private Object persistOrMerge(Object entity, boolean isMerge, int flushSize, boolean clearOnFlush) {
		Object result = null;

		EntityManager entityManager = getEntityManager();
		if (entity instanceof Iterable) {
			result = persistOrMergeIterable(entity, isMerge, flushSize, clearOnFlush);
		}
		else {
			if (isMerge) {
				result = entityManager.merge(entity);
			}
			else {
				entityManager.persist(entity);
			}
		}

		if (flushSize > 0) {
			entityManager.flush();
			if (clearOnFlush) {
				entityManager.clear();
			}
		}

		return result;
	}

	@Nullable
	private Object persistOrMergeIterable(Object entity, boolean isMerge, int flushSize, boolean clearOnFlush) {
		Object result = null;

		@SuppressWarnings("unchecked")
		Iterable<Object> entities = (Iterable<Object>) entity;

		AtomicInteger savedEntities = new AtomicInteger();
		AtomicInteger nullEntities = new AtomicInteger();

		List<Object> mergedEntities = new ArrayList<>();

		EntityManager entityManager = getEntityManager();
		for (Object iteratedEntity : entities) {
			if (iteratedEntity == null) {
				nullEntities.incrementAndGet();
			}
			else {
				if (isMerge) {
					mergedEntities.add(entityManager.merge(iteratedEntity));
				}
				else {
					entityManager.persist(iteratedEntity);
				}
				savedEntities.incrementAndGet();
				if (flushSize > 0 && savedEntities.get() % flushSize == 0) {
					entityManager.flush();
					if (clearOnFlush) {
						entityManager.clear();
					}
				}
			}
		}

		LOGGER.debug(() -> String.format("%s %s entities. %s NULL entities were ignored.",
				isMerge ? "Merged" : "Persisted", savedEntities.get(), nullEntities));

		if (isMerge) {
			result = mergedEntities;
		}
		return result;
	}

	/**
	 * Given a JPQL query, this method gets all parameters defined in this query and
	 * use the {@link ParameterSource} to find their values and set them.
	 *
	 */
	private void setParametersIfRequired(String queryString, @Nullable ParameterSource source, Query query) {
		Set<Parameter<?>> parameters = query.getParameters();

		if (parameters != null && !parameters.isEmpty()) {
			if (source != null) {
				for (Parameter<?> param : parameters) {
					String paramName = param.getName();
					Integer position = param.getPosition();

					final Object paramValue;

					if (position != null) {

						if (source instanceof PositionSupportingParameterSource) {
							paramValue = ((PositionSupportingParameterSource) source).getValueByPosition(position);
							query.setParameter(position, paramValue);
						}
						else {
							throw new JpaOperationFailedException("Positional Parameters are only support "
									+ "for PositionSupportingParameterSources.", queryString);
						}

					}
					else {

						if (StringUtils.hasText(paramName)) {
							paramValue = source.getValue(paramName);
							query.setParameter(paramName, paramValue);
						}
						else {
							throw new JpaOperationFailedException(
									"This parameter does not contain a parameter name. " +
											"Additionally it is not a positional parameter, neither.", queryString);
						}
					}

				}
			}
			else {
				throw new IllegalArgumentException("Query has parameters but no parameter source provided");
			}

		}
	}

}
