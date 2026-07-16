const state = {
    view: "status",
    titles: {
        status: "System status",
        actor: "Current actor",
        booking: "Booking detail",
        discrepancies: "Discrepancies",
        timeline: "Booking timeline"
    }
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
    element("loadBooking").addEventListener("click", loadBooking);
    element("loadDiscrepancies").addEventListener("click", loadDiscrepancies);
    element("loadDiscrepancyDetail").addEventListener("click", loadDiscrepancyDetail);
    element("loadTimeline").addEventListener("click", loadTimeline);
}

loadActorSettings();
bindEvents();
loadStatus();
