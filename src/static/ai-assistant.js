(function() {
    // Create button
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
    iframe.src = 'https://themain.ro/index.html';
    iframe.style.position = 'fixed';
    iframe.style.bottom = '70px';
    iframe.style.right = '20px';
    iframe.style.width = '600px';
    iframe.style.height = '800px';
    iframe.style.border = '1px solid #ccc';
    iframe.style.boxShadow = '0px 0px 10px rgba(0,0,0,0.1)';
    iframe.style.display = 'none';
    iframe.style.zIndex = '1000';
    document.body.appendChild(iframe);

    // Toggle iframe visibility
    button.addEventListener('click', function() {
        if (iframe.style.display === 'none') {
            iframe.style.display = 'block';
        } else {
            iframe.style.display = 'none';
        }
    });
})();
