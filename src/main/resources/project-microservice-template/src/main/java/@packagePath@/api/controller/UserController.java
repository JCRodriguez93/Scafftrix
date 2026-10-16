package @packageName@.api.controller;

import @packageName@.api.mapper.UserApiMapper;
import @packageName@.api.model.User;
import @packageName@.api.UsersApi;
import @packageName@.application.port.in.UserUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Implementa la interfaz generada a partir del contrato OpenAPI.
 *
 * <p>El contrato define la superficie HTTP; esta clase solo conecta esa superficie con el
 * caso de uso y traduce con MapStruct. No contiene logica de negocio.
 */
@RestController
public class UserController implements UsersApi {

    private final UserUseCase userUseCase;
    private final UserApiMapper mapper;

    public UserController(UserUseCase userUseCase, UserApiMapper mapper) {
        this.userUseCase = userUseCase;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userUseCase.findAll().stream().map(mapper::toApi).toList());
    }

    @Override
    public ResponseEntity<User> getUser(Long id) {
        return ResponseEntity.ok(mapper.toApi(userUseCase.findById(id)));
    }

    @Override
    public ResponseEntity<User> createUser(User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toApi(userUseCase.create(mapper.toDomain(user))));
    }

    @Override
    public ResponseEntity<User> updateUser(Long id, User user) {
        return ResponseEntity.ok(mapper.toApi(userUseCase.update(id, mapper.toDomain(user))));
    }

    @Override
    public ResponseEntity<Void> deleteUser(Long id) {
        userUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
