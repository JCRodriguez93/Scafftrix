-- Datos de ejemplo para que el microservicio responda algo en el primer arranque.
INSERT INTO center (name, city) VALUES ('Centro Madrid', 'Madrid');
INSERT INTO center (name, city) VALUES ('Centro Barcelona', 'Barcelona');

INSERT INTO app_user (username, email, first_name, last_name)
VALUES ('jdoe', 'jdoe@example.com', 'John', 'Doe');
INSERT INTO app_user (username, email, first_name, last_name)
VALUES ('alopez', 'alopez@example.com', 'Ana', 'Lopez');
