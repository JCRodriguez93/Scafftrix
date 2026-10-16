package es.scafftrix.factory.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevelopmentCurrentUserProviderTest {

    @Test
    void reportsTheBuiltInDevelopmentIdentity() {
        DevelopmentCurrentUserProvider provider = new DevelopmentCurrentUserProvider();

        CurrentUserProvider.CurrentUser user = provider.currentUser();

        assertThat(user.username()).isEqualTo("developer");
        assertThat(user.displayName()).contains("Developer");
    }

    @Test
    void trimsAndLowercasesTheUsername() {
        CurrentUserProvider.CurrentUser user = new CurrentUserProvider.CurrentUser("  Alice  ", "Ana");

        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.displayName()).isEqualTo("Ana");
    }

    @Test
    void fallsBackToTheUsernameWhenDisplayNameIsMissing() {
        CurrentUserProvider.CurrentUser user = new CurrentUserProvider.CurrentUser("bob", "   ");

        assertThat(user.displayName()).isEqualTo("bob");
    }

    @Test
    void rejectsABlankUsername() {
        assertThatThrownBy(() -> new CurrentUserProvider.CurrentUser("  ", "Ana"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrentUserProvider.CurrentUser(null, "Ana"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
