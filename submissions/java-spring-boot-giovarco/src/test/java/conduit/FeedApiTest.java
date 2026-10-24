package conduit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedApiTest extends ConduitApiTest {

    @Test
    void getFeed_returnsOnlyFollowedAuthorsNewestFirst() {
        RegisteredUser reader = newUser();
        RegisteredUser authorOne = newUser();
        RegisteredUser authorTwo = newUser();
        RegisteredUser outsider = newUser();
        follow(reader, authorOne);
        follow(reader, authorTwo);
        JsonNode articleTwo = createArticle(authorTwo);
        JsonNode articleOne = createArticle(authorOne);
        createArticle(outsider);

        ApiResponse response = get("/api/articles/feed", reader.token());

        assertThat(response.status()).isEqualTo(200);
        var articles = response.body().path("articles");
        assertThat(response.body().path("articlesCount").asInt()).isEqualTo(2);
        assertThat(articleIds(articles)).containsExactly(articleId(articleOne), articleId(articleTwo));
        assertThat(articles.get(0).has("body")).isFalse();
        assertThat(articles.get(0).path("author").path("username").asText())
                .isEqualTo(authorOne.username());
    }

    @Test
    void getFeed_withLimitAndOffset_pagesNewestFirst() {
        RegisteredUser reader = newUser();
        RegisteredUser author = newUser();
        follow(reader, author);
        JsonNode newest = createArticle(author);
        JsonNode middle = createArticle(author);
        JsonNode oldest = createArticle(author);

        ApiResponse page = get("/api/articles/feed?limit=2&offset=1", reader.token());

        assertThat(page.status()).isEqualTo(200);
        assertThat(page.body().path("articlesCount").asInt()).isEqualTo(3);
        assertThat(articleIds(page.body().path("articles"))).containsExactly(articleId(middle), articleId(oldest));
        assertThat(articleId(newest)).isGreaterThan(articleId(middle));
    }

    @Test
    void getFeed_withoutFollows_returnsEmptyWithZeroCount() {
        RegisteredUser reader = newUser();

        ApiResponse response = get("/api/articles/feed", reader.token());

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("articles").size()).isZero();
        assertThat(response.body().path("articlesCount").asInt()).isZero();
    }
}
