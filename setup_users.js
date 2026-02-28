const http = require('http');

async function run() {
    const loginRes = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: "daradebhushan15+admin@gmail.com", password: "Bbd@123" })
    });
    const loginData = await loginRes.json();
    const token = loginData.data.token;

    const deptRes = await fetch("http://localhost:8080/api/admin/departments", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
        body: JSON.stringify({ name: "E2E Roles Dept", nameMr: "E2E भूमिका विभाग" })
    });
    const deptData = await deptRes.json();
    const deptId = deptData.id || deptData.data?.id; // backend returns entire dept object or data wrapper depending on config, but it's likely just the object since spring data rest

    if (!deptId) {
        console.log("Dept creation failed or id not found:", deptData);
        // return; // Don't return, let's just attempt 1 for fallback if needed, or query existing.
    }

    // The dept object is usually returned directly in Spring Boot standard controllers
    console.log("Created Dept ID:", deptId);

    const headRes = await fetch("http://localhost:8080/api/admin/users", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
        body: JSON.stringify({
            name: "E2E Head User", email: "head_test@loknagar.com", mobile: "8888888888",
            password: "Password@123", role: "DEPARTMENT_HEAD", departmentId: deptId
        })
    });
    const headData = await headRes.json();
    console.log("Head Create Res:", headData);

    const staffRes = await fetch("http://localhost:8080/api/admin/users", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
        body: JSON.stringify({
            name: "E2E Staff User", email: "staff_test@loknagar.com", mobile: "7777777777",
            password: "Password@123", role: "STAFF", departmentId: deptId
        })
    });
    const staffData = await staffRes.json();
    console.log("Staff Create Res:", staffData);
}

run();
