(function() {
    // Create main button
    const button = document.createElement('button');
    button.textContent = 'AI Assistant';
    button.style.position = 'fixed';
    button.style.bottom = '20px';
    button.style.right = '20px';
    button.style.zIndex = '1000';
    button.style.padding = '10px 20px';
    button.style.backgroundColor = '#4CAF50';
    button.style.color = 'white';
    button.style.border = 'none';
    button.style.cursor = 'pointer';
    document.body.appendChild(button);

    // Create iframe
    const iframe = document.createElement('iframe');
    iframe.src = 'https://themain.ro/index-template.html';
    iframe.style.position = 'fixed';
    iframe.style.bottom = '70px';
    iframe.style.right = '20px';
    iframe.style.width = '500px';
    iframe.style.height = '300px';
    iframe.style.border = '1px solid #ccc';
    iframe.style.boxShadow = '0px 0px 10px rgba(0,0,0,0.1)';
    iframe.style.display = 'none';
    iframe.style.zIndex = '1000';
    document.body.appendChild(iframe);

    // Toggle iframe visibility
    button.addEventListener('click', function() {
        iframe.style.display = 'block';
        button.style.display = 'none';
    });

    // Inject close button into iframe after it loads
    iframe.onload = function() {
        const iframeDocument = iframe.contentDocument || iframe.contentWindow.document;

        // Create close button inside iframe
        const closeButton = iframeDocument.createElement('button');
        closeButton.textContent = 'Inchide';
        closeButton.style.position = 'fixed';
        closeButton.style.top = '5px';
        closeButton.style.right = '5px';
        closeButton.style.zIndex = '1001';
        closeButton.style.padding = '5px 10px';
        closeButton.style.backgroundColor = '#FF0000';
        closeButton.style.color = 'white';
        closeButton.style.border = 'none';
        closeButton.style.cursor = 'pointer';
        iframeDocument.body.appendChild(closeButton);

        // Close iframe when close button is clicked
        closeButton.addEventListener('click', function() {
            iframe.style.display = 'none';
            button.style.display = 'block';
        });
    };

    // Adjust iframe size based on screen width
    function adjustIframeSize() {
        if (window.innerWidth <= 600) { // Mobile devices
            iframe.style.width = '90%';
            iframe.style.height = '45%';
            iframe.style.bottom = '5px';
            iframe.style.right = '3px';
        } else { // Desktop devices
            iframe.style.width = '500px';
            iframe.style.height = '400px';
            iframe.style.bottom = '70px';
            iframe.style.right = '20px';
        }
    }

    // Initial adjustment
    adjustIframeSize();

    // Adjust on window resize
    window.addEventListener('resize', adjustIframeSize);
})();
