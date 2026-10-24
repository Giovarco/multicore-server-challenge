package conduit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleListApiTest extends ConduitApiTest {

    @Test
    void listArticles_byDefault_returns20NewestFirst() {
        RegisteredUser author = newUser();
        for (int i = 0; i < 25; i++) {
            createArticle(author);
        }

        ApiResponse response = get("/api/articles", null);

        assertThat(response.status()).isEqualTo(200);
        var articles = response.body().path("articles");
        assertThat(articles.size()).isEqualTo(20);
        assertThat(response.body().path("articlesCount").asInt()).isGreaterThanOrEqualTo(25);
        assertThatIdsAreNewestFirst(articles);
        assertThat(articles.get(0).has("body")).isFalse();
        assertThat(articles.get(0).has("description")).isTrue();
        assertThat(articles.get(0).path("favorited").asBoolean()).isFalse();
    }

    @Test
    void listArticles_withLimitOneAndHundred_returnsRespectively() {
        RegisteredUser author = newUser();
        createArticle(author);
        createArticle(author);
        createArticle(author);

        ApiResponse single = get("/api/articles?author=" + author.username() + "&limit=1", null);
        ApiResponse hundred = get("/api/articles?author=" + author.username() + "&limit=100", null);

        assertThat(single.status()).isEqualTo(200);
        assertThat(single.body().path("articles").size()).isEqualTo(1);
        assertThat(hundred.status()).isEqualTo(200);
        assertThat(hundred.body().path("articles").size()).isEqualTo(3);
        assertThatIdsAreNewestFirst(hundred.body().path("articles"));
    }

    @Test
    void listArticles_withLimitAndOffset_returnsDisjointPages() {
        RegisteredUser author = newUser();
        for (int i = 0; i < 7; i++) {
            createArticle(author);
        }
        String byAuthor = "/api/articles?author=" + author.username();

        JsonNode firstPage = get(byAuthor + "&limit=5&offset=0", null).body().path("articles");
        JsonNode secondPage = get(byAuthor + "&limit=5&offset=5", null).body().path("articles");

        List<Long> firstIds = articleIds(firstPage);
        List<Long> secondIds = articleIds(secondPage);

        assertThat(firstIds).hasSize(5);
        assertThat(secondIds).hasSize(2);
        assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
        assertThat(firstIds.getLast()).isGreaterThan(secondIds.getFirst());
        assertThatIdsAreNewestFirst(firstPage);
        assertThatIdsAreNewestFirst(secondPage);
    }

    @Test
    void listArticles_byTag_returnsOnlyArticlesWithThatTag() {
        RegisteredUser author = newUser();
        String tag = "tag-" + unique();
        JsonNode created = createArticle(author, List.of(tag));

        ApiResponse response = get("/api/articles?tag=" + tag, null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("articlesCount").asInt()).isEqualTo(1);
        assertThat(articleIds(response.body().path("articles"))).containsExactly(articleId(created));
    }

    @Test
    void listArticles_byAuthor_returnsOnlyThatAuthorsArticles() {
        RegisteredUser author = newUser();
        RegisteredUser other = newUser();
        JsonNode first = createArticle(author);
        JsonNode second = createArticle(author);
        createArticle(other);

        ApiResponse response = get("/api/articles?author=" + author.username(), null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("articlesCount").asInt()).isEqualTo(2);
        assertThat(articleIds(response.body().path("articles")))
                .containsExactly(articleId(first), articleId(second))
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void listArticles_byFavorited_returnsOnlyThatUsersFavorites() {
        RegisteredUser author = newUser();
        RegisteredUser fan = newUser();
        JsonNode favoritedArticle = createArticle(author);
        createArticle(author);

        assertThat(post("/api/articles/" + favoritedArticle.path("slug").asText() + "/favorite",
                fan.token(), null).status()).isEqualTo(200);

        ApiResponse response = get("/api/articles?favorited=" + fan.username(), null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("articlesCount").asInt()).isEqualTo(1);
        assertThat(articleIds(response.body().path("articles"))).containsExactly(articleId(favoritedArticle));
    }
}
