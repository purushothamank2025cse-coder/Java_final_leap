const menuButton = document.querySelector('.mobile-menu-button');
const navigation = document.getElementById('public-navigation');

menuButton.addEventListener('click', () => {
    const isOpen = navigation.classList.toggle('open');
    menuButton.setAttribute('aria-expanded', String(isOpen));
    menuButton.setAttribute('aria-label', isOpen ? 'Close navigation' : 'Open navigation');
});

navigation.addEventListener('click', (event) => {
    if (event.target.closest('a')) {
        navigation.classList.remove('open');
        menuButton.setAttribute('aria-expanded', 'false');
        menuButton.setAttribute('aria-label', 'Open navigation');
    }
});

async function updateAccountActions() {
    const signIn = document.querySelector('.account-sign-in');
    const signUp = document.querySelector('.account-sign-up');
    const signOut = document.getElementById('home-logout-button');
    const workspaceLink = document.querySelector('.hero-primary-action');
    const welcomeLink = document.querySelector('.welcome-action');
    try {
        const response = await fetch('/api/auth/me', { credentials: 'same-origin' });
        if (response.status === 401) return;
        if (!response.ok) {
            throw new Error('Could not check your sign-in status.');
        }
        signIn.href = '/dashboard';
        signIn.textContent = 'Open workspace';
        signUp.classList.add('hidden');
        signOut.classList.remove('hidden');
        workspaceLink.href = '/dashboard';
        workspaceLink.textContent = 'Open workspace →';
        welcomeLink.href = '/dashboard';
        welcomeLink.textContent = 'Open your workspace →';
    } catch (error) {
        document.getElementById('account-status').textContent =
            `${error.message} Sign-in options are still available.`;
    }
}

document.getElementById('home-logout-button').addEventListener('click', async (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    try {
        const csrfResponse = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
        if (!csrfResponse.ok) throw new Error('Could not start a secure session. Reload and try again.');
        const csrf = await csrfResponse.json();
        const response = await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { [csrf.headerName || 'X-CSRF-TOKEN']: csrf.token }
        });
        if (!response.ok) throw new Error('Sign out failed. Please try again.');
        window.location.reload();
    } catch (error) {
        button.disabled = false;
        button.textContent = error.message;
    }
});

updateAccountActions();
