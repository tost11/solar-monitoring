package de.tostsoft.solarmonitoring.configuration;

import java.lang.reflect.Method;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.ConvertingParameterAccessor;
import org.springframework.data.mongodb.repository.query.PartTreeMongoQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SoftDeleteMongoQueryLookupStrategy implements QueryLookupStrategy {
  private final QueryLookupStrategy strategy;
  private final MongoOperations mongoOperations;
  private final QueryMethodEvaluationContextProvider evaluationContextProvider;

  public SoftDeleteMongoQueryLookupStrategy(QueryLookupStrategy strategy,
      MongoOperations mongoOperations,
      QueryMethodEvaluationContextProvider evaluationContextProvider) {
    this.strategy = strategy;
    this.mongoOperations = mongoOperations;
    this.evaluationContextProvider = evaluationContextProvider;
  }

  @Override
  public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
      NamedQueries namedQueries) {
    RepositoryQuery repositoryQuery = strategy.resolveQuery(method, metadata, factory, namedQueries);

    // revert to the standard behavior if requested
    if (method.getAnnotation(SeesSoftlyDeletedRecords.class) != null) {
      return repositoryQuery;
    }

    if (!(repositoryQuery instanceof PartTreeMongoQuery)) {
      return repositoryQuery;
    }
    PartTreeMongoQuery partTreeQuery = (PartTreeMongoQuery) repositoryQuery;

    return new SoftDeletePartTreeMongoQuery(partTreeQuery);
  }

  private Criteria notDeleted() {
    return new Criteria().andOperator(
        Criteria.where("deletedAt").exists(false)
    );
  }

  private class SoftDeletePartTreeMongoQuery extends PartTreeMongoQuery {
    SoftDeletePartTreeMongoQuery(PartTreeMongoQuery partTreeQuery) {
      super(partTreeQuery.getQueryMethod(), mongoOperations, new SpelExpressionParser(), evaluationContextProvider);
    }

    @Override
    protected Query createQuery(ConvertingParameterAccessor accessor) {
      Query query = super.createQuery(accessor);
      return withNotDeleted(query);
    }

    @Override
    protected Query createCountQuery(ConvertingParameterAccessor accessor) {
      Query query = super.createCountQuery(accessor);
      return withNotDeleted(query);
    }

    private Query withNotDeleted(Query query) {
      return query.addCriteria(notDeleted());
    }
  }
}