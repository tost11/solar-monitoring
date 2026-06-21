package de.tostsoft.solarmonitoring.app.tag;

import com.fasterxml.jackson.databind.JsonNode;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StartPageEndpointTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void testStartPageDataStructure() throws Exception {
        Tag tag1 = addTag("tag1", "#FF0000", false, true, true);
        Tag tag2 = addTag("tag2", "#00FF00", false, true, false);

        var user = addUser(false);
        var jwt = signIn(user.getName());

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);
        system.setTags(List.of(tag1, tag2));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        ResponseEntity<String> response = doRequest(
            "api/tags/startpage", HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt));

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = objectMapper.readTree(response.getBody());

        Assertions.assertThat(json.has("tagsWithSystems")).isTrue();
        Assertions.assertThat(json.has("aggregationTags")).isTrue();

        JsonNode tagsWithSystems = json.get("tagsWithSystems");
        Assertions.assertThat(tagsWithSystems.isArray()).isTrue();
        Assertions.assertThat(tagsWithSystems.size()).isEqualTo(2);

        JsonNode aggregationTags = json.get("aggregationTags");
        Assertions.assertThat(aggregationTags.isArray()).isTrue();
        Assertions.assertThat(aggregationTags.size()).isEqualTo(1);
        Assertions.assertThat(aggregationTags.get(0).get("id").asText()).isEqualTo(tag1.getId());
    }

    @Test
    public void testAggregationTagsFilteredCorrectly() throws Exception {
        Tag tagA = addTag("tagA", "#FF0000", false, true, true);
        Tag tagB = addTag("tagB", "#00FF00", false, true, false);
        Tag tagC = addTag("tagC", "#0000FF", false, false, true);
        Tag tagD = addTag("tagD", "#FFFF00", false, false, false);

        var user = addUser(false);
        var jwt = signIn(user.getName());

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);
        system.setTags(List.of(tagA, tagB, tagC, tagD));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        ResponseEntity<String> response = doRequest(
            "api/tags/startpage", HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt));

        JsonNode json = objectMapper.readTree(response.getBody());

        JsonNode tagsWithSystems = json.get("tagsWithSystems");
        Assertions.assertThat(tagsWithSystems.size()).isEqualTo(2);
        Set<String> systemTagIds = new HashSet<>();
        for (JsonNode node : tagsWithSystems) {
            systemTagIds.add(node.get("tag").get("id").asText());
        }
        Assertions.assertThat(systemTagIds).contains(tagA.getId(), tagB.getId());

        JsonNode aggregationTags = json.get("aggregationTags");
        Assertions.assertThat(aggregationTags.size()).isEqualTo(2);
        Set<String> aggTagIds = new HashSet<>();
        for (JsonNode node : aggregationTags) {
            aggTagIds.add(node.get("id").asText());
        }
        Assertions.assertThat(aggTagIds).contains(tagA.getId(), tagC.getId());
    }

    @Test
    public void testPublicUserOnlySeesPublicSystems() throws Exception {
        Tag tag = addTag("publicTag", "#FF0000", false, true, true);

        var user = addUser(false);
        var jwt = signIn(user.getName());

        var publicSystem = addSolarSystemForUser(user, SolarSystemType.GRID, "publicSystem");
        publicSystem.setTags(Collections.singletonList(tag));
        publicSystem.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(publicSystem);

        var privateSystem = addSolarSystemForUser(user, SolarSystemType.GRID, "privateSystem");
        privateSystem.setTags(Collections.singletonList(tag));
        privateSystem.setPublicMode(PublicMode.NONE);
        solarSystemRepository.save(privateSystem);

        ResponseEntity<String> response = doRestRequest("api/tags/startpage");

        JsonNode json = objectMapper.readTree(response.getBody());

        JsonNode tagsWithSystems = json.get("tagsWithSystems");
        Assertions.assertThat(tagsWithSystems.size()).isEqualTo(1);
        JsonNode systems = tagsWithSystems.get(0).get("systems");
        Assertions.assertThat(systems.size()).isEqualTo(1);
        Assertions.assertThat(systems.get(0).get("id").asText()).isEqualTo(publicSystem.getId());

        JsonNode aggregationTags = json.get("aggregationTags");
        Assertions.assertThat(aggregationTags.size()).isEqualTo(1);
    }

    @Test
    public void testEmptyResponseWhenNoTagsConfigured() throws Exception {
        addTag("tag1", "#FF0000", false, false, false);
        addTag("tag2", "#00FF00", false, false, false);

        var user = addUser(false);
        var jwt = signIn(user.getName());

        ResponseEntity<String> response = doRequest(
            "api/tags/startpage", HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt));

        JsonNode json = objectMapper.readTree(response.getBody());

        Assertions.assertThat(json.get("tagsWithSystems").size()).isEqualTo(0);
        Assertions.assertThat(json.get("aggregationTags").size()).isEqualTo(0);
    }

    @Test
    public void testNullShowStartPageAggregationTreatedAsFalse() throws Exception {
        Tag oldTag = Tag.builder()
            .name("oldtag")
            .viewName("oldTag")
            .color("#FF0000")
            .locked(false)
            .showOnStartPage(true)
            .showStartPageAggregation(null)
            .build();
        oldTag = tagRepository.save(oldTag);

        var user = addUser(false);
        var jwt = signIn(user.getName());

        var system = addSolarSystemForUser(user, SolarSystemType.GRID);
        system.setTags(Collections.singletonList(oldTag));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        ResponseEntity<String> response = doRequest(
            "api/tags/startpage", HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt));

        JsonNode json = objectMapper.readTree(response.getBody());

        Assertions.assertThat(json.get("tagsWithSystems").size()).isEqualTo(1);
        Assertions.assertThat(json.get("aggregationTags").size()).isEqualTo(0);
    }
}
