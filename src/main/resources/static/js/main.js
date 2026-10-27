/**
 * SWAYAM NPTEL Assignment Manager - Main JavaScript
 */

// Sidebar toggle
document.addEventListener('DOMContentLoaded', function () {
    const toggleBtn = document.getElementById('sidebarToggle');
    const wrapper = document.getElementById('wrapper');

    if (toggleBtn) {
        toggleBtn.addEventListener('click', function () {
            if (window.innerWidth <= 768) {
                // Mobile: show/hide sidebar
                const sidebar = document.getElementById('sidebar');
                sidebar.classList.toggle('show');
            } else {
                // Desktop: collapse/expand
                wrapper.classList.toggle('sidebar-collapsed');
                localStorage.setItem('sidebarCollapsed',
                    wrapper.classList.contains('sidebar-collapsed'));
            }
        });
    }

    // Restore sidebar state on desktop
    if (window.innerWidth > 768 && localStorage.getItem('sidebarCollapsed') === 'true') {
        if (wrapper) wrapper.classList.add('sidebar-collapsed');
    }

    // Auto-dismiss alerts after 5 seconds
    document.querySelectorAll('.alert.alert-success, .alert.alert-danger').forEach(function (el) {
        setTimeout(function () {
            el.classList.remove('show');
            el.classList.add('fade');
        }, 5000);
    });

    // Dark mode toggle (reads from localStorage)
    const darkMode = localStorage.getItem('darkMode') === 'true';
    if (darkMode) {
        document.body.classList.add('dark-mode');
    }

    // Confirm delete forms (fallback if onclick not set)
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!confirm(form.getAttribute('data-confirm'))) {
                e.preventDefault();
            }
        });
    });

    // Update unread notification count badge dynamically via polling (optional)
    // Not implemented to keep it simple - page refresh handles it
});

/**
 * Toggle dark mode
 */
function toggleDarkMode() {
    document.body.classList.toggle('dark-mode');
    localStorage.setItem('darkMode', document.body.classList.contains('dark-mode'));
}

/**
 * Countdown timer for due dates
 */
function updateCountdowns() {
    document.querySelectorAll('[data-due-date]').forEach(function (el) {
        const dueDate = new Date(el.getAttribute('data-due-date'));
        const now = new Date();
        const diff = dueDate - now;
        if (diff < 0) {
            el.textContent = 'Overdue';
            el.classList.add('text-danger');
        } else {
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            if (days > 0) {
                el.textContent = days + 'd ' + hours + 'h left';
            } else {
                const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
                el.textContent = hours + 'h ' + mins + 'm left';
                el.classList.add('text-warning');
            }
        }
    });
}

setInterval(updateCountdowns, 60000);
updateCountdowns();
