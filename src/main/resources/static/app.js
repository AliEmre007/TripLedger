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

const scenarioLabels = {
    "DEMO-EXACT-1001": {
        label: "Exact match",
        description: "Clean case: expected sale and matched payment should agree."
    },
    "DEMO-OTA-1002": {
        label: "OTA settlement",
        description: "Booking paid through a channel settlement with deductions."
    },
    "DEMO-CANCEL-1003": {
        label: "Cancellation/refund",
        description: "Cancelled booking used to inspect refund behavior."
    },
    "DEMO-AMB-1004": {
        label: "Ambiguous payment",
        description: "Payment evidence may not identify one booking cleanly."
    },
    "DEMO-FX-1005": {
        label: "FX case",
        description: "Booking uses currency conversion evidence."
    },
    "DEMO-SHORT-1006": {
        label: "Short settlement",
        description: "Matched money may be lower than expected."
    }
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

function jsonDetails(title, value) {
    return `
        <details class="technical-details">
            <summary>${escapeHtml(title)}</summary>
            <pre class="json-view">${escapeHtml(JSON.stringify(value, null, 2))}</pre>
        </details>
    `;
}

function renderError(target, error) {
    renderJson(target, {
        status: error.status || "REQUEST_FAILED",
        error: error.body || {message: error.message}
    });
}

function renderInstruction(target, title, message) {
    target.innerHTML = `
        <div class="instruction-box">
            <strong>${escapeHtml(title)}</strong>
            <span>${escapeHtml(message)}</span>
        </div>
    `;
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
        target.innerHTML = `
            <div class="summary-callout status-up">
                <strong>Demo sources are ready.</strong>
                <span>TripLedger now knows which demo systems send bookings, supplier costs, and payment evidence.</span>
            </div>
            <div class="activity-list">
                ${sourceReadyItem("OTA booking source", systems.ota)}
                ${sourceReadyItem("Supplier source", systems.supplier)}
                ${sourceReadyItem("Payment source", systems.payment)}
            </div>
            <div class="next-action">Next: click Import demo data to load the bundled CSV fixtures.</div>
        `;
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
            renderInstruction(
                    target,
                    "No bookings found.",
                    "Run Import demo data first, then this table will show booking references, service dates, status, value, and action buttons."
            );
            return;
        }

        target.innerHTML = `
            <div class="summary-callout">
                <strong>${escapeHtml(state.bookings.length)} demo bookings are available.</strong>
                <span>Select a row action to inspect the booking, explain economics, run matching, or reconcile money.</span>
            </div>
            <table class="data-table booking-table">
                <thead>
                    <tr>
                        <th>Booking</th>
                        <th>Scenario</th>
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
    const scenario = scenarioLabels[booking.externalBookingId] || {
        label: "Demo booking",
        description: "Imported booking record."
    };
    return `
        <tr>
            <td>
                <strong>${escapeHtml(booking.externalBookingId)}</strong>
                <span class="muted-line">${escapeHtml(booking.customerReference || shortId(booking.id))}</span>
            </td>
            <td>
                <span class="scenario-pill">${escapeHtml(scenario.label)}</span>
                <span class="muted-line">${escapeHtml(scenario.description)}</span>
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
        renderControlResult(target, action, result);
    } catch (error) {
        renderError(target, error);
    }
}

function sourceReadyItem(label, source) {
    return `
        <div class="activity-item">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(source.externalCode)}</strong>
            <small>${escapeHtml(source.category)} source is active.</small>
            <span class="status-pill status-up">READY</span>
        </div>
    `;
}

function renderImportResults(results) {
    const accepted = sumResults(results, "acceptedCount");
    const rejected = sumResults(results, "rejectedCount");
    const duplicates = sumResults(results, "duplicateCount");
    const title = rejected > 0 ? "Import finished with rejected rows."
        : accepted > 0 ? "Demo data imported successfully."
            : "Demo data was already imported.";
    const message = rejected > 0
        ? `${rejected} row(s) need review before the dataset is fully clean.`
        : accepted > 0
            ? `${accepted} new row(s) were accepted. ${duplicates} duplicate row(s) were safely skipped.`
            : `${duplicates} row(s) already existed, so TripLedger did not create duplicate records.`;

    element("demoActivity").innerHTML = `
        <div class="summary-callout ${rejected > 0 ? "status-down" : "status-up"}">
            <strong>${escapeHtml(title)}</strong>
            <span>${escapeHtml(message)}</span>
        </div>
        <div class="activity-list">
            ${results.map(({label, result}) => `
                <div class="activity-item">
                    <span>${escapeHtml(label)}</span>
                    <strong>${escapeHtml(result.status || "IMPORTED")}</strong>
                    <small>${escapeHtml(importResultSentence(label, result))}</small>
                    <button class="secondary-button compact-button" data-batch-id="${escapeHtml(result.importBatchId || result.id || "")}" type="button">Rows</button>
                </div>
            `).join("")}
        </div>
        <div class="next-action">Next: choose a booking below, then run Economics, Match, and Reconcile.</div>
    `;
    element("demoActivity").querySelectorAll("[data-batch-id]").forEach((button) => {
        button.addEventListener("click", () => loadImportRows(button.dataset.batchId));
    });
}

