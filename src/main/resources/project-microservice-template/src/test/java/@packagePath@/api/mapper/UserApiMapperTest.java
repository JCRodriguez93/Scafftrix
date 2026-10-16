package @packageName@.api.mapper;

import @packageName@.api.model.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserApiMapperTest {

    private final UserApiMapper mapper = Mappers.getMapper(UserApiMapper.class);

    @Test
    void mapsDomainToApiModel() {
        User api = mapper.toApi(
                new @packageName@.domain.model.User(1L, "jdoe", "jdoe@example.com", "John", "Doe"));

        assertThat(api.getId()).isEqualTo(1L);
        assertThat(api.getUsername()).isEqualTo("jdoe");
        assertThat(api.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(api.getFirstName()).isEqualTo("John");
        assertThat(api.getLastName()).isEqualTo("Doe");
    }

    @Test
    void mapsApiModelToDomain() {
        User api = new User("jdoe", "jdoe@example.com").id(7L).firstName("John").lastName("Doe");

        @packageName@.domain.model.User domain = mapper.toDomain(api);

        assertThat(domain.id()).isEqualTo(7L);
        assertThat(domain.username()).isEqualTo("jdoe");
        assertThat(domain.email()).isEqualTo("jdoe@example.com");
    }
}
