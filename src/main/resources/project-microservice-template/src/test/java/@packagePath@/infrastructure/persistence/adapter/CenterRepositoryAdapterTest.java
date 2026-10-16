package @packageName@.infrastructure.persistence.adapter;

import @packageName@.application.port.out.CenterRepository;
import @packageName@.domain.model.Center;
import @packageName@.infrastructure.persistence.mapper.CenterPersistenceMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CenterRepositoryAdapter.class, CenterPersistenceMapperImpl.class})
class CenterRepositoryAdapterTest {

    @Autowired
    private CenterRepository adapter;

    @Test
    void savesAndFindsAllThroughThePort() {
        adapter.save(new Center(null, "Centro Nuevo", "Valencia"));

        assertThat(adapter.findAll())
                .anySatisfy(center -> assertThat(center.name()).isEqualTo("Centro Nuevo"));
    }

    @Test
    void updatesAnExistingCenter() {
        Center saved = adapter.save(new Center(null, "Centro Viejo", "Sevilla"));

        adapter.save(new Center(saved.id(), "Centro Renombrado", "Sevilla"));

        assertThat(adapter.findById(saved.id()))
                .get()
                .extracting(Center::name)
                .isEqualTo("Centro Renombrado");
    }
}
