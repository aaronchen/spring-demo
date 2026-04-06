export function enumToCamelCase(value) {
    return value.toLowerCase().replace(/_([a-z])/g, (_, c) => c.toUpperCase());
}

export function resolveLabel(prefix, value) {
    const key = `${prefix}.${enumToCamelCase(value)}`;
    return APP_CONFIG.messages[key] || value;
}
