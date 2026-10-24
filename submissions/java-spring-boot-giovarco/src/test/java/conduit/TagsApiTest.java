package conduit;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagsApiTest extends ConduitApiTest {

    @Test
    void getTags_returnsSortedWithoutDuplicates() {
        RegisteredUser author = newUser();
        String tagOne = "tag-" + unique();
        String tagTwo = "tag-" + unique();
        createArticle(author, List.of(tagOne, tagTwo));

        ApiResponse response = get("/api/tags", null);

        assertThat(response.status()).isEqualTo(200);
        List<String> tags = jsonToList(response.body().path("tags"));
        assertThat(tags).isSorted();
        assertThat(tags).doesNotHaveDuplicates();
        assertThat(tags).contains(tagOne, tagTwo);
    }

    @Test
    void getTags_twice_returnsSameList() {
        RegisteredUser author = newUser();
        createArticle(author, List.of("tag-" + unique()));

        List<String> first = jsonToList(get("/api/tags", null).body().path("tags"));
        List<String> second = jsonToList(get("/api/tags", null).body().path("tags"));

        assertThat(second).isEqualTo(first);
    }
}
