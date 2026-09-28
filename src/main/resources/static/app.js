const API = '/api';
const state = { doctors: [], patients: [], slots: [], appointments: [] };
let csrfToken;
const viewNames = {
    overview: 'Overview',
    appointments: 'Appointments',
    schedule: 'Schedule & slots',
    doctors: 'Doctors',
    patients: 'Patients'
};

const byId = (id) => document.getElementById(id);

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (character) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[character]);
}

function displayDate(value, options = { month: 'short', day: 'numeric', year: 'numeric' }) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, options).format(date);
}

function displayTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? value
        : new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(date);
}

function initials(name) {
    return String(name || '?').replace(/^Dr\.?\s*/i, '').split(/\s+/).filter(Boolean)
        .slice(0, 2).map((word) => word[0]).join('').toUpperCase();
}

function toast(message, type = 'success') {
    const node = document.createElement('div');
    node.className = `toast ${type}`;
    node.textContent = message;
    byId('toast-region').append(node);
    window.setTimeout(() => node.remove(), 4500);
}

async function request(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers };
    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
        if (!csrfToken) {
            const csrfResponse = await fetch(`${API}/auth/csrf`, { credentials: 'same-origin' });
            if (!csrfResponse.ok) throw new Error('Unable to start a secure session. Please reload and try again.');
            csrfToken = (await csrfResponse.json()).token;
        }
        headers['X-CSRF-TOKEN'] = csrfToken;
    }
    const response = await fetch(`${API}${path}`, {
        ...options,
        credentials: 'same-origin',
        headers
    });
    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }
    if (!response.ok) {
        const validation = data?.errors && Object.values(data.errors).join(' · ');
        throw new Error(validation || data?.message || `Request failed (${response.status})`);
    }
    return data;
}

function setView(view, updateUrl = true) {
    if (!Object.hasOwn(viewNames, view)) view = 'overview';
    if (updateUrl && window.location.hash !== `#${view}`) {
        window.history.pushState(null, '', `#${view}`);
    }
    document.querySelectorAll('.view').forEach((section) => section.classList.toggle('active', section.id === `view-${view}`));
    document.querySelectorAll('.nav-link').forEach((link) => {
        const isActive = link.dataset.view === view;
        link.classList.toggle('active', isActive);
        if (isActive) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    });
    byId('page-crumb').textContent = viewNames[view] || 'Overview';
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (view === 'appointments') loadBookedAppointments();
    if (view === 'schedule') loadSlots('schedule');
    if (view === 'doctors') loadDoctors();
    if (view === 'patients') loadPatients();
}

async function loadDashboard() {
    const result = await request('/dashboard');
    byId('metric-doctors').textContent = result.doctorCount;
    byId('metric-patients').textContent = result.patientCount;
    byId('metric-open').textContent = result.availableSlotCount;
    byId('metric-booked').textContent = result.bookedSlotCount;
}

async function loadDoctors() {
    state.doctors = await request('/doctors');
    renderDoctors();
    refreshDoctorSelects();
    renderSpecializationOptions();
}

function refreshDoctorSelects() {
    const selectors = ['publish-doctor', 'overview-doctor-select'];
    selectors.forEach((id) => {
        const select = byId(id);
        const previous = select.value;
        const activeDoctors = state.doctors.filter((doctor) => doctor.active);
        select.innerHTML = activeDoctors.length
            ? activeDoctors.map((doctor) => `<option value="${doctor.id}">${escapeHtml(doctor.name)}</option>`).join('')
            : '<option value="">No active doctors</option>';
        if (activeDoctors.some((doctor) => String(doctor.id) === previous)) select.value = previous;
        select.disabled = activeDoctors.length === 0;
    });
}

function renderSpecializationOptions() {
    const specializations = [...new Set(state.doctors.flatMap((doctor) => doctor.specializations || []))]
        .sort((a, b) => a.localeCompare(b));
    ['overview-specialization', 'schedule-specialization'].forEach((id) => {
        const select = byId(id);
        const previous = select.value;
        select.innerHTML = '<option value="">All specializations</option>' +
            specializations.map((item) => `<option value="${escapeHtml(item)}">${escapeHtml(item)}</option>`).join('');
        select.value = previous;
    });
}

