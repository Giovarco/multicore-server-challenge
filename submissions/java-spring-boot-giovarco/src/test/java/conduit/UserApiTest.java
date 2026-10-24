package conduit;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserApiTest extends ConduitApiTest {

    @Test
    void register_withValidUser_returns201WithUser() {
        String unique = unique();
        Map<String, String> fields = Map.of(
                "username", "user-" + unique,
                "email", unique + "@example.com",
                "password", "secret-" + unique);

        ApiResponse response = post("/api/users", null, Map.of("user", fields));

        assertThat(response.status()).isEqualTo(201);
        var body = response.body().path("user");
        assertThat(body.path("username").asText()).isEqualTo(fields.get("username"));
        assertThat(body.path("email").asText()).isEqualTo(fields.get("email"));
        assertThat(body.path("bio").isNull()).isTrue();
        assertThat(body.path("image").isNull()).isTrue();
        assertThat(body.path("token").asText()).isNotBlank();
    }

    @Test
    void login_withCorrectPassword_returns200WithUser() {
        RegisteredUser user = newUser();

        ApiResponse response = post("/api/users/login", null, Map.of("user",
                Map.of("email", user.email(), "password", user.password())));

        assertThat(response.status()).isEqualTo(200);
        var body = response.body().path("user");
        assertThat(body.path("username").asText()).isEqualTo(user.username());
        assertThat(body.path("email").asText()).isEqualTo(user.email());
        assertThat(body.path("token").asText()).isNotBlank();
    }

    @Test
    void getUser_withValidToken_returns200WithPresentedToken() {
        RegisteredUser user = newUser();

        ApiResponse response = get("/api/user", user.token());

        assertThat(response.status()).isEqualTo(200);
        var body = response.body().path("user");
        assertThat(body.path("email").asText()).isEqualTo(user.email());
        assertThat(body.path("username").asText()).isEqualTo(user.username());
        assertThat(body.path("token").asText()).isEqualTo(user.token());
    }

    @Test
    void updateUser_withOnlyBio_returns200AndKeepsOtherFields() {
        RegisteredUser user = newUser();

        ApiResponse response = put("/api/user", user.token(), Map.of("user", Map.of("bio", "about me")));

        assertThat(response.status()).isEqualTo(200);
        var body = response.body().path("user");
        assertThat(body.path("bio").asText()).isEqualTo("about me");
        assertThat(body.path("username").asText()).isEqualTo(user.username());
        assertThat(body.path("email").asText()).isEqualTo(user.email());
        assertThat(body.path("token").asText()).isEqualTo(user.token());
    }

    @Test
    void updateUser_withEmailAndPassword_returns200AndNewCredentialsWork() {
        RegisteredUser user = newUser();
        String newEmail = unique() + "@example.com";
        String newPassword = "secret-" + unique();

        ApiResponse response = put("/api/user", user.token(),
                Map.of("user", Map.of("email", newEmail, "password", newPassword)));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("user").path("email").asText()).isEqualTo(newEmail);

        ApiResponse login = post("/api/users/login", null,
                Map.of("user", Map.of("email", newEmail, "password", newPassword)));
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.body().path("user").path("username").asText()).isEqualTo(user.username());
    }

    @Test
    void updateUser_withImage_returns200WithImage() {
        RegisteredUser user = newUser();

        ApiResponse response = put("/api/user", user.token(),
                Map.of("user", Map.of("image", "https://example.com/" + unique() + ".png")));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().path("user").path("image").asText())
                .startsWith("https://example.com/");
    }
}
