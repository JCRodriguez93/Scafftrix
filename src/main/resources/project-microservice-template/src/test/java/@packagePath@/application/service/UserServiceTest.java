package @packageName@.application.service;

import @packageName@.application.port.out.UserRepository;
import @packageName@.domain.exception.UserNotFoundException;
import @packageName@.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService service;

    @Test
    void createIgnoresTheIdSentByTheClient() {
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User given = invocation.getArgument(0);
            return new User(99L, given.username(), given.email(), given.firstName(), given.lastName());
        });

        User created = service.create(new User(555L, "jdoe", "jdoe@example.com", "John", "Doe"));

        assertThat(created.id()).isEqualTo(99L);
        verify(repository).save(new User(null, "jdoe", "jdoe@example.com", "John", "Doe"));
    }

    @Test
    void findByIdThrowsWhenTheUserDoesNotExist() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void findAllReturnsTheContentOfThePort() {
        when(repository.findAll()).thenReturn(List.of(new User(1L, "a", "a@example.com", null, null)));

        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void updateThrowsWhenTheUserDoesNotExist() {
        when(repository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.update(7L, new User(null, "a", "a@example.com", null, null)))
                .isInstanceOf(UserNotFoundException.class);
    }
}
