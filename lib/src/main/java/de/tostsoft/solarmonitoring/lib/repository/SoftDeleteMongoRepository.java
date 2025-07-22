package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Validated
@NoRepositoryBean
public interface SoftDeleteMongoRepository<T1,T2> extends MongoRepository<T1,T2> {

    //---------- general funkction
    @NotNull
    @Override
    @Query("{ 'deletedAt': null}")
    List<T1> findAll();

    @NotNull
    @Query("{}")
    List<T1> findAllWithDeleted();

    @NotNull
    @Query(value = "{$and:[{'_id':?0},{'deletedAt': null}]}")
    @Override
    Optional<T1> findById(@NotNull T2 id);

    @Query(value = "{'_id':?0}")
    Optional<T1> findByIdWithDeleted(@NotNull T2 id);

    @NotNull
    @Query(value = "{'deletedAt': null}",count = true)
    @Override
    long count();

    @Query(value = "{}",count = true)
    long countWithDeleted();

    @Query("{ '_id': ?0}")
    @Update("{ '$set' : { 'deletedAt' : ?1 } }")
    void updateDeletedAt(@NotNull T2 id,LocalDateTime deletedAt);
}
