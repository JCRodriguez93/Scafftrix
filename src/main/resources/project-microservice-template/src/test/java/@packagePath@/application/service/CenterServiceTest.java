package @packageName@.application.service;

import @packageName@.application.port.out.CenterRepository;
import @packageName@.domain.exception.CenterNotFoundException;
import @packageName@.domain.model.Center;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CenterServiceTest {

    @Mock
    private CenterRepository repository;

    @InjectMocks
    private CenterService service;

    @Test
    void findByIdReturnsTheCenter() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Center(1L, "Centro Madrid", "Madrid")));

        assertThat(service.findById(1L).name()).isEqualTo("Centro Madrid");
    }

    @Test
    void findByIdThrowsWhenTheCenterDoesNotExist() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(CenterNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenTheCenterDoesNotExist() {
        when(repository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(CenterNotFoundException.class);
    }
}
