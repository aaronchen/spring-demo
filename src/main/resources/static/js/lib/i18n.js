export function t(key, ...args) {
    const template = APP_CONFIG.messages[key] || key;
    return args.reduce((s, v, i) => s.replaceAll(`{${i}}`, v), template);
}

export function enumToCamelCase(value) {
    return value.toLowerCase().replace(/_([a-z])/g, (_, c) => c.toUpperCase());
}

export function resolveLabel(prefix, value) {
    const key = `${prefix}.${enumToCamelCase(value)}`;
    return APP_CONFIG.messages[key] || value;
}
