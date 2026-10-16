(function () {
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(function (tab) {
        tab.addEventListener('click', function (e) {
            e.preventDefault();
            const target = tab.dataset.tab;
            tabs.forEach(function (t) { t.classList.toggle('active', t === tab); });
            document.querySelectorAll('.tab-panel').forEach(function (panel) {
                panel.classList.toggle('active', panel.id === 'panel-' + target);
            });
        });
    });

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('tab') === 'logs') {
        const logsTab = document.getElementById('tab-logs');
        if (logsTab) {
            logsTab.click();
        }
    }

    const typeSelect = document.getElementById('projectType');
    if (typeSelect) {
        const setText = function (id, value, fallback) {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = (value !== undefined && value !== null && value !== '') ? value : fallback;
            }
        };

        const formatSelect = document.getElementById('configFormat');
        let formatTouched = false;
        if (formatSelect) {
            formatSelect.addEventListener('change', function () { formatTouched = true; });
        }

        const renderTechPanel = function () {
            const option = typeSelect.options[typeSelect.selectedIndex];
            const selected = option && option.value !== '';
            setText('tech-database', selected ? option.dataset.database : '', '—');
            setText('tech-frontend', selected ? option.dataset.frontend : '', 'Sin frontend');
            setText('tech-build', selected ? option.dataset.build : '', '—');
            let contract = '';
            if (selected) {
                contract = option.dataset.contract === 'true' ? 'Sí' : 'No';
            }
            setText('tech-contract', contract, '—');

            const note = document.getElementById('tech-note');
            if (note) {
                note.textContent = selected && option.dataset.note
                    ? option.dataset.note
                    : 'Selecciona un tipo de proyecto para ver su stack fijo.';
            }

            if (selected && formatSelect && !formatTouched) {
                for (const formatOption of formatSelect.options) {
                    formatOption.selected = (formatOption.value === option.dataset.format);
                }
            }
        };

        typeSelect.addEventListener('change', renderTechPanel);
        renderTechPanel();
    }

    // Confirmación en el navegador para acciones destructivas. El texto vive en el
    // atributo data-confirm de la plantilla, no aquí.
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.confirm(form.dataset.confirm)) {
                e.preventDefault();
            }
        });
    });

    // Anti doble envío: si el formulario se envía de verdad, se deshabilita el botón
    // hasta que el navegador navegue. Se respeta preventDefault de las confirmaciones.
    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (e.defaultPrevented) {
                return;
            }
            const button = form.querySelector('button[type="submit"]');
            if (button) {
                button.disabled = true;
            }
        });
    });
})();