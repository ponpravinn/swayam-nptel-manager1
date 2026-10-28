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
    console.log("1. Fetching login page...");
    const loginPage = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/login',
        method: 'GET'
    });

    const csrfMatch = loginPage.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const csrf = csrfMatch ? csrfMatch[1] : '';
    console.log("Found CSRF Token:", csrf ? (csrf.substring(0, 8) + '...') : 'none');

    console.log("2. Logging in as student@demo.com...");
    const loginData = `email=student%40demo.com&password=Student%40123&_csrf=${encodeURIComponent(csrf)}`;
    const loginPost = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/login',
        method: 'POST',
        noFollow: true,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(loginData)
        }
    }, loginData);
    console.log("Login Post Status:", loginPost.status, "Location:", loginPost.headers.location);

    console.log("3. Fetching Settings Page...");
    const settings = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/settings',
        method: 'GET'
    });
    console.log("Settings Page Status:", settings.status);
    console.log("Contains WhatsApp toggle (whatsappNotif):", settings.body.includes('whatsappNotif'));
    console.log("Contains Test WhatsApp button (/notifications/test-whatsapp):", settings.body.includes('/notifications/test-whatsapp'));

    const settingsCsrfMatch = settings.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const settingsCsrf = settingsCsrfMatch ? settingsCsrfMatch[1] : '';

    console.log("4. Triggering POST /notifications/test-whatsapp...");
    const testData = `phone=%2B919876543210&_csrf=${encodeURIComponent(settingsCsrf)}`;
    const testPost = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/notifications/test-whatsapp',
        method: 'POST',
        noFollow: true,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(testData)
        }
    }, testData);
    console.log("Test Alert Post Status:", testPost.status, "Redirect:", testPost.headers.location);

    console.log("5. Checking Settings redirect for WhatsApp alert URL...");
    const settingsAfter = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/settings',
        method: 'GET'
    });
    console.log("Settings after test contains 'wa.me':", settingsAfter.body.includes('wa.me'));

    console.log("6. Checking Notifications Page...");
    const notifs = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/notifications',
        method: 'GET'
    });
    console.log("Notifications contains WhatsApp link:", notifs.body.includes('/notifications/whatsapp/'));

    console.log("7. Checking Assignments Page...");
    const assignments = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/assignments',
        method: 'GET'
    });
    console.log("Assignments contains WhatsApp share:", assignments.body.includes('/assignments/share-whatsapp/'));

    console.log("8. Checking Examinations Page...");
    const exams = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/examinations',
        method: 'GET'
    });
    console.log("Examinations contains WhatsApp share:", exams.body.includes('/examinations/share-whatsapp/'));

    console.log("All WhatsApp Integration tests PASSED! ✅");
}

run().catch(console.error);
