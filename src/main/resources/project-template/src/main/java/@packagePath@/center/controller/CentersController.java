package @packageName@.center.controller;

import @packageName@.center.service.CenterService;
import org.openapitools.api.CentrosApi;
import org.openapitools.model.Centro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CentersController implements CentrosApi {

    private final CenterService service;

    public CentersController(CenterService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<Centro>> getAllCentros() {
        return ResponseEntity.ok(service.findAll());
    }

    @Override
    public ResponseEntity<Centro> getCentro(Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Override
    public ResponseEntity<Centro> createCentro(Centro centro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(centro));
    }

    @Override
    public ResponseEntity<Centro> updateCentro(Integer id, Centro centro) {
        return ResponseEntity.ok(service.update(id, centro));
    }

    @Override
    public ResponseEntity<Void> deleteCentro(Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
