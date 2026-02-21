const { exec } = require('child_process');

function runCurl(mobile, body, mediaUrl) {
    return new Promise((resolve) => {
        let cmd = `curl -X POST http://localhost:8080/api/public/whatsapp -d "From=whatsapp:+${mobile}&To=whatsapp:+14155238886&Body=${body}"`;
        if (mediaUrl) {
            cmd += ` -d "NumMedia=1" -d "MediaUrl0=${mediaUrl}" -d "MediaContentType0=image/jpeg"`;
        }
        exec(cmd, () => resolve());
    });
}

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

(async () => {
    // Unique for manual user
    const mobile = '91999990000';
    const name = 'Manual Tester User';

    console.log(`Seeding Manual User: ${name}`);
    await runCurl(mobile, 'Hi');
    await delay(2000);
    await runCurl(mobile, '1');
    await delay(2000);
    await runCurl(mobile, '1');
    await delay(2000);
    await runCurl(mobile, name);
    await delay(2000);
    await runCurl(mobile, 'Manual Testing Description');
    await delay(2000);
    await runCurl(mobile, '', 'https://picsum.photos/200/300');
    await delay(2000);
    await runCurl(mobile, 'Pune');

    console.log('Manual Seed Complete.');
})();