function renderDoctors() {
    const activeCount = state.doctors.filter((doctor) => doctor.active).length;
    byId('doctor-count-label').textContent = `${activeCount} active ${activeCount === 1 ? 'doctor' : 'doctors'}`;
    if (!state.doctors.length) {
        byId('doctors-list').innerHTML = '<div class="empty-state"><strong>Your care team starts here</strong>Add a doctor using the form.</div>';
        return;
    }
    byId('doctors-list').innerHTML = state.doctors.map((doctor) => `
        <div class="doctor-card">
            <div class="doctor-monogram">${escapeHtml(initials(doctor.name))}</div>
            <div class="doctor-card-main">
                <strong>${escapeHtml(doctor.name)}${doctor.active ? '' : ' · Inactive'}</strong>
                <small>${escapeHtml(doctor.email)}</small>
                <div class="specialty-tags">${(doctor.specializations || []).map((item) => `<span class="specialty-tag">${escapeHtml(item)}</span>`).join('')}</div>
            </div>
            <div class="doctor-actions">
                ${doctor.active ? `<button class="quiet-button" data-edit-doctor="${doctor.id}">Edit</button><button class="quiet-button danger" data-remove-doctor="${doctor.id}">Remove</button>` : ''}
            </div>
        </div>`).join('');
}

async function loadPatients() {
    state.patients = await request('/patients');
    renderPatients();
    const select = byId('booking-patient');
    const previous = select.value;
    select.innerHTML = state.patients.length
        ? '<option value="">Choose a patient</option>' + state.patients.map((patient) =>
            `<option value="${patient.id}">${escapeHtml(patient.name)} · ${escapeHtml(patient.email)}</option>`).join('')
        : '<option value="">Register a patient first</option>';
    if (state.patients.some((patient) => String(patient.id) === previous)) select.value = previous;
    select.disabled = !state.patients.length;
}

function renderPatients() {
    byId('patient-count-label').textContent = `${state.patients.length} registered ${state.patients.length === 1 ? 'patient' : 'patients'}`;
    if (!state.patients.length) {
        byId('patients-list').innerHTML = '<div class="empty-state"><strong>No patients yet</strong>Register a patient to start booking visits.</div>';
        return;
    }
    byId('patients-list').innerHTML = state.patients.map((patient) => `
        <div class="patient-row">
            <div class="patient-initials">${escapeHtml(initials(patient.name))}</div>
            <div class="patient-details"><strong>${escapeHtml(patient.name)}</strong><small>${escapeHtml(patient.email)} · ${escapeHtml(patient.phone)}</small></div>
            <div class="patient-actions"><button class="quiet-button" data-edit-patient="${patient.id}">Edit</button></div>
        </div>`).join('');
}

function makeSlotMarkup(slot) {
    const isOpen = slot.status === 'AVAILABLE';
    return `<div class="slot-row">
        <div class="slot-doctor"><div class="doctor-monogram">${escapeHtml(initials(slot.doctorName))}</div><div><strong>${escapeHtml(slot.doctorName)}</strong><small>${escapeHtml(slot.specialization || 'Clinic appointment')}</small></div></div>
        <div class="slot-time"><strong>${escapeHtml(displayDate(slot.startTime))}</strong><small>${escapeHtml(displayTime(slot.startTime))} – ${escapeHtml(displayTime(slot.endTime))}</small></div>
        <span class="badge ${isOpen ? 'open' : 'booked'}"><span class="status-dot ${isOpen ? 'green' : 'blue-dot'}"></span>${isOpen ? 'Open' : 'Booked'}</span>
        <div class="slot-actions">${isOpen ? `<button class="mini-button" data-book-slot="${slot.id}">Book slot</button>` : ''}</div>
    </div>`;
}

async function loadSlots(target) {
    const date = byId(target === 'overview' ? 'overview-date' : 'schedule-date').value;
    const specialization = byId(target === 'overview' ? 'overview-specialization' : 'schedule-specialization').value;
    const params = new URLSearchParams();
    if (date) params.set('date', date);
    if (specialization) params.set('specialization', specialization);
    const container = byId(target === 'overview' ? 'overview-slots' : 'schedule-slots');
    container.innerHTML = '<div class="loading">Finding available times…</div>';
    try {
        state.slots = await request(`/slots${params.size ? `?${params.toString()}` : ''}`);
        const shownSlots = target === 'overview' ? state.slots.slice(0, 6) : state.slots;
        container.innerHTML = shownSlots.length
            ? shownSlots.map(makeSlotMarkup).join('')
            : '<div class="empty-state"><strong>No matching slots</strong>Try another date or specialization, or publish availability for a doctor.</div>';
        refreshBookingSlots();
    } catch (error) {
        container.innerHTML = '<div class="empty-state"><strong>Could not load slots</strong>Please try again.</div>';
        toast(error.message, 'error');
    }
}

