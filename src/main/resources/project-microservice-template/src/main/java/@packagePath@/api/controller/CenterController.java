package @packageName@.api.controller;

import @packageName@.api.mapper.CenterApiMapper;
import @packageName@.api.model.Center;
import @packageName@.api.CentersApi;
import @packageName@.application.port.in.CenterUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Implementa la interfaz de centros generada a partir del contrato OpenAPI.
 */
@RestController
public class CenterController implements CentersApi {

    private final CenterUseCase centerUseCase;
    private final CenterApiMapper mapper;

    public CenterController(CenterUseCase centerUseCase, CenterApiMapper mapper) {
        this.centerUseCase = centerUseCase;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<List<Center>> getAllCenters() {
        return ResponseEntity.ok(centerUseCase.findAll().stream().map(mapper::toApi).toList());
    }

    @Override
    public ResponseEntity<Center> getCenter(Long id) {
        return ResponseEntity.ok(mapper.toApi(centerUseCase.findById(id)));
    }

    @Override
    public ResponseEntity<Center> createCenter(Center center) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toApi(centerUseCase.create(mapper.toDomain(center))));
    }

    @Override
    public ResponseEntity<Center> updateCenter(Long id, Center center) {
        return ResponseEntity.ok(mapper.toApi(centerUseCase.update(id, mapper.toDomain(center))));
    }

    @Override
    public ResponseEntity<Void> deleteCenter(Long id) {
        centerUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
