const apiUrl = 'http://localhost:8080/api';

async function testPlusEmail() {
    console.log('1. Authenticating as CO/Admin...');
    const loginRes = await fetch(`${apiUrl}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'daradebhushan15+admin@gmail.com', password: 'Bbd@123' })
    });
    const loginData = await loginRes.json();
    const token = loginData.data ? loginData.data.token : loginData.token;
    const headers = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };

    console.log('2. Trying to create user with + alias...');
    const newUserRes = await fetch(`${apiUrl}/admin/users`, {
        method: 'POST', headers,
        body: JSON.stringify({
            name: 'Test Alias User',
            email: 'daradebhushan15+456@gmail.com',
            mobile: '8888888888',
            password: 'StrongPassword123!',
            role: 'STAFF',
            designation: 'Tester'
        })
    });
    const user = await newUserRes.json();
    console.log('API Response:', JSON.stringify(user, null, 2));

    // If created, delete it immediately to clean up
    if (user.success) {
        console.log('3. Deleting newly created user...');
        const userId = user.data.id;
        await fetch(`${apiUrl}/admin/users/${userId}`, { method: 'DELETE', headers });
        console.log('User deleted.');
    }

}
testPlusEmail().catch(console.error);