function renderControlResult(target, action, result) {
    if (action === "economics") {
        renderEconomicsSummary(target, result);
    } else if (action === "matching") {
        renderMatchingSummary(target, result);
    } else if (action === "reconciliation") {
        renderReconciliationSummary(target, result);
    } else {
        renderJson(target, result);
    }
}

function renderEconomicsSummary(target, result) {
    const components = result.components || [];
    const sale = sumComponents(components, "CONTRACTED_GROSS_SALE");
    const supplierCost = sumComponents(components, "ACTIVE_SUPPLIER_COST");
    const deductions = sumComponents(components, "EXPECTED_CHANNEL_COMMISSION")
        + sumComponents(components, "EXPECTED_PAYMENT_FEE");
    const margin = sale - deductions - supplierCost;
    const currency = result.currency || firstCurrency(components) || "";

    target.innerHTML = `
        <div class="control-summary">
            <div class="summary-callout">
                <strong>Economics explained for this booking.</strong>
                <span>TripLedger calculated sale, expected deductions, supplier cost, and estimated margin from stored evidence.</span>
            </div>
            <div class="summary-metrics">
                ${summaryMetric("Sale", money(sale, currency))}
                ${summaryMetric("Deductions", money(deductions, currency))}
                ${summaryMetric("Supplier cost", money(supplierCost, currency))}
                ${summaryMetric("Estimated margin", money(margin, currency))}
            </div>
            ${jsonDetails("Technical economics evidence", result)}
        </div>
    `;
}

function renderMatchingSummary(target, result) {
    const matched = result.status === "ACTIVE" && result.matchId;
    target.innerHTML = `
        <div class="control-summary">
            <div class="summary-callout ${matched ? "status-up" : "status-down"}">
                <strong>${escapeHtml(matched ? "A financial event was matched." : "No active match was created.")}</strong>
                <span>${escapeHtml(matchingSentence(result))}</span>
            </div>
            <div class="summary-metrics">
                ${summaryMetric("Rule", result.ruleCode || "UNKNOWN")}
                ${summaryMetric("Amount", money(result.amount, result.currency))}
                ${summaryMetric("Status", result.status || "UNKNOWN")}
            </div>
            ${jsonDetails("Technical matching result", result)}
        </div>
    `;
}

function renderReconciliationSummary(target, result) {
    const reconciled = result.status === "RECONCILED";
    target.innerHTML = `
        <div class="control-summary">
            <div class="summary-callout ${reconciled ? "status-up" : "status-down"}">
                <strong>${escapeHtml(reconciled ? "Booking is reconciled." : "Booking is not fully reconciled.")}</strong>
                <span>${escapeHtml(reconciliationSentence(result))}</span>
            </div>
            <div class="summary-metrics">
                ${summaryMetric("Expected", money(result.expectedAmount, result.currency))}
                ${summaryMetric("Matched", money(result.matchedAmount, result.currency))}
                ${summaryMetric("Variance", money(result.varianceAmount, result.currency))}
                ${summaryMetric("Status", result.status || "UNKNOWN")}
            </div>
            ${jsonDetails("Technical reconciliation result", result)}
        </div>
    `;
}

function importResultSentence(label, result) {
    const accepted = result.acceptedCount ?? 0;
    const rejected = result.rejectedCount ?? 0;
    const duplicate = result.duplicateCount ?? 0;
    if (rejected > 0) {
        return `${label}: ${accepted} accepted, ${rejected} rejected, ${duplicate} duplicates skipped.`;
    }
    if (accepted > 0) {
        return `${label}: ${accepted} new row(s) accepted and ${duplicate} duplicate row(s) skipped.`;
    }
    if (duplicate > 0) {
        return `${label}: all ${duplicate} row(s) were already present, so no duplicates were created.`;
    }
    return `${label}: no rows changed.`;
}

function matchingSentence(result) {
    if (result.status === "ACTIVE" && result.matchId) {
        return `Matched ${money(result.amount, result.currency)} using rule ${result.ruleCode || "UNKNOWN"}.`;
    }
    return result.reason || "TripLedger did not find enough evidence for an active match.";
}

function reconciliationSentence(result) {
    if (result.status === "RECONCILED") {
        return `Expected and matched money agree. Variance is ${money(result.varianceAmount, result.currency)}.`;
    }
    return `Expected ${money(result.expectedAmount, result.currency)}, matched ${money(result.matchedAmount, result.currency)}, variance ${money(result.varianceAmount, result.currency)}.`;
}

function summaryMetric(label, value) {
    return `
        <div class="summary-metric">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(value)}</strong>
        </div>
    `;
}

function sumResults(results, field) {
    return results.reduce((total, item) => total + (item.result[field] || 0), 0);
}

function sumComponents(components, type) {
    return components
        .filter((component) => component.componentType === type)
        .reduce((total, component) => total + Number(component.amount || 0), 0);
}

function firstCurrency(components) {
    const component = components.find((item) => item.currency);
    return component ? component.currency : "";
}

function money(amount, currency) {
    if (amount === null || amount === undefined || Number.isNaN(Number(amount))) {
        return currency || "UNKNOWN";
    }
    const rounded = Number(amount).toLocaleString(undefined, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    return `${rounded} ${currency || ""}`.trim();
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
