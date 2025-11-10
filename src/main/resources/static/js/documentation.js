// Documentation page JavaScript

// Configure marked.js when it loads
document.addEventListener('DOMContentLoaded', function() {
    if (typeof marked !== 'undefined') {
        marked.setOptions({
            breaks: true,
            gfm: true,
            headerIds: true,
            mangle: false
        });
    }
});

// Toggle markdown content visibility using DOM traversal
// This function is global so it can be called from onclick attributes
function toggleMarkdown(button) {
    // Get the parent row (main documentation row)
    const docRow = button.closest('tr');
    if (!docRow) {
        console.error('Could not find parent row');
        return;
    }

    // Get the next sibling (markdown expandable row)
    const markdownRow = docRow.nextElementSibling;
    if (!markdownRow || !markdownRow.classList.contains('markdown-row')) {
        console.error('Could not find markdown row');
        return;
    }

    // Find the icon within the button
    const toggleIcon = button.querySelector('i');
    if (!toggleIcon) {
        console.error('Could not find toggle icon');
        return;
    }

    // Get doc ID from button's data attribute
    const docId = button.dataset.docId;
    if (!docId) {
        console.error('Could not find doc ID');
        return;
    }

    if (markdownRow.classList.contains('d-none')) {
        // Expand
        markdownRow.classList.remove('d-none');
        toggleIcon.classList.remove('bi-chevron-right');
        toggleIcon.classList.add('bi-chevron-down');

        // Load markdown if not already loaded
        const markdownContent = markdownRow.querySelector('.markdown-content');
        if (markdownContent && markdownContent.innerHTML.trim() === 'Content will be loaded here via JavaScript') {
            loadMarkdownContent(docId, markdownRow);
        }
    } else {
        // Collapse
        markdownRow.classList.add('d-none');
        toggleIcon.classList.remove('bi-chevron-down');
        toggleIcon.classList.add('bi-chevron-right');
    }
}

// Load markdown content from server
function loadMarkdownContent(docId, markdownRow) {
    const loadingDiv = markdownRow.querySelector('.markdown-loading');
    const markdownContent = markdownRow.querySelector('.markdown-content');
    const errorDiv = markdownRow.querySelector('.markdown-error');
    const errorMsg = markdownRow.querySelector('.markdown-error-msg');

    if (!markdownContent) {
        console.error('Could not find markdown content div');
        return;
    }

    // Show loading spinner if it exists
    if (loadingDiv) loadingDiv.classList.remove('d-none');
    markdownContent.classList.add('d-none');
    if (errorDiv) errorDiv.classList.add('d-none');

    // Fetch markdown content
    fetch(`/api/documentation/${docId}/content`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.content && data.content.trim() !== '') {
                // Render markdown
                const htmlContent = marked.parse(data.content);
                markdownContent.innerHTML = htmlContent;

                // Hide loading, show content
                if (loadingDiv) loadingDiv.classList.add('d-none');
                markdownContent.classList.remove('d-none');
            } else {
                // Show "no content" message
                if (errorMsg) {
                    errorMsg.textContent = 'No content available for this documentation';
                }
                if (loadingDiv) loadingDiv.classList.add('d-none');
                if (errorDiv) errorDiv.classList.remove('d-none');
            }
        })
        .catch(error => {
            console.error('Error loading markdown:', error);
            if (errorMsg) {
                errorMsg.textContent = `Failed to load content: ${error.message}`;
            }

            // Hide loading, show error
            if (loadingDiv) loadingDiv.classList.add('d-none');
            if (errorDiv) errorDiv.classList.remove('d-none');
        });
}
