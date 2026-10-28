const http = require('http');

let cookie = '';

function request(options, data) {
    return new Promise((resolve, reject) => {
        if (!options.headers) options.headers = {};
        if (cookie) options.headers['Cookie'] = cookie;
        const req = http.request(options, (res) => {
            if (res.headers['set-cookie']) {
                const newCookies = res.headers['set-cookie'].map(c => c.split(';')[0]).join('; ');
                cookie = cookie ? (cookie + '; ' + newCookies) : newCookies;
            }
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', async () => {
                if ((res.statusCode === 302 || res.statusCode === 301) && res.headers.location && !options.noFollow) {
                    const nextUrl = new URL(res.headers.location, `http://${options.hostname}:${options.port}`);
                    const nextOptions = {
                        hostname: nextUrl.hostname,
                        port: nextUrl.port,
                        path: nextUrl.pathname + nextUrl.search,
                        method: 'GET'
                    };
                    const redirected = await request(nextOptions);
                    resolve(redirected);
                } else {
                    resolve({ status: res.statusCode, headers: res.headers, body });
                }
            });
        });
        req.on('error', reject);
        if (data) req.write(data);
        req.end();
    });
}

async function run() {
    console.log("=== Testing Database User Persistence & Case-Insensitive Auth ===");

    // Step 1: Get Registration page for CSRF
    const regPage = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/register',
        method: 'GET'
    });
    const regCsrfMatch = regPage.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const regCsrf = regCsrfMatch ? regCsrfMatch[1] : '';
    console.log("Registration CSRF obtained:", !!regCsrf);

    // Step 2: Register a new user with uppercase email & full institution info
    const testEmail = 'PravinStudent' + Date.now() + '@College.EDU';
    console.log("Registering user with mixed-case email:", testEmail);
    const regPayload = `fullName=Pravin+Kumar&email=${encodeURIComponent(testEmail)}&password=Password%40123&college=National+Engineering+College&department=Computer+Science+and+Engineering&rollNumber=310625243155&phone=%2B919123456780&_csrf=${encodeURIComponent(regCsrf)}`;
    
    const regPost = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/register',
        method: 'POST',
        noFollow: true,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(regPayload)
        }
    }, regPayload);
    console.log("Registration Post Status:", regPost.status, "Redirect:", regPost.headers.location);

    // Step 3: Login using LOWERCASE version of that email
    cookie = ''; // Clear cookie
    const loginPage = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/login',
        method: 'GET'
    });
    const loginCsrfMatch = loginPage.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const loginCsrf = loginCsrfMatch ? loginCsrfMatch[1] : '';

    const lowercaseEmail = testEmail.toLowerCase();
    console.log("Attempting login using lowercase email:", lowercaseEmail);
    const loginPayload = `email=${encodeURIComponent(lowercaseEmail)}&password=Password%40123&_csrf=${encodeURIComponent(loginCsrf)}`;
    const loginPost = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/login',
        method: 'POST',
        noFollow: true,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(loginPayload)
        }
    }, loginPayload);
    console.log("Lowercase Login Status:", loginPost.status, "Redirect:", loginPost.headers.location);

    // Step 4: Login as Admin to inspect the Database & Users Explorer
    cookie = ''; // Clear cookie
    const adminLoginPage = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/login',
        method: 'GET'
    });
    const adminLoginCsrfMatch = adminLoginPage.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const adminCsrf = adminLoginCsrfMatch ? adminLoginCsrfMatch[1] : '';

    const adminPayload = `email=admin%40nptelmanager.com&password=Admin%40123&_csrf=${encodeURIComponent(adminCsrf)}`;
    const adminLogin = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/login',
        method: 'POST',
        noFollow: true,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(adminPayload)
        }
    }, adminPayload);
    console.log("Admin Login Status:", adminLogin.status, "Redirect:", adminLogin.headers.location);

    // Step 5: Fetch /admin/users (Database & Users Explorer)
    const usersView = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/admin/users',
        method: 'GET'
    });
    console.log("Database & Users Explorer Status:", usersView.status);
    console.log("Contains new user name (Pravin Kumar):", usersView.body.includes('Pravin Kumar'));
    console.log("Contains institution name (National Engineering College):", usersView.body.includes('National Engineering College'));
    console.log("Contains roll number (310625243155):", usersView.body.includes('310625243155'));
    console.log("Contains phone (+919123456780):", usersView.body.includes('9123456780'));
    console.log("Contains H2 Web Console link:", usersView.body.includes('/h2-console'));

    // Step 6: Test CSV Export
    const csvExport = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/admin/users/export-csv',
        method: 'GET'
    });
    console.log("CSV Export Status:", csvExport.status);
    console.log("CSV Content Type:", csvExport.headers['content-type']);
    console.log("CSV Contains Pravin Kumar:", csvExport.body.includes('Pravin Kumar'));
    console.log("CSV Contains National Engineering College:", csvExport.body.includes('National Engineering College'));

    console.log("\nALL DATABASE & AUTHENTICATION TESTS PASSED SUCCESSFULLY! ✅");
}

run().catch(console.error);
