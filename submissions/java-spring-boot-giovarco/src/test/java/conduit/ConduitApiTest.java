package conduit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.NullNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ConduitApiTest.H2Database.class)
abstract class ConduitApiTest {

    private static final ObjectMapper json = new ObjectMapper();

    private static final String JWT_SECRET = "multicore-server-challenge";

    @Autowired
    MockMvcTester tester;

    @DynamicPropertySource
    static void runtimeContract(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_URL", () -> "postgres://conduit:conduit@127.0.0.1:5432/conduit");
        registry.add("JWT_SECRET", () -> "multicore-server-challenge");
    }

    static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    ApiResponse get(String path, String token) {
        return exchange(HttpMethod.GET, path, token, null);
    }

    ApiResponse post(String path, String token, Object body) {
        return exchange(HttpMethod.POST, path, token, body);
    }

    ApiResponse put(String path, String token, Object body) {
        return exchange(HttpMethod.PUT, path, token, body);
    }

    ApiResponse delete(String path, String token, Object body) {
        return exchange(HttpMethod.DELETE, path, token, body);
    }

    private ApiResponse exchange(HttpMethod method, String path, String token, Object body) {
        var request = method.equals(HttpMethod.GET) ? tester.get()
                : method.equals(HttpMethod.POST) ? tester.post()
                : method.equals(HttpMethod.PUT) ? tester.put()
                : tester.delete();
        request = request.uri(path);
        if (token != null) {
            request = request.header("Authorization", "Token " + token);
        }
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(jsonString(body));
        }
        return read(path, request.exchange());
    }

    private ApiResponse read(String path, MvcTestResult result) {
        var response = result.getMvcResult().getResponse();
        byte[] content = response.getContentAsByteArray();
        try {
            JsonNode tree = content.length == 0 ? NullNode.instance : json.readTree(content);
            return new ApiResponse(response.getStatus(), tree);
        } catch (IOException ex) {
            throw new UncheckedIOException("cannot parse response of " + path, ex);
        }
    }

    private String jsonString(Object body) {
        try {
            return json.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException("cannot serialize request body", ex);
        }
    }

    record ApiResponse(int status, JsonNode body) {

        boolean is(int expected) {
            return status == expected;
        }

        String error() {
            return body.path("errors").path("body").get(0).asText();
        }
    }

    record RegisteredUser(String username, String email, String password, String token) {
    }

    RegisteredUser newUser() {
        String unique = unique();
        RegisteredUser draft = new RegisteredUser("user-" + unique, unique + "@example.com", "secret-" + unique, null);
        ApiResponse response = post("/api/users", null, Map.of("user", Map.of(
                "username", draft.username(),
                "email", draft.email(),
                "password", draft.password())));
        if (!response.is(201)) {
            throw new IllegalStateException("registering " + draft.email() + " returned " + response.status());
        }
        return new RegisteredUser(draft.username(), draft.email(), draft.password(),
                response.body().path("user").path("token").asText());
    }

    void follow(RegisteredUser follower, RegisteredUser followee) {
        ApiResponse response = post("/api/profiles/" + followee.username() + "/follow", follower.token(), Map.of());
        if (!response.is(200)) {
            throw new IllegalStateException("following " + followee.username() + " returned " + response.status());
        }
    }

    JsonNode createArticle(RegisteredUser author) {
        return createArticle(author, null);
    }

    JsonNode createArticle(RegisteredUser author, List<String> tags) {
        String unique = unique();
        return createArticle(author, "Title " + unique, "Description " + unique, "Body " + unique, tags);
    }

    JsonNode createArticle(RegisteredUser author, String title, String description, String body,
            List<String> tags) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", title);
        fields.put("description", description);
        fields.put("body", body);
        if (tags != null) {
            fields.put("tagList", tags);
        }
        ApiResponse response = post("/api/articles", author.token(), Map.of("article", fields));
        if (!response.is(201)) {
            throw new IllegalStateException("creating article returned " + response.status()
                    + System.lineSeparator() + response.body());
        }
        return response.body().path("article");
    }

    long articleId(JsonNode article) {
        String slug = article.path("slug").asText();
        return Long.parseLong(slug.substring(slug.lastIndexOf('-') + 1));
    }

    List<Long> articleIds(JsonNode articles) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode article : articles) {
            ids.add(articleId(article));
        }
        return ids;
    }

    void assertThatIdsAreNewestFirst(JsonNode articles) {
        assertThat(articleIds(articles)).isSortedAccordingTo(Comparator.reverseOrder());
    }

    void assertThatTimestampFormat(JsonNode value) {
        assertThat(value.asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
    }

    List<String> jsonToList(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : array) {
            values.add(value.asText());
        }
        return values;
    }

    @Configuration(proxyBeanMethods = false)
    static class H2Database {

        @Bean
        @Primary
        DataSource h2DataSource() {
            var dataSource = new org.h2.jdbcx.JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:conduit;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection,
                        new EncodedResource(new ClassPathResource("schema-h2.sql")));
            } catch (Exception ex) {
                throw new IllegalStateException("cannot initialize the H2 schema", ex);
            }
            return dataSource;
        }
    }
}
