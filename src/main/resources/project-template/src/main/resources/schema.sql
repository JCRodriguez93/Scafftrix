CREATE TABLE centro (
    num_centro    INTEGER      NOT NULL,
    nombre_centro VARCHAR(255) NOT NULL,
    CONSTRAINT pk_centro PRIMARY KEY (num_centro)
);

CREATE TABLE empleado (
    id_empleado INTEGER      NOT NULL,
    nombre      VARCHAR(255) NOT NULL,
    id_centro   INTEGER,
    centro      VARCHAR(255),
    CONSTRAINT pk_empleado PRIMARY KEY (id_empleado)
);
