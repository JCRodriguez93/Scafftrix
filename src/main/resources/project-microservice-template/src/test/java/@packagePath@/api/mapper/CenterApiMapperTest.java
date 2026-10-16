package @packageName@.api.mapper;

import @packageName@.api.model.Center;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class CenterApiMapperTest {

    private final CenterApiMapper mapper = Mappers.getMapper(CenterApiMapper.class);

    @Test
    void mapsDomainToApiModel() {
        Center api = mapper.toApi(new @packageName@.domain.model.Center(2L, "Centro Madrid", "Madrid"));

        assertThat(api.getId()).isEqualTo(2L);
        assertThat(api.getName()).isEqualTo("Centro Madrid");
        assertThat(api.getCity()).isEqualTo("Madrid");
    }

    @Test
    void mapsApiModelToDomain() {
        Center api = new Center("Centro Barcelona").id(3L).city("Barcelona");

        @packageName@.domain.model.Center domain = mapper.toDomain(api);

        assertThat(domain.id()).isEqualTo(3L);
        assertThat(domain.name()).isEqualTo("Centro Barcelona");
        assertThat(domain.city()).isEqualTo("Barcelona");
    }
}
