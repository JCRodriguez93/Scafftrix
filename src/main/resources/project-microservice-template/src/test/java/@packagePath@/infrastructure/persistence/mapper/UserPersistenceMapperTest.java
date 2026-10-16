package @packageName@.infrastructure.persistence.mapper;

import @packageName@.domain.model.User;
import @packageName@.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserPersistenceMapperTest {

    private final UserPersistenceMapper mapper = Mappers.getMapper(UserPersistenceMapper.class);

    @Test
    void mapsEntityToDomain() {
        UserEntity entity = new UserEntity();
        entity.setId(3L);
        entity.setUsername("jdoe");
        entity.setEmail("jdoe@example.com");
        entity.setFirstName("John");
        entity.setLastName("Doe");

        User domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(3L);
        assertThat(domain.username()).isEqualTo("jdoe");
        assertThat(domain.lastName()).isEqualTo("Doe");
    }

    @Test
    void mapsDomainToEntity() {
        UserEntity entity = mapper.toEntity(new User(3L, "jdoe", "jdoe@example.com", "John", "Doe"));

        assertThat(entity.getId()).isEqualTo(3L);
        assertThat(entity.getUsername()).isEqualTo("jdoe");
        assertThat(entity.getEmail()).isEqualTo("jdoe@example.com");
    }
}
