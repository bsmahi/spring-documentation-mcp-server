// Custom JavaScript for Spring MCP Server

// Auto-dismiss alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

// CSRF token for HTMX requests
document.addEventListener('DOMContentLoaded', function() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');

    if (token && header) {
        document.body.addEventListener('htmx:configRequest', function(event) {
            event.detail.headers[header.content] = token.content;
        });
    }
});
