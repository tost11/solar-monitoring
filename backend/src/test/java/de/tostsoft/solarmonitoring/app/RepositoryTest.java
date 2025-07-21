package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RepositoryTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    void checkUsersQueriesDeleteAtWorking(){
        var user = addUser(true);

        var opt = userRepository.findById(user.getId());
        assertThat(opt).isNotEmpty();

        var all = userRepository.findAll();
        assertThat(all.size()).isEqualTo(1);

        var found = userRepository.findOneByNameOrMail(user.getName(),"NOT_VALID");
        assertThat(found).isNotNull();

        found = userRepository.findOneByNameOrMail("NOT_VALID",user.getMail());
        assertThat(found).isNotNull();

        found = userRepository.findByName(user.getName());
        assertThat(found).isNotNull();

        opt = userRepository.findById(user.getId());
        assertThat(opt).isNotEmpty();

        all = userRepository.findAllByNameStartingWith("test");
        assertThat(all.size()).isEqualTo(1);

        //---------------- set deleted ----------------------
        user.setDeletedAt(LocalDateTime.now(ZoneId.of("UTC")));
        user = userRepository.save(user);

        opt = userRepository.findById(user.getId());
        assertThat(opt).isEmpty();

        all = userRepository.findAll();
        assertThat(all.size()).isEqualTo(0);

        opt = userRepository.findByIdWithDeleted(user.getId());
        assertThat(opt).isNotEmpty();

        all = userRepository.findAllByNameStartingWithWithDeleted("test");
        assertThat(all.size()).isEqualTo(1);

        opt = userRepository.findByIdWithDeleted(user.getId());
        assertThat(opt).isNotEmpty();

        opt = userRepository.findByInfluxBucketNameWithDeleted(user.getInfluxBucketName());
        assertThat(opt).isNotEmpty();

        all = userRepository.findAllWithDeleted();
        assertThat(all.size()).isEqualTo(1);

        var count = userRepository.countByIdAndIsAdminWithDeleted(user.getId(),true);
        assertThat(count).isEqualTo(1);

        count = userRepository.countByNameWithDeleted(user.getName());
        assertThat(count).isEqualTo(1);

        count = userRepository.countByMailWithDeleted(user.getMail());
        assertThat(count).isEqualTo(1);

        //all not avialble
        opt = userRepository.findById(user.getId());
        assertThat(opt).isEmpty();

        all = userRepository.findAllWithDeleted();
        assertThat(all.size()).isEqualTo(1);

        found = userRepository.findOneByNameOrMail(user.getName(),"NOT_VALID");
        assertThat(found).isNull();

        found = userRepository.findOneByNameOrMail("NOT_VALID",user.getMail());
        assertThat(found).isNull();

        found = userRepository.findByName(user.getName());
        assertThat(found).isNull();

        opt = userRepository.findById(user.getId());
        assertThat(opt).isEmpty();

        all = userRepository.findAllByNameStartingWith("test");
        assertThat(all.size()).isEqualTo(0);

    }

    @Test
    void checkSolarSystemsQueriesDeleteAtWorking(){
        var user = addUser(true,"test");

        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"test");

        var tag = new Tag();
        tag.setColor("#FFFFFF");
        tag.setName("test");
        tag.setLocked(false);
        tag.setViewName("test");
        tag.setShowOnStartPage(true);
        tag = tagRepository.save(tag);

        system.setShortener("sys");
        system.setDeyeSunSerials(Collections.singleton(1234L));
        system.setNeedsStatisticRecalculation(true);
        system.setPublicMode(PublicMode.ALL);
        system.setLastCalculation(null);
        system.setTags(Collections.singletonList(tag));

        system = solarSystemRepository.save(system);

        var opt = solarSystemRepository.findById(system.getId());
        assertThat(opt).isNotEmpty();

        var all = solarSystemRepository.findAll();
        assertThat(all.size()).isEqualTo(1);

        all = solarSystemRepository.findAllByIdOrShortenerIn(Collections.singleton("sys"));
        assertThat(all.size()).isEqualTo(1);

        all = solarSystemRepository.findAllByIdOrShortenerIn(Collections.singleton(system.getId()));
        assertThat(all.size()).isEqualTo(1);

        all = solarSystemRepository.findAllByIdOrShortener("sys");
        assertThat(all.size()).isEqualTo(1);

        all = solarSystemRepository.findAllByIdOrShortener(system.getId());
        assertThat(all.size()).isEqualTo(1);

        opt = solarSystemRepository.findSolarSystemBySerialInAndDeyeSunSerials(1234L);
        assertThat(opt).isNotEmpty();

        all = solarSystemRepository.findAllByNeedsStatisticRecalculation(true);
        assertThat(all.size()).isEqualTo(1);

        var exists = solarSystemRepository.existsByShortener("sys");
        assertThat(exists).isEqualTo(true);

        all = solarSystemRepository.findAllByPublicMode(PublicMode.ALL);
        assertThat(all.size()).isEqualTo(1);

        all = solarSystemRepository.findAllByPublicModeIsNot(PublicMode.PRODUCTION);
        assertThat(all.size()).isEqualTo(1);

        all = solarSystemRepository.findAllByLastCalculationIsNull();
        assertThat(all.size()).isEqualTo(1);

        system.setLastCalculation(Instant.now().toEpochMilli());
        system =  solarSystemRepository.save(system);

        all = solarSystemRepository.findAllByLastCalculationIsLessThan(Instant.now().toEpochMilli());
        assertThat(all.size()).isEqualTo(1);

        opt = solarSystemRepository.findByIdAndOwnedById(system.getId(),user.getId());
        assertThat(opt).isNotEmpty();

        opt = solarSystemRepository.findByInfluxTagName(system.getInfluxTagName());
        assertThat(opt).isNotEmpty();

        all = solarSystemRepository.findByTypeAndOwnedById(system.getType(),user.getId());
        assertThat(all.size()).isEqualTo(1);

        all = solarSystemRepository.findAllByTagsContains(tag.getId());
        assertThat(all.size()).isEqualTo(1);

        all = solarSystemRepository.findAllByTagsContainsAndPublicModeIsNot(tag.getId(),PublicMode.PRODUCTION);
        assertThat(all.size()).isEqualTo(1);

        //---------------- set deleted ----------------------
        system.setDeletedAt(LocalDateTime.now(ZoneId.of("UTC")));
        system = solarSystemRepository.save(system);

        all = solarSystemRepository.findAll();
        assertThat(all.size()).isEqualTo(0);

        opt = solarSystemRepository.findById(system.getId());
        assertThat(opt).isEmpty();

        all = solarSystemRepository.findAllWithDeleted();
        assertThat(all.size()).isEqualTo(1);

        opt = solarSystemRepository.findByIdWithDeleted(system.getId());
        assertThat(opt).isNotEmpty();

        all = solarSystemRepository.findAllByIdOrShortenerIn(Collections.singleton("sys"));
        assertThat(all.size()).isEqualTo(0);

        all = solarSystemRepository.findAllByIdOrShortenerIn(Collections.singleton(system.getId()));
        assertThat(all.size()).isEqualTo(0);

        all = solarSystemRepository.findAllByIdOrShortener("sys");
        assertThat(all.size()).isEqualTo(0);

        all = solarSystemRepository.findAllByIdOrShortener(system.getId());
        assertThat(all.size()).isEqualTo(0);

        opt = solarSystemRepository.findSolarSystemBySerialInAndDeyeSunSerials(1234L);
        assertThat(opt).isEmpty();

        all = solarSystemRepository.findAllByNeedsStatisticRecalculation(true);
        assertThat(all.size()).isEqualTo(0);

        exists = solarSystemRepository.existsByShortener("sys");
        assertThat(exists).isEqualTo(false);

        all = solarSystemRepository.findAllByPublicMode(PublicMode.ALL);
        assertThat(all.size()).isEqualTo(0);

        all = solarSystemRepository.findAllByPublicModeIsNot(PublicMode.PRODUCTION);
        assertThat(all.size()).isEqualTo(0);

        all = solarSystemRepository.findAllByLastCalculationIsNull();
        assertThat(all.size()).isEqualTo(0);

        system.setLastCalculation(Instant.now().toEpochMilli());
        system =  solarSystemRepository.save(system);

        all = solarSystemRepository.findAllByLastCalculationIsLessThan(Instant.now().toEpochMilli());
        assertThat(all.size()).isEqualTo(0);

        opt = solarSystemRepository.findByIdAndOwnedById(system.getId(),user.getId());
        assertThat(opt).isEmpty();

        opt = solarSystemRepository.findByInfluxTagName(system.getInfluxTagName());
        assertThat(opt).isEmpty();

        all = solarSystemRepository.findByTypeAndOwnedById(system.getType(),user.getId());
        assertThat(all.size()).isEqualTo(0);

        all = solarSystemRepository.findAllByTagsContainsAndPublicModeIsNot(tag.getId(),PublicMode.PRODUCTION);
        assertThat(all.size()).isEqualTo(0);

        all = solarSystemRepository.findAllByTagsContains(tag.getId());
        assertThat(all.size()).isEqualTo(0);
    }
}