function refreshBookingSlots(selectedId = '') {
    const openSlots = state.slots.filter((slot) => slot.status === 'AVAILABLE');
    const select = byId('booking-slot');
    select.innerHTML = openSlots.length
        ? '<option value="">Choose an open slot</option>' + openSlots.map((slot) =>
            `<option value="${slot.id}">${escapeHtml(slot.doctorName)} · ${escapeHtml(displayDate(slot.startTime))} · ${escapeHtml(displayTime(slot.startTime))}–${escapeHtml(displayTime(slot.endTime))}</option>`).join('')
        : '<option value="">No open slots in this search</option>';
    select.disabled = !openSlots.length;
    if (openSlots.some((slot) => String(slot.id) === String(selectedId))) select.value = selectedId;
}

async function loadTodaysAppointments(context) {
    const selector = byId(context === 'overview' ? 'overview-doctor-select' : 'appointments-doctor-select');
    const container = byId(context === 'overview' ? 'overview-today-list' : 'appointments-list');
    const doctorId = selector.value;
    if (!doctorId) {
        container.innerHTML = '<div class="empty-state"><strong>No active doctors</strong>Add a doctor to manage appointments.</div>';
        return;
    }
    container.innerHTML = '<div class="loading">Loading appointments…</div>';
    try {
        state.appointments = await request(`/doctors/${doctorId}/appointments/today`);
        if (context === 'overview') {
            container.className = state.appointments.length ? 'table-wrap' : 'empty-inline';
            container.innerHTML = state.appointments.length
                ? appointmentTable(state.appointments, true)
                : 'No booked appointments for this doctor today.';
        } else {
            container.innerHTML = state.appointments.length
                ? appointmentTable(state.appointments, false)
                : '<div class="empty-state"><strong>A clear schedule today</strong>There are no booked appointments for this doctor.</div>';
        }
    } catch (error) {
        container.innerHTML = '<div class="empty-state"><strong>Could not load appointments</strong>Please try again.</div>';
        toast(error.message, 'error');
    }
}

async function loadBookedAppointments() {
    const container = byId('appointments-list');
    container.innerHTML = '<div class="loading">Loading booked appointments…</div>';
    try {
        const range = byId('appointment-date-range').value;
        let date = '';
        if (range === 'today') date = localDateString(new Date());
        if (range === 'tomorrow') {
            const tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);
            date = localDateString(tomorrow);
        }
        if (range === 'custom') date = byId('appointment-custom-date').value;
        if (range === 'custom' && !date) {
            byId('appointment-date-label').textContent = 'Choose a date to see its booked appointments.';
            container.innerHTML = '<div class="empty-state"><strong>Select a date</strong>Choose a date above, then show appointments.</div>';
            return;
        }
        state.appointments = await request(`/appointments${date ? `?date=${encodeURIComponent(date)}` : ''}`);
        const rangeLabel = range === 'today' ? 'today' : range === 'tomorrow' ? 'tomorrow'
            : range === 'custom' ? displayDate(`${date}T00:00:00`) : 'across all dates';
        byId('appointment-date-label').textContent = state.appointments.length
            ? `${state.appointments.length} booked ${state.appointments.length === 1 ? 'visit' : 'visits'} ${rangeLabel}`
            : `No booked visits ${rangeLabel}.`;
        container.innerHTML = state.appointments.length
            ? appointmentTable(state.appointments, false)
            : `<div class="empty-state"><strong>No booked appointments ${escapeHtml(rangeLabel)}</strong>Try another date or choose all dates.</div>`;
    } catch (error) {
        container.innerHTML = '<div class="empty-state"><strong>Could not load appointments</strong>Please try again.</div>';
        toast(error.message, 'error');
    }
}

function appointmentTable(appointments, compact) {
    return `<table class="data-table"><thead><tr><th>Patient</th>${compact ? '' : '<th>Doctor</th><th>Date</th>'}<th>Time</th>${compact ? '' : '<th>Contact</th>'}<th>Status</th><th></th></tr></thead><tbody>
        ${appointments.map((appointment) => {
            const patient = state.patients.find((item) => item.id === appointment.patientId);
            return `<tr><td>${escapeHtml(appointment.patientName)}</td>${compact ? '' : `<td>${escapeHtml(appointment.doctorName)}</td><td>${escapeHtml(displayDate(appointment.startTime))}</td>`}<td>${escapeHtml(displayTime(appointment.startTime))}</td>${compact ? '' : `<td>${escapeHtml(patient?.email || '—')}</td>`}<td><span class="badge booked">Booked</span></td><td><button class="quiet-button danger" data-cancel-appointment="${appointment.id}">Cancel</button></td></tr>`;
        }).join('')}
    </tbody></table>`;
}

