package de.tostsoft.solarmonitoring.app.tag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.dtos.tags.AdminTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.CreateTagDTO;
import de.tostsoft.solarmonitoring.app.dtos.tags.TagDTO;
import de.tostsoft.solarmonitoring.lib.model.Tag;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TagTest extends AppBaseTest {

    @BeforeEach
    public void prepare() {
        clearDatabase();
    }

    @Test
    public void tagNameDuplicateCreate(){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color("#ffffff").locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void tagNameDuplicateEdit() throws JsonProcessingException {
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color("#ffffff").locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));
        dto.setName("test2");
        var ret = doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var adminTagDTO = objectMapper.readValue(ret.getBody(), AdminTagDTO.class);
        dto.setId(adminTagDTO.getId());
        dto.setName("test");
        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags", dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void editTagTest() throws JsonProcessingException {
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color("#ffffff").locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        var ret = doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        dto = objectMapper.readValue(ret.getBody(), CreateTagDTO.class);
        dto.setName("Test2");
        dto.setColor("#000000");
        dto.setLocked(true);
        dto.setShowOnStartPage(true);
        dto.setShowStartPageAggregation(true);
        CreateTagDTO finalDto = dto;
        doRestRequest("api/tags", finalDto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));

        var dbTag = tagRepository.findById(dto.getId()).get();
        Assertions.assertThat(dbTag.getName()).matches("test2");
        Assertions.assertThat(dbTag.getViewName()).matches("Test2");
        Assertions.assertThat(dbTag.getLocked()).isTrue();
        Assertions.assertThat(dbTag.getShowOnStartPage()).isTrue();
        Assertions.assertThat(dbTag.getShowStartPageAggregation()).isTrue();
        Assertions.assertThat(dbTag.getColor()).matches("#000000");
    }

    @Test
    public void addTag() throws JsonProcessingException {
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("Test").color("#FFffff").locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        var ret = doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));
        dto = objectMapper.readValue(ret.getBody(), CreateTagDTO.class);

        var dbTag = tagRepository.findById(dto.getId()).get();
        Assertions.assertThat(dbTag.getName()).matches("test");
        Assertions.assertThat(dbTag.getViewName()).matches("Test");
        Assertions.assertThat(dbTag.getLocked()).isFalse();
        Assertions.assertThat(dbTag.getShowOnStartPage()).isFalse();
        Assertions.assertThat(dbTag.getColor()).matches("#ffffff");
    }

    @ParameterizedTest
    @ValueSource(strings={"","aa","aaaaaaaaaaaaaaaaaaaaa","aaa,aaaa","aa "," aa"})
    public void checkNamingRegexNotWorking(String name){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name(name).color("#ffffff").locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings={"aaa","aaaaaaaaaaaaaaaaaaaa","a a","éßäüöÄÜÖ"})
    public void checkNamingRegexOk(String name){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name(name).color("#ffffff").locked(false).showOnStartPage(false).build();

       doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));
    }

    @ParameterizedTest
    @ValueSource(strings={"ffffff","#gggggg",""})
    public void checkColorRegexNotWorking(String name){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color(name).locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings={"#FFFFFF","#ffffff","#000000"})
    public void checkColorRegexOk(String name){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color(name).locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

       doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt));
    }

    @Test
    public void checkZeroValuesName(){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name(null).color("#ffffff").locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void checkZeroValuesColor(){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color(null).locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void checkZeroValuesLocked(){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color("#ffffff").locked(null).showOnStartPage(false).showStartPageAggregation(false).build();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void checkZeroValuesShowOnStartPage(){
        addUser(true);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color("#ffffff").locked(false).showOnStartPage(null).showStartPageAggregation(false).build();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void postTagAccessTest(){
        addUser(false);
        var jwt = signIn();

        var dto = CreateTagDTO.builder().name("test").color("#fffffff").locked(false).showOnStartPage(false).showStartPageAggregation(false).build();

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags",dto, HttpMethod.POST, Collections.singletonMap("Cookie","jwt="+jwt)));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void getAvailableTagsAccessTest() throws JsonProcessingException {
        addTag("test", "#fffffff", false);
        addTag("test2", "#fffffff", true);  // Locked tag - not accessible to non-admin
        addUser(false);
        var jwt = signIn();

        var res = doRestRequest("api/tags/available","", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        var tags = objectMapper.readValue(res.getBody(), new TypeReference<List<TagDTO>>(){});
        Assertions.assertThat(tags.size()).isEqualTo(1);
    }

    @Test
    public void getAvailableTagsAdminAccessTest() throws JsonProcessingException {
        addTag("test", "#fffffff", false);
        addTag("test2", "#fffffff", true);  // Locked tag - accessible to admin
        addUser(true);
        var jwt = signIn();

        var res = doRestRequest("api/tags/available","", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        var tags = objectMapper.readValue(res.getBody(), new TypeReference<List<TagDTO>>(){});
        Assertions.assertThat(tags.size()).isEqualTo(2);
    }

    @Test
    public void getAvailableTagsNoUser() throws JsonProcessingException {
        addTag("test", "#fffffff");
        addTag("test2", "#fffffff");

        var ex = assertThrows(HttpClientErrorException.class,()-> doRestRequest("api/tags/available"));
        Assertions.assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void getAllTagsNoUser() throws JsonProcessingException {
        addTag("test", "#fffffff",true);
        addTag("test2", "#ffffff0",false);

        var res = doRestRequest("api/tags");

        var tags = objectMapper.readValue(res.getBody(), new TypeReference<List<TagDTO>>(){});
        Assertions.assertThat(tags.size()).isEqualTo(2);

        Assertions.assertThat(tags.get(0).getName()).isEqualTo("test");
        Assertions.assertThat(tags.get(0).getColor()).isEqualTo("#fffffff");
        Assertions.assertThat(tags.get(1).getName()).isEqualTo("test2");
        Assertions.assertThat(tags.get(1).getColor()).isEqualTo("#ffffff0");

        var tagsAdmin = objectMapper.readValue(res.getBody(), new TypeReference<List<AdminTagDTO>>(){});

        //check ist not avalid admint dto (but parses) but lock and startpage are empty
        Assertions.assertThat(tagsAdmin.get(0).getLocked()).isNull();
        Assertions.assertThat(tagsAdmin.get(0).getShowOnStartPage()).isNull();
        Assertions.assertThat(tagsAdmin.get(1).getLocked()).isNull();
        Assertions.assertThat(tagsAdmin.get(1).getShowOnStartPage()).isNull();
    }

    @Test
    public void getAllTags() throws JsonProcessingException {
        addTag("test", "#fffffff");
        addTag("test2", "#fffffff");

        addUser(true);
        var jwt = signIn();

        var res = doRestRequest("api/tags","", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        var tags = objectMapper.readValue(res.getBody(), new TypeReference<List<AdminTagDTO>>(){});
        Assertions.assertThat(tags.size()).isEqualTo(2);
    }

    @Test
    public void getTagsById() throws JsonProcessingException {
        addTag("test", "#fffffff");
        var t1 = addTag("test2", "#fffffff");
        var t2 = tagRepository.save(Tag.builder().name("test3").color("#fffffff").viewName("test3").locked(true).showOnStartPage(false).build());

        var res = doRestRequest("api/tags/byIds?ids="+t1.getId()+","+t2.getId()+",NOT_AN_ID","", HttpMethod.GET);

        var tags = objectMapper.readValue(res.getBody(), new TypeReference<List<AdminTagDTO>>(){});
        Assertions.assertThat(tags.size()).isEqualTo(2);
    }

    @Test
    public void checkLockedTagsNoAdminUser() throws JsonProcessingException {
        addTag("test", "#fffffff", false);
        addTag("test2", "#fffffff", true);  // Locked tag - not accessible to non-admin

        var user = addUser(false);
        var jwt = signIn(user.getName());

        var res = doRestRequest("api/tags/available","", HttpMethod.GET, Collections.singletonMap("Cookie","jwt="+jwt));

        var tags = objectMapper.readValue(res.getBody(), new TypeReference<List<AdminTagDTO>>(){});
        Assertions.assertThat(tags.size()).isEqualTo(1);
        Assertions.assertThat(tags.get(0).getName()).isEqualTo("test");
    }
}
