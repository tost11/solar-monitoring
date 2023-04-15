package de.tostsoft.solarmonitoring.repository;

import de.tostsoft.solarmonitoring.model.User;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;

@Document("user-deleted")
public interface DeletedUserRepository extends MongoRepository<User,String> {
  User findByName(String name);

  List<User> findAllByNameStartingWith(String start);
}