async function loadAll() {
    try {
        const [account] = await Promise.all([request('/auth/me'), loadDashboard(), loadDoctors(), loadPatients()]);
        byId('user-name').textContent = account.name;
        byId('user-avatar').textContent = initials(account.name);
        byId('top-user-avatar').textContent = initials(account.name);
        await Promise.all([loadSlots('overview'), loadTodaysAppointments('overview')]);
    } catch (error) {
        toast(error.message || 'Unable to connect to the clinic API.', 'error');
    }
}

function resetDoctorForm() {
    byId('doctor-form').reset();
    byId('doctor-edit-id').value = '';
    byId('doctor-form-title').textContent = 'Add a doctor';
    byId('doctor-submit-button').innerHTML = 'Add doctor <span>→</span>';
    byId('doctor-cancel-edit').classList.add('hidden');
}

function resetPatientForm() {
    byId('patient-form').reset();
    byId('patient-edit-id').value = '';
    byId('patient-form-title').textContent = 'Register a patient';
    byId('patient-submit-button').innerHTML = 'Add patient <span>→</span>';
    byId('patient-cancel-edit').classList.add('hidden');
}

function localDateString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

document.querySelectorAll('.nav-link').forEach((button) =>
    button.addEventListener('click', (event) => {
        if (button.tagName === 'A') {
            event.preventDefault();
            setView(button.dataset.view);
        }
    }));
window.addEventListener('hashchange', () => setView(window.location.hash.slice(1) || 'overview', false));
document.querySelectorAll('[data-go]').forEach((button) =>
    button.addEventListener('click', () => setView(button.dataset.go)));
byId('refresh-button').addEventListener('click', loadAll);
byId('overview-filter-button').addEventListener('click', () => loadSlots('overview'));
byId('schedule-filter-button').addEventListener('click', () => loadSlots('schedule'));
byId('overview-today-button').addEventListener('click', () => loadTodaysAppointments('overview'));
byId('appointments-load-button').addEventListener('click', loadBookedAppointments);
byId('appointment-date-range').addEventListener('change', () => {
    byId('appointment-custom-date-field').classList.toggle('hidden', byId('appointment-date-range').value !== 'custom');
    loadBookedAppointments();
});
byId('appointment-custom-date').addEventListener('change', loadBookedAppointments);
byId('overview-doctor-select').addEventListener('change', () => loadTodaysAppointments('overview'));
byId('appointment-date-label').textContent = 'All booked visits across your clinic.';

byId('publish-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const doctorId = form.get('doctorId');
    const daysOfWeek = [...byId('working-days').selectedOptions].map((option) => option.value);
    const payload = {
        dateFrom: form.get('dateFrom'),
        dateTo: form.get('dateTo'),
        timeFrom: `${form.get('timeFrom')}:00`,
        timeTo: `${form.get('timeTo')}:00`,
        slotMinutes: Number(form.get('slotMinutes')),
        ...(daysOfWeek.length ? { daysOfWeek } : {})
    };
    try {
        const created = await request(`/doctors/${doctorId}/slots`, { method: 'POST', body: JSON.stringify(payload) });
        toast(`${created.length} ${created.length === 1 ? 'slot' : 'slots'} published successfully.`);
        await Promise.all([loadDashboard(), loadSlots('schedule')]);
        event.currentTarget.reset();
        byId('publish-doctor').value = doctorId;
    } catch (error) {
        toast(error.message, 'error');
    }
});

byId('booking-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
        await request('/appointments', {
            method: 'POST',
            body: JSON.stringify({ patientId: Number(form.get('patientId')), slotId: Number(form.get('slotId')) })
        });
        toast('Appointment booked. The slot is no longer available.');
        event.currentTarget.reset();
        await Promise.all([loadDashboard(), loadSlots('schedule')]);
    } catch (error) {
        toast(error.message, 'error');
    }
});

byId('doctor-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const doctorId = form.get('doctorId');
    const payload = {
        name: String(form.get('name')).trim(),
        email: String(form.get('email')).trim(),
        specializations: String(form.get('specializations')).split(',').map((item) => item.trim()).filter(Boolean)
    };
    try {
        await request(doctorId ? `/doctors/${doctorId}` : '/doctors', {
            method: doctorId ? 'PUT' : 'POST',
            body: JSON.stringify(payload)
        });
        toast(doctorId ? 'Doctor details updated.' : 'Doctor added to the care team.');
        resetDoctorForm();
        await Promise.all([loadDoctors(), loadDashboard()]);
    } catch (error) {
        toast(error.message, 'error');
    }
});

