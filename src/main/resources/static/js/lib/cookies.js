export function getCookie(name) {
    const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${name}=([^;]*)`));
    return match ? match[1] : null;
}

export function setCookie(name, value) {
    document.cookie = `${name}=${value}; path=/; max-age=31536000`;
}
