package @packageName@.center.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "centro")
public class CenterEntity {

    @Id
    @Column(name = "num_centro")
    private Integer numCentro;

    @Column(name = "nombre_centro")
    private String nombreCentro;

    public Integer getNumCentro() {
        return numCentro;
    }

    public void setNumCentro(Integer numCentro) {
        this.numCentro = numCentro;
    }

    public String getNombreCentro() {
        return nombreCentro;
    }

    public void setNombreCentro(String nombreCentro) {
        this.nombreCentro = nombreCentro;
    }
}
