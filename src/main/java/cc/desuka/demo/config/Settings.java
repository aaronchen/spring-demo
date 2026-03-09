package cc.desuka.demo.config;

/**
 * Typed representation of all site-wide settings with defaults.
 * This is the single source of truth — add new settings here.
 *
 * <p>{@link cc.desuka.demo.service.SettingService#load()} uses Spring's
 * {@code BeanWrapper} to auto-map DB rows to fields by name, with type
 * conversion (e.g. String → boolean). Missing keys keep their default.</p>
 *
 * <p><b>To add a new setting:</b></p>
 * <ol>
 *   <li>Add a field with its default value below</li>
 *   <li>Add a {@code KEY_*} constant whose value matches the field name exactly</li>
 *   <li>Add {@code audit.field.<key>} to {@code messages.properties} for audit display</li>
 * </ol>
 */
public class Settings {

    /**
     * DB key constants — each value must match the corresponding field name
     * exactly (BeanWrapper resolves fields by name).
     */
    public static final String KEY_THEME = "theme";
    public static final String KEY_SITE_NAME = "siteName";
    public static final String KEY_REGISTRATION_ENABLED = "registrationEnabled";
    public static final String KEY_MAINTENANCE_BANNER = "maintenanceBanner";

    private String theme = "";
    private String siteName = "Spring Workshop";
    private boolean registrationEnabled = true;
    private String maintenanceBanner = "";

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public String getMaintenanceBanner() {
        return maintenanceBanner;
    }

    public void setMaintenanceBanner(String maintenanceBanner) {
        this.maintenanceBanner = maintenanceBanner;
    }
}
