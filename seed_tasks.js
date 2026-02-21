const { exec } = require('child_process');
const fs = require('fs');

function runCurl(mobile, body) {
    return new Promise((resolve, reject) => {
        // Encode only the body if needed, but simple alphanum is fine
        const cmd = `curl -X POST http://localhost:8080/api/public/whatsapp -d "From=whatsapp:+${mobile}&To=whatsapp:+14155238886&Body=${body}"`;
        exec(cmd, (error, stdout, stderr) => {
            if (error) console.error(`exec error: ${error}`);
            console.log(`Sent to ${mobile}: ${body}`);
            resolve();
        });
    });
}

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

(async () => {
    // Generate unique number and name
    const randomSuffix = Math.floor(Math.random() * 10000);
    const uniqueMobile = `9199999${String(randomSuffix).padStart(4, '0')}`; // e.g. 91999991234
    const uniqueName = `Auto Test User ${randomSuffix}`;
    const uniqueDesc = `Description for ${uniqueName}`;

    console.log(`Using Mobile: ${uniqueMobile}, Name: ${uniqueName}`);

    // Save config for verification script
    fs.writeFileSync('seed_config.json', JSON.stringify({
        name: uniqueName,
        mobile: uniqueMobile,
        desc: uniqueDesc
    }));

    console.log('Starting Seed...');
    await runCurl(uniqueMobile, 'Hi'); // Reset/Welcome
    await delay(3000); // 3s delay to be safe
    await runCurl(uniqueMobile, '1'); // Eng
    await delay(3000);
    await runCurl(uniqueMobile, '1'); // Dept
    await delay(3000);
    await runCurl(uniqueMobile, uniqueName); // Name
    await delay(3000);
    await runCurl(uniqueMobile, uniqueDesc); // Desc
    await delay(3000);
    await runCurl(uniqueMobile, 'Skip'); // Photo
    await delay(3000);
    await runCurl(uniqueMobile, 'Pune'); // Location

    console.log('Seed Complete. Waiting for backend persistence...');
    await delay(2000);
})();
