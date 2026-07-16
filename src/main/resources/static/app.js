const state = {
    view: "status",
    titles: {
        status: "System status",
        actor: "Current actor",
        demo: "Demo workflow",
        booking: "Booking detail",
        discrepancies: "Discrepancies",
        timeline: "Booking timeline"
    },
    demoSources: {},
    bookings: []
};

const actorDefaults = {
    actorSubject: "local-admin",
    organisationId: "11111111-1111-1111-1111-111111111111",
    mfaSatisfied: "true"
};

function element(id) {
    return document.getElementById(id);
}

function actorHeaders() {
    return {
        "X-TripLedger-Actor-Subject": element("actorSubject").value.trim(),
        "X-TripLedger-Organisation-Id": element("organisationId").value.trim(),
        "X-TripLedger-Mfa-Satisfied": element("mfaSatisfied").value
    };
}

function jsonHeaders() {
    return {
        ...actorHeaders(),
        "Content-Type": "application/json"
    };
}

async function requestJson(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: {
            "Accept": "application/json",
            ...(options.headers || {})
        }
    });

    let body = null;
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        body = await response.json();
    } else {
        const text = await response.text();
        body = text ? {message: text} : null;
    }

    if (!response.ok) {
        const error = new Error(`HTTP ${response.status}`);
        error.status = response.status;
        error.body = body;
        throw error;
    }

    return body;
}

async function postJson(path, body) {
    return requestJson(path, {
        method: "POST",
        headers: jsonHeaders(),
        body: JSON.stringify(body)
    });
}

function renderJson(target, value) {
    target.innerHTML = "";
    const pre = document.createElement("pre");
    pre.className = "json-view";
    pre.textContent = JSON.stringify(value, null, 2);
    target.appendChild(pre);
}

function renderError(target, error) {
    renderJson(target, {
        status: error.status || "REQUEST_FAILED",
        error: error.body || {message: error.message}
    });
}

function statusClass(status) {
    return status === "UP" ? "status-up" : "status-down";
}

function setStatusText(id, text, className) {
    const target = element(id);
    target.textContent = text;
    target.className = className || "";
}

