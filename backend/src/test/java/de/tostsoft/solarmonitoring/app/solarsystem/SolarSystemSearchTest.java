package de.tostsoft.solarmonitoring.app.solarsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemListItemDTO;
import de.tostsoft.solarmonitoring.app.dtos.solarsystem.SolarSystemSearchDTO;
import de.tostsoft.solarmonitoring.lib.dto.PagedResponse;
import de.tostsoft.solarmonitoring.lib.model.Manages;
import de.tostsoft.solarmonitoring.lib.model.Permissions;
import de.tostsoft.solarmonitoring.lib.model.SolarSystem;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolarSystemSearchTest  extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    private PagedResponse<SolarSystemListItemDTO> searchSystems(SolarSystemSearchDTO searchDTO) throws JsonProcessingException {
        var ret = doRestRequest("api/system/search", searchDTO, HttpMethod.POST);
        return objectMapper.readValue(ret.getBody(), new TypeReference<PagedResponse<SolarSystemListItemDTO>>(){});
    }

    @Test
    public void emtpySearchDTOTest() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"system1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        var ret = doRestRequest("api/system/search","{}", HttpMethod.POST);

        var pagedResponse = objectMapper.readValue(ret.getBody(), new TypeReference<PagedResponse<SolarSystemListItemDTO>>(){});
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
    }


    @Test
    public void PublicTrueSearchDTOTest() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"system1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @Test
    public void PublicFalseSearchDTOTest() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"system1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        //this is still 1 one because user is not signed in so something have to be returned
        Assertions.assertThat(list).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(SolarSystemType.class)
    public void TypeSearchTest(SolarSystemType type) throws JsonProcessingException {
        var user = addUser(true);

        SolarSystem system = null;

        for(var typeToAdd : SolarSystemType.values()){
            var sys = addSolarSystemForUser(user, typeToAdd,"system-"+typeToAdd);
            sys.setPublicMode(PublicMode.ALL);
            sys = solarSystemRepository.save(sys);
            if(typeToAdd == type){
                system = sys;
            }
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setType(type);

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getType()).isEqualTo(type);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"est","test","this-test-system","this","system","TEST","THIS-TEST-System","s-t"})
    public void checkNameTest(String searchTerm) throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"NOT_CONTAINING_SEARCH_TERM");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"this-test-system");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        system = solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setName(searchTerm);

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {".*test.*","\\Qtest\\E"})//check if regex is escaped
    public void checkNameNotFoundTest(String searchTerm) throws JsonProcessingException {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"this-test-system");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setName(searchTerm);

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"","e","es"})//check if regex is escaped
    public void checkNameNotValidTest(String searchTerm) throws JsonProcessingException {
        var user = addUser(true);
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"this-test-system");
        system = solarSystemRepository.findById(system.getId()).get();
        system.setPublicMode(PublicMode.PRODUCTION);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setName(searchTerm);

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/search",searchDTO, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void checkTagsInvalidTagId() throws JsonProcessingException {
        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setTags(Collections.singletonList("NOT_VALID_TAG_ID"));

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/system/search",searchDTO, HttpMethod.POST));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void checkTagsTest() throws JsonProcessingException {
        var user = addUser(true);
        addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"test2");

        var tag = addTag("test", "#ffffff");

        system.setTags(Collections.singletonList(tag));
        system.setPublicMode(PublicMode.ALL);
        system = solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setTags(Collections.singletonList(tag.getId()));

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @Test
    public void checkTwoTagsTest() throws JsonProcessingException {
        var user = addUser(true);

        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        var tag1 = addTag("test1", "#ffffff");
        system.setTags(Collections.singletonList(tag1));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        system = addSolarSystemForUser(user, SolarSystemType.GRID,"test2");
        var tag2 = addTag("test2", "#ffffff");
        system.setTags(Collections.singletonList(tag1));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setTags(Arrays.asList(tag1.getId(), tag2.getId()));

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(2);
    }

    @Test
    public void checkTwoTagsSameSystemReturnDestictTest() throws JsonProcessingException {
        var user = addUser(true);

        var system = addSolarSystemForUser(user, SolarSystemType.GRID,"test1");
        var tag1 = addTag("test1", "#ffffff");
        var tag2 = addTag("test2", "#ffffff");

        system.setTags(Arrays.asList(tag1,tag2));
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setTags(Arrays.asList(tag1.getId(), tag2.getId()));

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
    }


    @Test
    public void permissionOwnsTest() throws JsonProcessingException {
        var user1 = addUser(true,"user1");
        var user2 = addUser(true,"user2");

        addSolarSystemForUser(user1, SolarSystemType.GRID,"test1");
        var system = addSolarSystemForUser(user2, SolarSystemType.GRID,"test2");

        var jwt = signIn("user2");

        var ret = doRestRequest("api/system/search","{}", HttpMethod.POST,Collections.singletonMap("Cookie","jwt="+jwt));

        var pagedResponse = objectMapper.readValue(ret.getBody(), new TypeReference<PagedResponse<SolarSystemListItemDTO>>(){});
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @Test
    public void permissionOwnsTestAndPublic() throws JsonProcessingException {
        var user1 = addUser(true,"user1");
        var user2 = addUser(true,"user2");

        addSolarSystemForUser(user1, SolarSystemType.GRID,"test1");
        var system = addSolarSystemForUser(user2, SolarSystemType.GRID,"test2");
        system.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);

        var jwt = signIn("user1");

        var ret = doRestRequest("api/system/search",searchDTO, HttpMethod.POST,Collections.singletonMap("Cookie","jwt="+jwt));

        var pagedResponse = objectMapper.readValue(ret.getBody(), new TypeReference<PagedResponse<SolarSystemListItemDTO>>(){});
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(Permissions.class)
    public void permissonManagesTest(Permissions permission) throws JsonProcessingException {
        addUser(true,"user1");
        var user2 = addUser(true,"user2");

        var system = addSolarSystemForUser(user2, SolarSystemType.GRID,"test");
        var manages = Manages.builder().solarSystem(system).user(user2).permission(permission).build();
        managesRepository.save(manages);

        var jwt = signIn("user2");

        var ret = doRestRequest("api/system/search","{}", HttpMethod.POST,Collections.singletonMap("Cookie","jwt="+jwt));

        var pagedResponse = objectMapper.readValue(ret.getBody(), new TypeReference<PagedResponse<SolarSystemListItemDTO>>(){});
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }

    @ParameterizedTest
    @EnumSource(Permissions.class)
    public void permissonManagesAndPublicDesctinctTest(Permissions permission) throws JsonProcessingException {
        addUser(true,"user1");
        var user2 = addUser(true,"user2");

        var system = addSolarSystemForUser(user2, SolarSystemType.GRID,"test");
        system.setPublicMode(PublicMode.ALL);
        system = solarSystemRepository.save(system);
        var manages = Manages.builder().solarSystem(system).user(user2).permission(permission).build();
        managesRepository.save(manages);

        var solarSystemSearchDTO = new SolarSystemSearchDTO();
        solarSystemSearchDTO.setIsPublic(true);

        var jwt = signIn("user2");

        var ret = doRestRequest("api/system/search",solarSystemSearchDTO, HttpMethod.POST,Collections.singletonMap("Cookie","jwt="+jwt));

        var pagedResponse = objectMapper.readValue(ret.getBody(), new TypeReference<PagedResponse<SolarSystemListItemDTO>>(){});
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system.getId());
    }


    @Test
    public void DeletedSystemTest() throws JsonProcessingException {
        var user = addUser(true);
        var system1 = addSolarSystemForUser(user, SolarSystemType.GRID,"system1");
        var system2 = addSolarSystemForUser(user, SolarSystemType.GRID,"system2");
        system2.setDeletedAt(LocalDateTime.now(ZoneId.of("UTC")));
        system2.setPublicMode(PublicMode.ALL);
        system2 = solarSystemRepository.save(system2);
        system1.setPublicMode(PublicMode.ALL);
        system1 = solarSystemRepository.save(system1);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list).hasSize(1);
        Assertions.assertThat(list.get(0).getId()).isEqualTo(system1.getId());
    }

    // ===== Pagination feature tests =====

    @Test
    public void testDefaultPagination() throws JsonProcessingException {
        var user = addUser(true);
        for (int i = 0; i < 20; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "system" + i);
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);

        var pagedResponse = searchSystems(searchDTO);

        Assertions.assertThat(pagedResponse.getPage()).isEqualTo(0);
        Assertions.assertThat(pagedResponse.getSize()).isEqualTo(15);
        Assertions.assertThat(pagedResponse.getTotalElements()).isEqualTo(20);
        Assertions.assertThat(pagedResponse.getTotalPages()).isEqualTo(2);
        Assertions.assertThat(pagedResponse.getContent()).hasSize(15);
    }

    @Test
    public void testCustomPageSize() throws JsonProcessingException {
        var user = addUser(true);
        for (int i = 0; i < 25; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "system" + i);
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);
        searchDTO.setPage(0);
        searchDTO.setSize(10);

        var pagedResponse = searchSystems(searchDTO);

        Assertions.assertThat(pagedResponse.getPage()).isEqualTo(0);
        Assertions.assertThat(pagedResponse.getSize()).isEqualTo(10);
        Assertions.assertThat(pagedResponse.getTotalElements()).isEqualTo(25);
        Assertions.assertThat(pagedResponse.getTotalPages()).isEqualTo(3);
        Assertions.assertThat(pagedResponse.getContent()).hasSize(10);
    }

    @Test
    public void testSecondPage() throws JsonProcessingException {
        var user = addUser(true);
        for (int i = 0; i < 25; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "system" + String.format("%03d", i));
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);
        searchDTO.setPage(1);
        searchDTO.setSize(10);

        var pagedResponse = searchSystems(searchDTO);

        Assertions.assertThat(pagedResponse.getPage()).isEqualTo(1);
        Assertions.assertThat(pagedResponse.getSize()).isEqualTo(10);
        Assertions.assertThat(pagedResponse.getTotalElements()).isEqualTo(25);
        Assertions.assertThat(pagedResponse.getTotalPages()).isEqualTo(3);
        Assertions.assertThat(pagedResponse.getContent()).hasSize(10);
    }

    @Test
    public void testPageBeyondTotal() throws JsonProcessingException {
        var user = addUser(true);
        for (int i = 0; i < 5; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "system" + i);
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);
        searchDTO.setPage(10);
        searchDTO.setSize(15);

        var pagedResponse = searchSystems(searchDTO);

        Assertions.assertThat(pagedResponse.getPage()).isEqualTo(10);
        Assertions.assertThat(pagedResponse.getSize()).isEqualTo(15);
        Assertions.assertThat(pagedResponse.getTotalElements()).isEqualTo(5);
        Assertions.assertThat(pagedResponse.getTotalPages()).isEqualTo(1);
        Assertions.assertThat(pagedResponse.getContent()).hasSize(0);
    }

    @Test
    public void testPaginationMetadataAccuracy() throws JsonProcessingException {
        var user = addUser(true);
        for (int i = 0; i < 47; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "system" + i);
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);
        searchDTO.setPage(0);
        searchDTO.setSize(15);

        var pagedResponse = searchSystems(searchDTO);

        Assertions.assertThat(pagedResponse.getTotalElements()).isEqualTo(47);
        Assertions.assertThat(pagedResponse.getTotalPages()).isEqualTo(4);
    }

    // ===== Sorting feature tests =====

    @Test
    public void testSortByNameAsc() throws JsonProcessingException {
        var user = addUser(true);
        var zebra = addSolarSystemForUser(user, SolarSystemType.GRID, "Zebra");
        zebra.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(zebra);

        var alpha = addSolarSystemForUser(user, SolarSystemType.GRID, "Alpha");
        alpha.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(alpha);

        var beta = addSolarSystemForUser(user, SolarSystemType.GRID, "Beta");
        beta.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(beta);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);
        searchDTO.setSortBy("name");
        searchDTO.setSortOrder("asc");

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list)
            .extracting(SolarSystemListItemDTO::getName)
            .containsExactly("Alpha", "Beta", "Zebra");
    }

    @Test
    public void testSortByNameDesc() throws JsonProcessingException {
        var user = addUser(true);
        var zebra = addSolarSystemForUser(user, SolarSystemType.GRID, "Zebra");
        zebra.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(zebra);

        var alpha = addSolarSystemForUser(user, SolarSystemType.GRID, "Alpha");
        alpha.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(alpha);

        var beta = addSolarSystemForUser(user, SolarSystemType.GRID, "Beta");
        beta.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(beta);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);
        searchDTO.setSortBy("name");
        searchDTO.setSortOrder("desc");

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        Assertions.assertThat(list)
            .extracting(SolarSystemListItemDTO::getName)
            .containsExactly("Zebra", "Beta", "Alpha");
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "desc"})
    public void testSortByCreationDate(String sortOrder) throws JsonProcessingException {
        var user = addUser(true);

        var system1 = addSolarSystemForUser(user, SolarSystemType.GRID, "System1");
        system1.setCreationDate(LocalDateTime.of(2024, 1, 1, 0, 0));
        system1.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system1);

        var system2 = addSolarSystemForUser(user, SolarSystemType.GRID, "System2");
        system2.setCreationDate(LocalDateTime.of(2024, 6, 1, 0, 0));
        system2.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system2);

        var system3 = addSolarSystemForUser(user, SolarSystemType.GRID, "System3");
        system3.setCreationDate(LocalDateTime.of(2024, 3, 1, 0, 0));
        system3.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system3);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);
        searchDTO.setSortBy("creationDate");
        searchDTO.setSortOrder(sortOrder);

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        if ("asc".equals(sortOrder)) {
            Assertions.assertThat(list)
                .extracting(SolarSystemListItemDTO::getName)
                .containsExactly("System1", "System3", "System2");
        } else {
            Assertions.assertThat(list)
                .extracting(SolarSystemListItemDTO::getName)
                .containsExactly("System2", "System3", "System1");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "desc"})
    public void testSortByBuildingDate(String sortOrder) throws JsonProcessingException {
        var user = addUser(true);

        var system1 = addSolarSystemForUser(user, SolarSystemType.GRID, "System1");
        system1.getSystemInformations().setBuildingDate(LocalDateTime.of(2023, 12, 1, 0, 0));
        system1.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system1);

        var system2 = addSolarSystemForUser(user, SolarSystemType.GRID, "System2");
        system2.getSystemInformations().setBuildingDate(LocalDateTime.of(2024, 5, 1, 0, 0));
        system2.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system2);

        var system3 = addSolarSystemForUser(user, SolarSystemType.GRID, "System3");
        system3.getSystemInformations().setBuildingDate(LocalDateTime.of(2024, 2, 1, 0, 0));
        system3.setPublicMode(PublicMode.ALL);
        solarSystemRepository.save(system3);

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setIsPublic(true);
        searchDTO.setSortBy("buildingDate");
        searchDTO.setSortOrder(sortOrder);

        var pagedResponse = searchSystems(searchDTO);
        var list = pagedResponse.getContent();

        if ("asc".equals(sortOrder)) {
            Assertions.assertThat(list)
                .extracting(SolarSystemListItemDTO::getName)
                .containsExactly("System1", "System3", "System2");
        } else {
            Assertions.assertThat(list)
                .extracting(SolarSystemListItemDTO::getName)
                .containsExactly("System2", "System3", "System1");
        }
    }

    @Test
    public void testInvalidSortField() {
        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setSortBy("password");

        var ex = assertThrows(HttpClientErrorException.class, () -> searchSystems(searchDTO));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(ex.getMessage()).contains("Invalid sort field");
    }

    @Test
    public void testPaginationWithFilters() throws JsonProcessingException {
        var user = addUser(true);
        for (int i = 0; i < 15; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "solar" + i);
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }
        for (int i = 0; i < 15; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "other" + i);
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setName("solar");
        searchDTO.setIsPublic(true);
        searchDTO.setPage(0);
        searchDTO.setSize(10);

        var pagedResponse = searchSystems(searchDTO);

        Assertions.assertThat(pagedResponse.getTotalElements()).isEqualTo(15);
        Assertions.assertThat(pagedResponse.getTotalPages()).isEqualTo(2);
        Assertions.assertThat(pagedResponse.getContent()).hasSize(10);

        searchDTO.setPage(1);
        pagedResponse = searchSystems(searchDTO);

        Assertions.assertThat(pagedResponse.getTotalElements()).isEqualTo(15);
        Assertions.assertThat(pagedResponse.getTotalPages()).isEqualTo(2);
        Assertions.assertThat(pagedResponse.getContent()).hasSize(5);
    }

    @Test
    public void testPaginationWithTagFilter() throws JsonProcessingException {
        var user = addUser(true);
        var tag1 = addTag("tag1", "#ffffff");

        for (int i = 0; i < 8; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "tagged" + i);
            system.setTags(Collections.singletonList(tag1));
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }
        for (int i = 0; i < 12; i++) {
            var system = addSolarSystemForUser(user, SolarSystemType.GRID, "untagged" + i);
            system.setPublicMode(PublicMode.ALL);
            solarSystemRepository.save(system);
        }

        SolarSystemSearchDTO searchDTO = new SolarSystemSearchDTO();
        searchDTO.setTags(Collections.singletonList(tag1.getId()));
        searchDTO.setIsPublic(true);
        searchDTO.setPage(0);
        searchDTO.setSize(5);

        var pagedResponse = searchSystems(searchDTO);

        Assertions.assertThat(pagedResponse.getTotalElements()).isEqualTo(8);
        Assertions.assertThat(pagedResponse.getTotalPages()).isEqualTo(2);
        Assertions.assertThat(pagedResponse.getContent()).hasSize(5);
    }


}
