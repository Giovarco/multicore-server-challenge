package conduit;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileApiTest extends ConduitApiTest {

    @Test
    void getProfile_anonymously_returns200WithFollowingFalse() {
        RegisteredUser target = newUser();

        ApiResponse response = get("/api/profiles/" + target.username(), null);

        assertThat(response.status()).isEqualTo(200);
        var profile = response.body().path("profile");
        assertThat(profile.path("username").asText()).isEqualTo(target.username());
        assertThat(profile.path("bio").isNull()).isTrue();
        assertThat(profile.path("image").isNull()).isTrue();
        assertThat(profile.path("following").asBoolean()).isFalse();
    }

    @Test
    void getProfile_asAnotherUser_returns200WithFollowingFalse() {
        RegisteredUser viewer = newUser();
        RegisteredUser target = newUser();

        ApiResponse response = get("/api/profiles/" + target.username(), viewer.token());

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("profile").path("following").asBoolean()).isFalse();
    }

    @Test
    void followProfile_returns200WithFollowingTrue() {
        RegisteredUser follower = newUser();
        RegisteredUser followee = newUser();

        ApiResponse response = post("/api/profiles/" + followee.username() + "/follow", follower.token(), Map.of());

        assertThat(response.status()).isEqualTo(200);
        var profile = response.body().path("profile");
        assertThat(profile.path("username").asText()).isEqualTo(followee.username());
        assertThat(profile.path("following").asBoolean()).isTrue();

        ApiResponse fetched = get("/api/profiles/" + followee.username(), follower.token());
        assertThat(fetched.body().path("profile").path("following").asBoolean()).isTrue();
    }

    @Test
    void unfollowProfile_returns200WithFollowingFalse() {
        RegisteredUser follower = newUser();
        RegisteredUser followee = newUser();
        follow(follower, followee);

        ApiResponse response = delete("/api/profiles/" + followee.username() + "/follow", follower.token(), Map.of());

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("profile").path("following").asBoolean()).isFalse();

        ApiResponse fetched = get("/api/profiles/" + followee.username(), follower.token());
        assertThat(fetched.body().path("profile").path("following").asBoolean()).isFalse();
    }

    @Test
    void followProfile_twice_returns200WithFollowingTrue() {
        RegisteredUser follower = newUser();
        RegisteredUser followee = newUser();
        follow(follower, followee);

        ApiResponse response = post("/api/profiles/" + followee.username() + "/follow", follower.token(), Map.of());

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("profile").path("following").asBoolean()).isTrue();
    }
}
