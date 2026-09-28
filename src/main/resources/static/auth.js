let csrfToken;

const message = (text, isError = false) => {
    const element = document.getElementById('auth-message');
    element.textContent = text;
    element.classList.remove('hidden', 'border-rose-200', 'bg-rose-50', 'text-rose-800', 'border-emerald-200', 'bg-emerald-50', 'text-emerald-800');
    element.classList.add(...(isError
        ? ['border-rose-200', 'bg-rose-50', 'text-rose-800']
        : ['border-emerald-200', 'bg-emerald-50', 'text-emerald-800']));
};

async function refreshCsrfToken() {
    const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
    if (!response.ok) throw new Error('Unable to start a secure session. Please reload and try again.');
    const csrf = await response.json();
    csrfToken = csrf.token;
}

async function postJson(path, body) {
    if (!csrfToken) await refreshCsrfToken();
    const response = await fetch(path, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', [document.querySelector('meta[name="csrf-header"]')?.content || 'X-CSRF-TOKEN']: csrfToken },
        body: JSON.stringify(body)
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.message || 'We could not complete your request.');
    return result;
}

document.addEventListener('DOMContentLoaded', async () => {
    const form = document.getElementById('login-form') || document.getElementById('signup-form');
    if (!form) return;
    try {
        await refreshCsrfToken();
    } catch (error) {
        message(error.message, true);
        return;
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const submit = form.querySelector('button[type="submit"]');
        submit.disabled = true;
        try {
            const values = Object.fromEntries(new FormData(form));
            if (form.id === 'signup-form') {
                await postJson('/api/auth/signup', values);
                message('Your account is ready. Sign in to continue.');
                form.reset();
                return;
            }
            const account = await postJson('/api/auth/login', values);
            window.location.assign('/dashboard');
        } catch (error) {
            message(error.message, true);
        } finally {
            submit.disabled = false;
        }
    });
});