async function loadStatus() {
    const readinessTarget = element("readinessChecks");
    readinessTarget.textContent = "Loading readiness...";

    try {
        const [health, readiness, actuator] = await Promise.all([
            requestJson("/api/v1/health"),
            requestJson("/api/v1/health/ready"),
            requestJson("/actuator/health")
        ]);

        setStatusText("appStatus", health.status, statusClass(health.status));
        element("appVersion").textContent = `${health.service} ${health.version}`;
        setStatusText("readyStatus", readiness.status, statusClass(readiness.status));
        element("readyTimestamp").textContent = readiness.timestamp;
        setStatusText("actuatorStatus", actuator.status, statusClass(actuator.status));

        if (!readiness.checks || readiness.checks.length === 0) {
            readinessTarget.textContent = "No readiness checks returned.";
            return;
        }

        const rows = readiness.checks.map((check) => `
            <tr>
                <td>${escapeHtml(check.name)}</td>
                <td><span class="status-pill ${statusClass(check.status)}">${escapeHtml(check.status)}</span></td>
                <td>${escapeHtml(check.message || "")}</td>
            </tr>
        `).join("");

        readinessTarget.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr><th>Check</th><th>Status</th><th>Message</th></tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        `;
    } catch (error) {
        setStatusText("appStatus", "Down", "status-down");
        setStatusText("readyStatus", "Down", "status-down");
        setStatusText("actuatorStatus", "Down", "status-down");
        renderError(readinessTarget, error);
    }
}

async function loadActor() {
    const target = element("actorResult");
    target.textContent = "Loading actor...";

    try {
        const actor = await requestJson("/api/v1/me", {headers: actorHeaders()});
        renderJson(target, actor);
    } catch (error) {
        renderError(target, error);
    }
}

async function loadDemoOverview() {
    await Promise.all([
        loadDemoSources(),
        loadDemoBookings()
    ]);
}

async function loadDemoSources() {
    const target = element("demoSources");
    target.textContent = "Loading source systems...";

    try {
        const systems = await requestJson("/api/v1/source-systems", {headers: actorHeaders()});
        updateDemoSourceState(systems);
        if (!systems || systems.length === 0) {
            target.textContent = "No source systems found.";
            return;
        }

        target.innerHTML = `
            <table class="data-table compact-table">
                <thead>
                    <tr><th>Name</th><th>Category</th><th>Code</th><th>Status</th></tr>
                </thead>
                <tbody>
                    ${systems.map((system) => `
                        <tr>
                            <td>${escapeHtml(system.name)}</td>
                            <td>${escapeHtml(system.category)}</td>
                            <td>${escapeHtml(system.externalCode)}</td>
                            <td><span class="status-pill ${system.active ? "status-up" : "status-down"}">${system.active ? "ACTIVE" : "INACTIVE"}</span></td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        `;
    } catch (error) {
        renderError(target, error);
    }
}

async function prepareDemoSources() {
    const target = element("demoActivity");
    target.textContent = "Preparing demo source systems...";

    try {
        const systems = await ensureDemoSources();
        await loadDemoSources();
        renderDemoActivity([
            {label: "OTA booking source", value: systems.ota.id},
            {label: "Supplier source", value: systems.supplier.id},
            {label: "Payment source", value: systems.payment.id}
        ]);
    } catch (error) {
        renderError(target, error);
    }
}

async function ensureDemoSources() {
    const existing = await requestJson("/api/v1/source-systems", {headers: actorHeaders()});
    updateDemoSourceState(existing);

    const specs = {
        ota: {
            name: "Demo OTA",
            category: "BOOKING_CHANNEL",
            externalCode: "OTA-DEMO",
            timeZone: "Europe/Istanbul",
            active: true
        },
        supplier: {
            name: "Demo Supplier",
            category: "SUPPLIER",
            externalCode: "SUPPLIER-DEMO",
            timeZone: "Europe/Istanbul",
            active: true
        },
        payment: {
            name: "Demo Payments",
            category: "PAYMENT_PROVIDER",
            externalCode: "PAYMENT-DEMO",
            timeZone: "Europe/Istanbul",
            active: true
        }
    };

    for (const [key, spec] of Object.entries(specs)) {
        if (!state.demoSources[key]) {
            state.demoSources[key] = await createSourceSystem(spec);
        }
    }

    return state.demoSources;
}

async function createSourceSystem(spec) {
    try {
        return await postJson("/api/v1/source-systems", spec);
    } catch (error) {
        if (error.status !== 409) {
            throw error;
        }
        const systems = await requestJson("/api/v1/source-systems", {headers: actorHeaders()});
        const existing = systems.find((system) => system.externalCode === spec.externalCode);
        if (!existing) {
            throw error;
        }
        return existing;
    }
}

function updateDemoSourceState(systems) {
    state.demoSources = {
        ota: (systems || []).find((system) => system.externalCode === "OTA-DEMO") || state.demoSources.ota,
        supplier: (systems || []).find((system) => system.externalCode === "SUPPLIER-DEMO") || state.demoSources.supplier,
        payment: (systems || []).find((system) => system.externalCode === "PAYMENT-DEMO") || state.demoSources.payment
    };
}

async function runAllDemoImports() {
    const target = element("demoActivity");
    target.textContent = "Running demo imports...";

    try {
        const results = [];
        results.push(await runDemoImport("bookings", false));
        results.push(await runDemoImport("supplier", false));
        results.push(await runDemoImport("financial", false));
        renderImportResults(results);
        await Promise.all([loadDemoSources(), loadDemoBookings(), loadDiscrepancies()]);
    } catch (error) {
        renderError(target, error);
    }
}

async function runSingleDemoImport(kind) {
    const target = element("demoActivity");
    target.textContent = `Importing ${kind} demo data...`;

    try {
        const result = await runDemoImport(kind, true);
        renderImportResults([result]);
        await loadDemoBookings();
    } catch (error) {
        renderError(target, error);
    }
}

async function runDemoImport(kind, renderResult) {
    const sources = await ensureDemoSources();
    const imports = {
        bookings: {
            label: "Bookings",
            path: "/api/v1/booking-imports",
            file: "/demo/bookings.csv",
            fileName: "bookings.csv",
            sourceSystemId: sources.ota.id
        },
        supplier: {
            label: "Supplier obligations",
            path: "/api/v1/supplier-obligation-imports",
            file: "/demo/supplier_obligations.csv",
            fileName: "supplier_obligations.csv",
            sourceSystemId: sources.supplier.id
        },
        financial: {
            label: "Financial events",
            path: "/api/v1/financial-event-imports",
            file: "/demo/financial_events.csv",
            fileName: "financial_events.csv",
            sourceSystemId: sources.ota.id
        }
    };
    const config = imports[kind];
    const csvContent = await fetchText(config.file);
    const result = await postJson(config.path, {
        sourceSystemId: config.sourceSystemId,
        fileName: config.fileName,
        fileChecksum: await checksum(csvContent),
        csvContent
    });
    const summary = {label: config.label, result};
    if (renderResult) {
        renderImportResults([summary]);
    }
    return summary;
}

async function fetchText(path) {
    const response = await fetch(path, {headers: {"Accept": "text/csv"}});
    if (!response.ok) {
        throw new Error(`Could not load ${path}`);
    }
    return response.text();
}

async function checksum(text) {
    if (!window.crypto || !window.crypto.subtle) {
        return `length:${text.length}`;
    }
    const data = new TextEncoder().encode(text);
    const digest = await window.crypto.subtle.digest("SHA-256", data);
    const hex = Array.from(new Uint8Array(digest))
        .map((byte) => byte.toString(16).padStart(2, "0"))
        .join("");
    return `sha256:${hex}`;
}

async function loadDemoBookings() {
    const target = element("demoBookings");
    target.textContent = "Loading bookings...";

    try {
        const bookings = await requestJson("/api/v1/bookings", {headers: actorHeaders()});
        state.bookings = bookings || [];
        if (state.bookings.length === 0) {
            target.textContent = "No bookings found. Run the demo import first.";
            return;
        }

        target.innerHTML = `
            <table class="data-table booking-table">
                <thead>
                    <tr>
                        <th>Booking</th>
                        <th>Service</th>
                        <th>Status</th>
                        <th>Value</th>
                        <th>Controls</th>
                    </tr>
                </thead>
                <tbody>
                    ${state.bookings.map((booking) => demoBookingRow(booking)).join("")}
                </tbody>
            </table>
        `;
        bindBookingActions(target);
    } catch (error) {
        renderError(target, error);
    }
}

function demoBookingRow(booking) {
    const amount = booking.contractedSellingAmount ?? "";
    const currency = booking.sellingCurrency || "";
    return `
        <tr>
            <td>
                <strong>${escapeHtml(booking.externalBookingId)}</strong>
                <span class="muted-line">${escapeHtml(booking.customerReference || shortId(booking.id))}</span>
            </td>
            <td>${escapeHtml(booking.serviceStartDate || "")} to ${escapeHtml(booking.serviceEndDate || "")}</td>
            <td><span class="status-pill">${escapeHtml(booking.lifecycleStatus || "UNKNOWN")}</span></td>
            <td>${escapeHtml(amount)} ${escapeHtml(currency)}</td>
            <td>
                <div class="table-actions">
                    <button class="secondary-button compact-button" data-booking-action="detail" data-booking-id="${escapeHtml(booking.id)}" type="button">Open</button>
                    <button class="secondary-button compact-button" data-booking-action="timeline" data-booking-id="${escapeHtml(booking.id)}" type="button">Timeline</button>
                    <button class="secondary-button compact-button" data-booking-action="economics" data-booking-id="${escapeHtml(booking.id)}" type="button">Economics</button>
                    <button class="secondary-button compact-button" data-booking-action="matching" data-booking-id="${escapeHtml(booking.id)}" type="button">Match</button>
                    <button class="secondary-button compact-button" data-booking-action="reconciliation" data-booking-id="${escapeHtml(booking.id)}" type="button">Reconcile</button>
                </div>
            </td>
        </tr>
    `;
}

function bindBookingActions(target) {
    target.querySelectorAll("[data-booking-action]").forEach((button) => {
        button.addEventListener("click", () => runBookingAction(button.dataset.bookingId, button.dataset.bookingAction));
    });
}

async function runBookingAction(bookingId, action) {
    const target = element("demoActivity");
    target.textContent = `Running ${action} for ${shortId(bookingId)}...`;

    try {
        if (action === "detail") {
            element("bookingId").value = bookingId;
            switchView("booking");
            await loadBooking();
            return;
        }
        if (action === "timeline") {
            element("timelineBookingId").value = bookingId;
            switchView("timeline");
            await loadTimeline();
            return;
        }

        const paths = {
            economics: `/api/v1/bookings/${encodeURIComponent(bookingId)}/economics/explanation`,
            matching: `/api/v1/bookings/${encodeURIComponent(bookingId)}/matching-runs`,
            reconciliation: `/api/v1/bookings/${encodeURIComponent(bookingId)}/reconciliation-runs`
        };
        const options = action === "economics" ? {headers: actorHeaders()} : {
            method: "POST",
            headers: actorHeaders()
        };
        const result = await requestJson(paths[action], options);
        renderJson(target, result);
    } catch (error) {
        renderError(target, error);
    }
}

function renderDemoActivity(items) {
    element("demoActivity").innerHTML = `
        <div class="activity-list">
            ${items.map((item) => `
                <div class="activity-item">
                    <span>${escapeHtml(item.label)}</span>
                    <strong>${escapeHtml(shortId(item.value))}</strong>
                </div>
            `).join("")}
        </div>
    `;
}

function renderImportResults(results) {
    element("demoActivity").innerHTML = `
        <div class="activity-list">
            ${results.map(({label, result}) => `
                <div class="activity-item">
                    <span>${escapeHtml(label)}</span>
                    <strong>${escapeHtml(result.status || "IMPORTED")}</strong>
                    <small>accepted ${escapeHtml(result.acceptedCount ?? 0)} / rejected ${escapeHtml(result.rejectedCount ?? 0)} / duplicate ${escapeHtml(result.duplicateCount ?? 0)}</small>
                    <button class="secondary-button compact-button" data-batch-id="${escapeHtml(result.importBatchId || result.id || "")}" type="button">Rows</button>
                </div>
            `).join("")}
        </div>
    `;
    element("demoActivity").querySelectorAll("[data-batch-id]").forEach((button) => {
        button.addEventListener("click", () => loadImportRows(button.dataset.batchId));
    });
}

async function loadImportRows(batchId) {
    const target = element("demoActivity");
    if (!batchId) {
        target.innerHTML = '<div class="notice">Import batch id was not returned.</div>';
        return;
    }

    target.textContent = "Loading import row results...";
    try {
        const rows = await requestJson(`/api/v1/import-batches/${encodeURIComponent(batchId)}/row-results`, {
            headers: actorHeaders()
        });
        renderJson(target, rows);
    } catch (error) {
        renderError(target, error);
    }
}

async function loadBooking() {
    const target = element("bookingResult");
    const bookingId = element("bookingId").value.trim();
    if (!bookingId) {
        target.innerHTML = '<div class="notice">Booking id is required.</div>';
        return;
    }

    target.textContent = "Loading booking...";
    try {
        const booking = await requestJson(`/api/v1/bookings/${encodeURIComponent(bookingId)}`, {
            headers: actorHeaders()
        });
        renderJson(target, booking);
    } catch (error) {
        renderError(target, error);
    }
}

async function loadDiscrepancies() {
    const target = element("discrepancyList");
    const params = new URLSearchParams();
    const status = element("discrepancyStatus").value;
    const severity = element("discrepancySeverity").value;
    if (status) {
        params.set("status", status);
    }
    if (severity) {
        params.set("severity", severity);
    }

    target.textContent = "Loading discrepancies...";
    try {
        const path = `/api/v1/discrepancies${params.toString() ? `?${params}` : ""}`;
        const result = await requestJson(path, {headers: actorHeaders()});
        const rows = rowsFromDiscrepancyResponse(result);
        if (rows.length === 0) {
            target.textContent = "No discrepancies found.";
            return;
        }

        target.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Booking</th>
                        <th>Type</th>
                        <th>Severity</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${rows.map((item) => discrepancyRow(item)).join("")}
                </tbody>
            </table>
        `;

        target.querySelectorAll("[data-discrepancy-id]").forEach((button) => {
            button.addEventListener("click", () => {
                element("discrepancyId").value = button.dataset.discrepancyId;
                loadDiscrepancyDetail();
            });
        });
    } catch (error) {
        renderError(target, error);
    }
}

async function loadDiscrepancyDetail() {
    const target = element("discrepancyDetail");
    const discrepancyId = element("discrepancyId").value.trim();
    if (!discrepancyId) {
        target.innerHTML = '<div class="notice">Discrepancy id is required.</div>';
        return;
    }

    target.textContent = "Loading discrepancy...";
    try {
        const detail = await requestJson(`/api/v1/discrepancies/${encodeURIComponent(discrepancyId)}`, {
            headers: actorHeaders()
        });
        renderJson(target, detail);
    } catch (error) {
        renderError(target, error);
    }
}

async function loadTimeline() {
    const target = element("timelineResult");
    const bookingId = element("timelineBookingId").value.trim();
    if (!bookingId) {
        target.innerHTML = '<div class="notice">Booking id is required.</div>';
        return;
    }

    target.textContent = "Loading timeline...";
    try {
        const timeline = await requestJson(`/api/v1/bookings/${encodeURIComponent(bookingId)}/timeline`, {
            headers: actorHeaders()
        });
        const events = timeline.events || [];
        if (events.length === 0) {
            target.textContent = "No timeline events found.";
            return;
        }

        target.innerHTML = `
            <div class="timeline-list">
                ${events.map((event) => `
                    <div class="timeline-item">
                        <span class="timeline-time">${escapeHtml(event.occurredAt || "")}</span>
                        <div>
                            <strong>${escapeHtml(event.eventType || event.type || "Event")}</strong>
                            <span>${escapeHtml(event.category || "")}</span>
                            <pre class="json-view">${escapeHtml(JSON.stringify(event, null, 2))}</pre>
                        </div>
                    </div>
                `).join("")}
            </div>
        `;
    } catch (error) {
        renderError(target, error);
    }
}

function rowsFromDiscrepancyResponse(result) {
    if (Array.isArray(result)) {
        return result;
    }
    return result.items || result.discrepancies || result.content || [];
}

function discrepancyRow(item) {
    const id = item.discrepancyId || item.id || "";
    const bookingId = item.bookingId || "";
    const severity = item.severity || "";
    const severityClass = severity === "CRITICAL" ? "severity-critical"
        : severity === "WARNING" ? "severity-warning" : "severity-low";

    return `
        <tr>
            <td><button class="secondary-button" type="button" data-discrepancy-id="${escapeHtml(id)}">${escapeHtml(shortId(id))}</button></td>
            <td>${escapeHtml(shortId(bookingId))}</td>
            <td>${escapeHtml(item.type || item.discrepancyType || "")}</td>
            <td><span class="status-pill ${severityClass}">${escapeHtml(severity || "INFO")}</span></td>
            <td>${escapeHtml(item.status || "")}</td>
        </tr>
    `;
}

function shortId(value) {
    if (!value) {
        return "";
    }
    return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function saveActorSettings() {
    const settings = {
        actorSubject: element("actorSubject").value.trim(),
        organisationId: element("organisationId").value.trim(),
        mfaSatisfied: element("mfaSatisfied").value
    };
    localStorage.setItem("tripledger.validationActor", JSON.stringify(settings));
    loadActor();
}

function loadActorSettings() {
    const raw = localStorage.getItem("tripledger.validationActor");
    const settings = raw ? JSON.parse(raw) : actorDefaults;
    element("actorSubject").value = settings.actorSubject || actorDefaults.actorSubject;
    element("organisationId").value = settings.organisationId || actorDefaults.organisationId;
    element("mfaSatisfied").value = settings.mfaSatisfied || actorDefaults.mfaSatisfied;
}

function switchView(view) {
    state.view = view;
    document.querySelectorAll(".nav-item").forEach((item) => {
        item.classList.toggle("active", item.dataset.view === view);
    });
    document.querySelectorAll(".view").forEach((section) => {
        section.classList.toggle("active", section.id === `${view}View`);
    });
    element("viewTitle").textContent = state.titles[view];
}

function refreshCurrentView() {
    if (state.view === "status") {
        loadStatus();
    } else if (state.view === "actor") {
        loadActor();
    } else if (state.view === "demo") {
        loadDemoOverview();
    } else if (state.view === "booking") {
        loadBooking();
    } else if (state.view === "discrepancies") {
        loadDiscrepancies();
    } else if (state.view === "timeline") {
        loadTimeline();
    }
}

function bindEvents() {
    document.querySelectorAll(".nav-item").forEach((item) => {
        item.addEventListener("click", () => switchView(item.dataset.view));
    });
    element("refreshView").addEventListener("click", refreshCurrentView);
    element("saveActor").addEventListener("click", saveActorSettings);
    element("loadActor").addEventListener("click", loadActor);
    element("prepareDemoSources").addEventListener("click", prepareDemoSources);
    element("loadDemoSources").addEventListener("click", loadDemoSources);
    element("runDemoImports").addEventListener("click", runAllDemoImports);
    element("loadDemoBookings").addEventListener("click", loadDemoBookings);
    document.querySelectorAll("[data-import-kind]").forEach((button) => {
        button.addEventListener("click", () => runSingleDemoImport(button.dataset.importKind));
    });
    element("loadBooking").addEventListener("click", loadBooking);
    element("loadDiscrepancies").addEventListener("click", loadDiscrepancies);
    element("loadDiscrepancyDetail").addEventListener("click", loadDiscrepancyDetail);
    element("loadTimeline").addEventListener("click", loadTimeline);
}

loadActorSettings();
bindEvents();
loadStatus();
