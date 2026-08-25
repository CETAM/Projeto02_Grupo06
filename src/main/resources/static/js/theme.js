document.addEventListener('DOMContentLoaded', function () {
    var root = document.documentElement;
    var btn = document.getElementById('themeToggleBtn');
    var icon = document.getElementById('themeToggleIcon');

    function aplicarIcone(theme) {
        if (!icon) {
            return;
        }
        icon.className = theme === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
    }

    aplicarIcone(root.getAttribute('data-theme') || 'light');

    if (btn) {
        btn.addEventListener('click', function () {
            var atual = root.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
            var proximo = atual === 'dark' ? 'light' : 'dark';

            root.setAttribute('data-theme', proximo);
            localStorage.setItem('spd-theme', proximo);
            aplicarIcone(proximo);

            window.dispatchEvent(new CustomEvent('spd-theme-changed', { detail: { theme: proximo } }));
        });
    }
});
