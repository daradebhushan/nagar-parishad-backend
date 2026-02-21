const https = require('https');
const http = require('http');

// Configuration
const BASE_URL = 'http://localhost:8080/api'; // Assuming backend is on 8080
const EMAIL = 'notify@test.com';
const PASSWORD = 'password';

async function post(path, body, token = null) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'localhost',
            port: 8080,
            path: '/api' + path,
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': 'Bearer ' + token } : {})
            }
        };

        const req = http.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    console.error('Error parsing JSON:', data);
                    resolve({ error: 'Parse Error', raw: data });
                }
            });
        });

        req.on('error', (e) => reject(e));
        req.write(JSON.stringify(body));
        req.end();
    });
}

async function get(path, token) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'localhost',
            port: 8080,
            path: '/api' + path,
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        };

        const req = http.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    console.error('Error parsing JSON:', data);
                    resolve({ error: 'Parse Error', raw: data });
                }
            });
        });

        req.on('error', (e) => reject(e));
        req.end();
    });
}

async function run() {
    console.log('1. Logging in...');
    const loginRes = await post('/auth/login', { email: EMAIL, password: PASSWORD });

    if (!loginRes.success || !loginRes.data || !loginRes.data.token) {
        console.error('Login Failed:', loginRes);
        return;
    }

    const token = loginRes.data.token;
    console.log('Login Successful. Token obtained.');

    console.log('\n2. Fetching Unread Count...');
    const countRes = await get('/notifications/unread-count', token);
    console.log('Count Response:', JSON.stringify(countRes, null, 2));

    console.log('\n3. Fetching Notifications List...');
    const listRes = await get('/notifications', token);
    console.log('Lists Response:', JSON.stringify(listRes, null, 2));
}

run();
