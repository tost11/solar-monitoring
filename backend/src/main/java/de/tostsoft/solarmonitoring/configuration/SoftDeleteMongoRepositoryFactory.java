package de.tostsoft.solarmonitoring.configuration;

import java.util.Optional;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

public class SoftDeleteMongoRepositoryFactory extends MongoRepositoryFactory {
  private final MongoOperations mongoOperations;

  public SoftDeleteMongoRepositoryFactory(MongoOperations mongoOperations) {
    super(mongoOperations);
    this.mongoOperations = mongoOperations;
  }

  @Override
  protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
      QueryMethodEvaluationContextProvider evaluationContextProvider) {
    Optional<QueryLookupStrategy> optStrategy = super.getQueryLookupStrategy(key,
        evaluationContextProvider);
    return Optional.of(createSoftDeleteQueryLookupStrategy(optStrategy.get(), evaluationContextProvider));
  }

  private SoftDeleteMongoQueryLookupStrategy createSoftDeleteQueryLookupStrategy(QueryLookupStrategy strategy,
      QueryMethodEvaluationContextProvider evaluationContextProvider) {
    return new SoftDeleteMongoQueryLookupStrategy(strategy, mongoOperations, evaluationContextProvider);
  }
}