byId('patient-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    const payload = Object.fromEntries(['name', 'email', 'phone'].map((key) => [key, String(formData.get(key)).trim()]));
    const patientId = formData.get('patientId');
    try {
        await request(patientId ? `/patients/${patientId}` : '/patients', {
            method: patientId ? 'PUT' : 'POST',
            body: JSON.stringify(payload)
        });
        toast(patientId ? 'Patient details updated.' : 'Patient registered successfully.');
        resetPatientForm();
        await Promise.all([loadPatients(), loadDashboard()]);
    } catch (error) {
        toast(error.message, 'error');
    }
});

byId('doctor-cancel-edit').addEventListener('click', resetDoctorForm);
byId('patient-cancel-edit').addEventListener('click', resetPatientForm);

document.addEventListener('click', async (event) => {
    const bookButton = event.target.closest('[data-book-slot]');
    if (bookButton) {
        setView('schedule');
        await loadSlots('schedule');
        refreshBookingSlots(bookButton.dataset.bookSlot);
        byId('booking-patient').focus();
        return;
    }
    const cancelButton = event.target.closest('[data-cancel-appointment]');
    if (cancelButton) {
        if (!window.confirm('Cancel this appointment and release its time slot?')) return;
        try {
            await request(`/appointments/${cancelButton.dataset.cancelAppointment}/cancel`, { method: 'POST' });
            toast('Appointment cancelled. Its slot is available again.');
            await Promise.all([
                loadDashboard(),
                loadBookedAppointments(),
                loadTodaysAppointments('overview'),
                loadSlots('schedule')
            ]);
        } catch (error) {
            toast(error.message, 'error');
        }
        return;
    }
    const editButton = event.target.closest('[data-edit-doctor]');
    if (editButton) {
        const doctor = state.doctors.find((item) => String(item.id) === editButton.dataset.editDoctor);
        if (!doctor) return;
        byId('doctor-edit-id').value = doctor.id;
        byId('doctor-form').elements.name.value = doctor.name;
        byId('doctor-form').elements.email.value = doctor.email;
        byId('doctor-form').elements.specializations.value = (doctor.specializations || []).join(', ');
        byId('doctor-form-title').textContent = 'Update doctor';
        byId('doctor-submit-button').innerHTML = 'Save changes <span>→</span>';
        byId('doctor-cancel-edit').classList.remove('hidden');
        byId('doctor-form').scrollIntoView({ behavior: 'smooth', block: 'center' });
        return;
    }
    const editPatientButton = event.target.closest('[data-edit-patient]');
    if (editPatientButton) {
        const patient = state.patients.find((item) => String(item.id) === editPatientButton.dataset.editPatient);
        if (!patient) return;
        byId('patient-edit-id').value = patient.id;
        byId('patient-form').elements.name.value = patient.name;
        byId('patient-form').elements.email.value = patient.email;
        byId('patient-form').elements.phone.value = patient.phone;
        byId('patient-form-title').textContent = 'Update patient';
        byId('patient-submit-button').innerHTML = 'Save changes <span>→</span>';
        byId('patient-cancel-edit').classList.remove('hidden');
        byId('patient-form').scrollIntoView({ behavior: 'smooth', block: 'center' });
        return;
    }
    const removeButton = event.target.closest('[data-remove-doctor]');
    if (removeButton) {
        if (!window.confirm('Remove this doctor from the active clinic team? Existing appointments and schedules will be kept.')) return;
        try {
            await request(`/doctors/${removeButton.dataset.removeDoctor}`, { method: 'DELETE' });
            toast('Doctor deactivated. Existing schedule history was retained.');
            await Promise.all([loadDoctors(), loadDashboard(), loadSlots('schedule')]);
        } catch (error) {
            toast(error.message, 'error');
        }
    }
});

byId('header-date').textContent = displayDate(new Date().toISOString(), { weekday: 'short', month: 'short', day: 'numeric' });
async function signOut(button) {
    button.disabled = true;
    try {
        await request('/auth/logout', { method: 'POST' });
        window.location.assign('/');
    } catch (error) {
        toast(error.message, 'error');
        button.disabled = false;
    }
}
byId('logout-button').addEventListener('click', (event) => signOut(event.currentTarget));
byId('top-logout-button').addEventListener('click', (event) => signOut(event.currentTarget));
byId('appointment-date-label').textContent = displayDate(new Date().toISOString());
const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
const localTomorrow = new Date(tomorrow.getTime() - tomorrow.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
document.querySelectorAll('#publish-form input[type="date"]').forEach((input) => { input.value = localTomorrow; });
setView(window.location.hash.slice(1) || 'overview', false);
loadAll();
