package conduit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleApiTest extends ConduitApiTest {

    @Test
    void createArticle_withoutTags_returns201WithSlugifiedSlug() {
        RegisteredUser author = newUser();
        String unique = unique();

        JsonNode article = createArticle(author, "Hello World " + unique, "first description",
                "first body", null);

        assertThat(article.path("title").asText()).isEqualTo("Hello World " + unique);
        assertThat(article.path("slug").asText()).matches("hello-world-" + unique + "-\\d+");
        assertThat(article.path("description").asText()).isEqualTo("first description");
        assertThat(article.path("body").asText()).isEqualTo("first body");
        assertThat(article.path("tagList").size()).isZero();
        assertThat(article.path("favorited").asBoolean()).isFalse();
        assertThat(article.path("favoritesCount").asInt()).isZero();
        assertThat(article.path("author").path("username").asText()).isEqualTo(author.username());
        assertThatTimestampFormat(article.path("createdAt"));
        assertThatTimestampFormat(article.path("updatedAt"));
    }

    @Test
    void createArticle_withTags_returns201WithSortedTagList() {
        RegisteredUser author = newUser();

        JsonNode article = createArticle(author, List.of("zulu", "alpha", "mike"));

        assertThat(jsonToList(article.path("tagList"))).containsExactly("alpha", "mike", "zulu");
    }

    @Test
    void getArticle_bySlug_returns200WithFullArticle() {
        RegisteredUser author = newUser();
        JsonNode created = createArticle(author, List.of("golf"));

        ApiResponse response = get("/api/articles/" + created.path("slug").asText(), null);

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("slug").asText()).isEqualTo(created.path("slug").asText());
        assertThat(article.path("title").asText()).isEqualTo(created.path("title").asText());
        assertThat(article.path("description").asText()).isEqualTo(created.path("description").asText());
        assertThat(article.path("body").asText()).isEqualTo(created.path("body").asText());
        assertThat(jsonToList(article.path("tagList"))).containsExactly("golf");
        assertThat(article.path("favorited").asBoolean()).isFalse();
        assertThat(article.path("favoritesCount").asInt()).isZero();
        assertThat(article.path("author").path("username").asText()).isEqualTo(author.username());
    }

    @Test
    void updateArticle_withAllFields_returns200WithNewTitleAndSlug() {
        RegisteredUser author = newUser();
        JsonNode created = createArticle(author, null);
        long id = articleId(created);
        String newTitle = "New Title " + unique();

        ApiResponse response = put("/api/articles/" + created.path("slug").asText(), author.token(),
                Map.of("article", Map.of("title", newTitle, "description", "new description", "body", "new body")));

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("title").asText()).isEqualTo(newTitle);
        assertThat(article.path("description").asText()).isEqualTo("new description");
        assertThat(article.path("body").asText()).isEqualTo("new body");
        assertThat(article.path("slug").asText()).isEqualTo("new-title-" + newTitle.split(" ")[2] + "-" + id);
        assertThat(article.path("createdAt").asText()).isEqualTo(created.path("createdAt").asText());
        assertThatTimestampFormat(article.path("updatedAt"));
    }

    @Test
    void updateArticle_withOnlyDescription_keepsTitleAndSlugAndBody() {
        RegisteredUser author = newUser();
        JsonNode created = createArticle(author, null);

        ApiResponse response = put("/api/articles/" + created.path("slug").asText(), author.token(),
                Map.of("article", Map.of("description", "changed description")));

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("description").asText()).isEqualTo("changed description");
        assertThat(article.path("title").asText()).isEqualTo(created.path("title").asText());
        assertThat(article.path("slug").asText()).isEqualTo(created.path("slug").asText());
        assertThat(article.path("body").asText()).isEqualTo(created.path("body").asText());
    }

    @Test
    void deleteArticle_returns204AndRemovesArticle() {
        RegisteredUser author = newUser();
        JsonNode created = createArticle(author, null);

        ApiResponse response = delete("/api/articles/" + created.path("slug").asText(), author.token(), null);

        assertThat(response.status()).isEqualTo(204);
        assertThat(get("/api/articles/" + created.path("slug").asText(), null).status()).isEqualTo(404);
    }
}
