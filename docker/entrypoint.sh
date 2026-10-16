#!/bin/sh
# =============================================================================
# Entrypoint del contenedor Scafftrix.
#
# La imagen arranca como root unicamente para un ajuste de permisos y despues
# baja a 'factory'. Motivo: ./data y ./logs son bind mounts del host, y en un
# host Linux Docker los crea root. Sin este ajuste, el usuario no root no puede
# abrir el fichero de H2 y la aplicacion no arranca. En Docker Desktop (Windows
# y macOS) los bind mounts son escribibles por cualquier uid y el paso es inocuo.
#
# El chown es **no recursivo y solo si el directorio es de root**: los ficheros
# de dentro los crea siempre la aplicacion corriendo como 'factory', asi que no
# necesitan cambiar de propietario. Recursivo seria peligroso: un bind mount mal
# configurado (una ruta del host que no es nuestra) haria que se cambiase la
# propiedad de todo un arbol ajeno.
# =============================================================================
set -e

APP_USER=factory
STATE_DIRS="/app/data /app/logs /opt/scafftrix/workspace"

if [ "$(id -u)" = "0" ]; then
    for dir in $STATE_DIRS; do
        mkdir -p "$dir"
        if [ "$(stat -c %u "$dir")" = "0" ]; then
            chown "$APP_USER":"$APP_USER" "$dir"
        fi
    done

    # HOME explicito: lo usan el user de la JVM y cualquier cache de Maven/JDK
    # que pueda crear la aplicacion.
    export HOME=/home/factory

    # setpriv viene de util-linux, que ya esta en la imagen: no hace falta
    # instalar su-exec/gosu (su-exec ya no existe en Ubuntu 26.04).
    # El proceso final queda como 'factory', con todos los privilegios del
    # root inicial descartados.
    exec setpriv --reuid="$APP_USER" --regid="$APP_USER" --init-groups "$@"
fi

# Ya se ejecuta como un usuario concreto (compose user:, docker run --user).
# Solo se comprueba que los directorios con estado existen.
for dir in $STATE_DIRS; do
    mkdir -p "$dir" 2>/dev/null || true
done

exec "$@"
