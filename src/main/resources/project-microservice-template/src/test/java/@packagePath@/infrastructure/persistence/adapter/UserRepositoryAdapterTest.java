package @packageName@.infrastructure.persistence.adapter;

import @packageName@.application.port.out.UserRepository;
import @packageName@.domain.model.User;
import @packageName@.infrastructure.persistence.mapper.UserPersistenceMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el adaptador contra una base de datos real (H2) con las migraciones Flyway
 * aplicadas: es la prueba que demuestra que la persistencia funciona de punta a punta.
 */
@DataJpaTest
@Import({UserRepositoryAdapter.class, UserPersistenceMapperImpl.class})
class UserRepositoryAdapterTest {

    @Autowired
    private UserRepository adapter;

    @Test
    void savesAndReadsBackThroughThePort() {
        User saved = adapter.save(new User(null, "newbie", "newbie@example.com", "New", "Bee"));

        assertThat(saved.id()).isNotNull();
        Optional<User> found = adapter.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().username()).isEqualTo("newbie");
    }

    @Test
    void deletesAnExistingUser() {
        User saved = adapter.save(new User(null, "todelete", "todelete@example.com", null, null));

        adapter.deleteById(saved.id());

        assertThat(adapter.existsById(saved.id())).isFalse();
    }
}
