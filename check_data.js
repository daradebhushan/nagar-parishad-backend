// Using http std lib to avoid dependencies
const http = require('http');

function request(options, data) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            let body = '';
            res.on('data', (chunk) => body += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(body));
                } catch (e) {
                    resolve(body);
                }
            });
        });
        req.on('error', (e) => reject(e));
        if (data) req.write(data);
        req.end();
    });
}

(async () => {
    try {
        // 1. Login
        const loginData = JSON.stringify({ email: "admin@nagarparishad.in", password: "password" });
        const loginRes = await request({
            hostname: 'localhost', port: 8080, path: '/api/auth/login', method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Content-Length': loginData.length }
        }, loginData);

        if (!loginRes.data || !loginRes.data.token) {
            console.error('Login Failed:', loginRes);
            return;
        }
        const token = loginRes.data.token;
        console.log('Got Token.');

        // 2. Get Complaints
        const listRes = await request({
            hostname: 'localhost', port: 8080, path: '/api/admin/complaints', method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (Array.isArray(listRes)) {
            console.log(`Found ${listRes.length} complaints.`);
            listRes.forEach(c => console.log(` - ID: ${c.complaintNo}, Name: "${c.citizenName}", Status: ${c.status}`));
        } else {
            console.log('List response:', listRes);
        }

    } catch (e) {
        console.error(e);
    }
})();
