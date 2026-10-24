package conduit;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FavoritesApiTest extends ConduitApiTest {

    private String slugOf(JsonNode article) {
        return article.path("slug").asText();
    }

    @Test
    void favoriteArticle_returns200WithFavoritedTrueAndCountOne() {
        RegisteredUser author = newUser();
        RegisteredUser fan = newUser();
        JsonNode created = createArticle(author);

        ApiResponse response = post("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("favorited").asBoolean()).isTrue();
        assertThat(article.path("favoritesCount").asInt()).isEqualTo(1);
    }

    @Test
    void favoriteArticle_twice_keepsCountAtOne() {
        RegisteredUser author = newUser();
        RegisteredUser fan = newUser();
        JsonNode created = createArticle(author);
        post("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);

        ApiResponse response = post("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("favorited").asBoolean()).isTrue();
        assertThat(article.path("favoritesCount").asInt()).isEqualTo(1);
    }

    @Test
    void favoriteArticle_keepsAuthorUnfavorited() {
        RegisteredUser author = newUser();
        RegisteredUser fan = newUser();
        JsonNode created = createArticle(author);
        post("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);

        ApiResponse response = get("/api/articles/" + slugOf(created), author.token());

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("favorited").asBoolean()).isFalse();
        assertThat(article.path("favoritesCount").asInt()).isEqualTo(1);
    }

    @Test
    void favoriteArticle_bySeveralUsers_countsEachUserOnce() {
        RegisteredUser author = newUser();
        RegisteredUser fanOne = newUser();
        RegisteredUser fanTwo = newUser();
        RegisteredUser fanThree = newUser();
        JsonNode created = createArticle(author);

        post("/api/articles/" + slugOf(created) + "/favorite", fanOne.token(), null);
        post("/api/articles/" + slugOf(created) + "/favorite", fanTwo.token(), null);
        ApiResponse response = post("/api/articles/" + slugOf(created) + "/favorite", fanThree.token(), null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("article").path("favoritesCount").asInt()).isEqualTo(3);
    }

    @Test
    void unfavoriteArticle_returns200WithFavoritedFalseAndCountZero() {
        RegisteredUser author = newUser();
        RegisteredUser fan = newUser();
        JsonNode created = createArticle(author);
        post("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);

        ApiResponse response = delete("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("favorited").asBoolean()).isFalse();
        assertThat(article.path("favoritesCount").asInt()).isZero();
    }

    @Test
    void unfavoriteArticle_notFavorited_returns200WithFavoritedFalse() {
        RegisteredUser author = newUser();
        RegisteredUser fan = newUser();
        JsonNode created = createArticle(author);

        ApiResponse response = delete("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("favorited").asBoolean()).isFalse();
        assertThat(article.path("favoritesCount").asInt()).isZero();
    }

    @Test
    void unfavoriteArticle_afterRefavoriting_returns200WithCountOne() {
        RegisteredUser author = newUser();
        RegisteredUser fan = newUser();
        JsonNode created = createArticle(author);
        post("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);
        delete("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);
        post("/api/articles/" + slugOf(created) + "/favorite", fan.token(), null);

        ApiResponse response = get("/api/articles/" + slugOf(created), fan.token());

        assertThat(response.status()).isEqualTo(200);
        var article = response.body().path("article");
        assertThat(article.path("favorited").asBoolean()).isTrue();
        assertThat(article.path("favoritesCount").asInt()).isEqualTo(1);
    }
}
