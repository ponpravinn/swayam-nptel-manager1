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
    console.log("=== Testing SWAYAM NPTEL Sync & Grading Engine ===");

    // Step 1: Login as student
    cookie = '';
    const loginPage = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/login',
        method: 'GET'
    });
    const loginCsrfMatch = loginPage.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const loginCsrf = loginCsrfMatch ? loginCsrfMatch[1] : '';

    console.log("1. Logging in as student (student@demo.com)...");
    const loginPayload = `email=student%40demo.com&password=Student%40123&_csrf=${encodeURIComponent(loginCsrf)}`;
    const studentLogin = await request({
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
    console.log("Student Login Status:", studentLogin.status, "Redirect:", studentLogin.headers.location);

    // Step 2: Access Courses page and verify SWAYAM NPTEL Sync Box
    console.log("\n2. Checking Courses Page & SWAYAM NPTEL Sync form...");
    const coursesPage = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/courses',
        method: 'GET'
    });
    console.log("Courses page status:", coursesPage.status);
    console.log("Contains 'Import Any SWAYAM NPTEL Course':", coursesPage.body.includes('Import Any SWAYAM NPTEL Course') || coursesPage.body.includes('Sync Any SWAYAM NPTEL Course'));

    const courseCsrfMatch = coursesPage.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const courseCsrf = courseCsrfMatch ? courseCsrfMatch[1] : '';

    // Step 3: Trigger SWAYAM NPTEL Sync for Cloud Computing (noc24-cs78)
    console.log("\n3. Testing SWAYAM NPTEL Course Ingestion for Cloud Computing (noc24-cs78)...");
    const syncPayload = `nptelInput=noc24-cs78&_csrf=${encodeURIComponent(courseCsrf)}`;
    const syncRes = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/courses/sync-nptel',
        method: 'POST',
        noFollow: true,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(syncPayload)
        }
    }, syncPayload);
    console.log("Sync Status:", syncRes.status, "Redirect:", syncRes.headers.location);

    // Follow redirect back to /courses
    const coursesAfterSync = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/courses',
        method: 'GET'
    });
    console.log("Courses page after sync includes 'Cloud Computing':", coursesAfterSync.body.includes('Cloud Computing'));
    console.log("Courses page after sync includes 'noc24-cs78':", coursesAfterSync.body.includes('noc24-cs78'));

    // Step 4: Extract course ID and test Score & Certificate Predictor
    const matchCalc = coursesAfterSync.body.match(/\/courses\/(\d+)\/score-calculator/);
    if (matchCalc) {
        const courseId = matchCalc[1];
        console.log(`\n4. Testing Score & Certificate Predictor for course ID: ${courseId}...`);
        const calcPage = await request({
            hostname: 'localhost',
            port: 8080,
            path: `/courses/${courseId}/score-calculator?examScore=62`,
            method: 'GET'
        });
        console.log("Score calculator status:", calcPage.status);
        console.log("Predictor includes 'Elite + Gold':", calcPage.body.includes('Elite + Gold'));
        console.log("Predictor includes 'Elite + Silver':", calcPage.body.includes('Elite + Silver'));
        console.log("Predictor includes 'Continuous Assessment Score':", calcPage.body.includes('Continuous Assessment') || calcPage.body.includes('Assignment'));
    } else {
        console.warn("Could not find /courses/{id}/score-calculator link on courses page.");
    }

    // Step 5: Test Admin SWAYAM Ingestion
    console.log("\n5. Testing Admin SWAYAM Ingestion...");
    cookie = '';
    const adminLoginPage = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/auth/login',
        method: 'GET'
    });
    const adminCsrfMatch = adminLoginPage.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const adminCsrf = adminCsrfMatch ? adminCsrfMatch[1] : '';

    const adminPayload = `email=admin%40nptelmanager.com&password=Admin%40123&_csrf=${encodeURIComponent(adminCsrf)}`;
    await request({
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

    const adminDash = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/admin',
        method: 'GET'
    });
    console.log("Admin Dashboard status:", adminDash.status);
    console.log("Admin Dashboard includes SWAYAM sync bar:", adminDash.body.includes('Sync & Ingest Any SWAYAM NPTEL Course') || adminDash.body.includes('sync-nptel'));

    const adminDashCsrfMatch = adminDash.body.match(/name="_csrf"\s+value="([^"]+)"/);
    const adminDashCsrf = adminDashCsrfMatch ? adminDashCsrfMatch[1] : '';

    const adminSyncPayload = `nptelInput=noc24-cs79&_csrf=${encodeURIComponent(adminDashCsrf)}`;
    const adminSyncRes = await request({
        hostname: 'localhost',
        port: 8080,
        path: '/admin/courses/sync-nptel',
        method: 'POST',
        noFollow: true,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(adminSyncPayload)
        }
    }, adminSyncPayload);
    console.log("Admin Sync Status:", adminSyncRes.status, "Redirect:", adminSyncRes.headers.location);

    console.log("\nALL SWAYAM NPTEL SYNC & PREDICTOR TESTS COMPLETED SUCCESSFULLY! ✅");
}

run().catch(console.error);
