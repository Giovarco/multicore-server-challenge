package conduit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentsApiTest extends ConduitApiTest {

    private String slugOf(JsonNode article) {
        return article.path("slug").asText();
    }

    @Test
    void createComment_returns201WithCommentAndFollowingFalse() {
        RegisteredUser author = newUser();
        RegisteredUser commenter = newUser();
        JsonNode created = createArticle(author);

        ApiResponse response = post("/api/articles/" + slugOf(created) + "/comments", commenter.token(),
                Map.of("comment", Map.of("body", "nice article")));

        assertThat(response.status()).isEqualTo(201);
        var comment = response.body().path("comment");
        assertThat(comment.path("id").asLong()).isPositive();
        assertThat(comment.path("body").asText()).isEqualTo("nice article");
        assertThat(comment.path("author").path("username").asText()).isEqualTo(commenter.username());
        assertThat(comment.path("author").path("following").asBoolean()).isFalse();
        assertThatTimestampFormat(comment.path("createdAt"));
        assertThatTimestampFormat(comment.path("updatedAt"));
    }

    @Test
    void getComments_anonymously_returnsNewestFirst() {
        RegisteredUser author = newUser();
        JsonNode created = createArticle(author);
        List<Long> commentIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            commentIds.add(createComment(author, created.path("slug").asText(), "comment " + i));
        }

        ApiResponse response = get("/api/articles/" + slugOf(created) + "/comments", null);

        assertThat(response.status()).isEqualTo(200);
        var comments = response.body().path("comments");
        assertThat(comments.size()).isEqualTo(3);
        List<Long> fetchedIds = new ArrayList<>();
        for (var comment : comments) {
            fetchedIds.add(comment.path("id").asLong());
        }
        assertThat(fetchedIds).containsExactly(commentIds.get(2), commentIds.get(1), commentIds.get(0));
    }

    @Test
    void deleteOwnComment_returns204AndRemovesComment() {
        RegisteredUser author = newUser();
        JsonNode created = createArticle(author);
        long commentId = createComment(author, slugOf(created), "to delete");

        ApiResponse response = delete("/api/articles/" + slugOf(created) + "/comments/" + commentId,
                author.token(), null);

        assertThat(response.status()).isEqualTo(204);
        ApiResponse fetched = get("/api/articles/" + slugOf(created) + "/comments", null);
        assertThat(fetched.body().path("comments").size()).isZero();
    }

    private long createComment(RegisteredUser commenter, String slug, String body) {
        ApiResponse response = post("/api/articles/" + slug + "/comments", commenter.token(),
                Map.of("comment", Map.of("body", body)));
        if (!response.is(201)) {
            throw new IllegalStateException("creating comment returned " + response.status());
        }
        return response.body().path("comment").path("id").asLong();
    }
}
