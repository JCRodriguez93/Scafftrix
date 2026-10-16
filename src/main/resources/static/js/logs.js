(function () {
    const consoleEl = document.getElementById('log-console');
    const statusEl = document.getElementById('log-status');
    const refreshBtn = document.getElementById('refresh-logs');
    if (!consoleEl || !window.projectId) {
        return;
    }

    let source = null;
    function connect() {
        if (source) {
            source.close();
        }
        statusEl.textContent = 'Conectando al stream de logs...';
        source = new EventSource('/projects/' + window.projectId + '/generation/log/stream');
        source.addEventListener('status', function (event) {
            try {
                const data = JSON.parse(event.data);
                render(data.entries || []);
                if (!data.finished) {
                    statusEl.textContent = 'Generación en curso...';
                    return;
                }
                statusEl.textContent = 'Generación finalizada. Estado: ' + data.status;
                source.close();
                source = null;
                // Recargar sólo si la página se pintó en un estado en curso.
                // No sirve "vimos algún frame running": si el proyecto termina
                // antes de que el navegador abra el stream (batch tarda <1s), el
                // primer frame ya llega finished=true y la página se quedaría en
                // GENERATING. Comparar con el estado pintado tampoco entra en
                // bucle, porque una página GENERATED vuelve a ver finished=true.
                if (window.projectStatus === 'GENERATING') {
                    setTimeout(function () {
                        location.reload();
                    }, 1200);
                }
            } catch (e) {
                console.error(e);
            }
        });
        source.onerror = function () {
            statusEl.textContent = 'Conexión perdida, reintentando en 2s...';
            setTimeout(connect, 2000);
        };
    }

    // El servidor envía el snapshot completo en cada frame, así que se
    // reemplaza el contenido (acumular duplicaría las líneas cada segundo).
    function render(entries) {
        const lines = entries.map(function (e) {
            return '[' + e.instant + '] [' + e.level + '] ' + e.message;
        });
        consoleEl.textContent = lines.join('\n');
        consoleEl.scrollTop = consoleEl.scrollHeight;
    }

    if (refreshBtn) {
        refreshBtn.addEventListener('click', function () {
            fetch('/projects/' + window.projectId + '/generation/status')
                .then(function (res) { return res.json(); })
                .then(function (data) { render(data.entries || []); });
        });
    }

    connect();
})();