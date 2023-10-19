package de.tostsoft.solarmonitoring.lib.configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.lang.Nullable;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

public class CustomMongoTemplate extends MongoTemplate {
  public CustomMongoTemplate(MongoTemplate mongoTemplate) {
    super(mongoTemplate.getMongoDatabaseFactory());
  }

  public CustomMongoTemplate(MongoClient mongoClient, String databaseName) {
    super(mongoClient, databaseName);
  }

  public CustomMongoTemplate(MongoDatabaseFactory mongoDbFactory) {
    super(mongoDbFactory);
  }

  public CustomMongoTemplate(MongoDatabaseFactory mongoDbFactory, MongoConverter mongoConverter) {
    super(mongoDbFactory, mongoConverter);
  }

  @Override
  public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
    if(query == null){
      throw new RuntimeException("Query must not be null!");
    }
    if(collectionName == null){
      throw new RuntimeException("CollectionName must not be null!");
    }
    if(entityClass == null){
      throw new RuntimeException("EntityClass must not be null!");
    }
    query.addCriteria(Criteria.where("deletedAt").exists(Boolean.FALSE));

    return super.find(query, entityClass, collectionName);
  }

  @Nullable
  @Override
  public <T> T findById(Object id, Class<T> entityClass, String collectionName) {
    T t = super.findById(id, entityClass, collectionName);

    if(t == null){
      return null;
    }

    try {
      Field field = entityClass.getDeclaredField("deletedAt");
      field.setAccessible(Boolean.TRUE);
      if (Objects.nonNull(field.get(t))) {
        return null;
      }
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
    }

    return t;
  }

  @Nullable
  @Override
  public <T> T findOne(Query query, Class<T> entityClass, String collectionName) {
    if(query == null){
      throw new RuntimeException("Query must not be null!");
    }
    if(collectionName == null){
      throw new RuntimeException("CollectionName must not be null!");
    }
    if(entityClass == null){
      throw new RuntimeException("EntityClass must not be null!");
    }
    query.addCriteria(Criteria.where("deletedAt").exists(Boolean.FALSE));

    return super.findOne(query, entityClass, collectionName);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public boolean exists(Query query, @Nullable Class<?> entityClass, String collectionName) {
    if (query == null) {
      throw new InvalidDataAccessApiUsageException("Query passed in to exist can't be null");
    }

    query.addCriteria(Criteria.where("deletedAt").exists(Boolean.FALSE));

    return super.exists(query, entityClass, collectionName);
  }

// You can also override ```delete()``` method, but I decided to not to do this

//  Maybe here should add other methods: count, findAndModify and ect. It depends which methods you going to use.
}