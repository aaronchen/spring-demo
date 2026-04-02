package cc.desuka.demo.controller.admin;

import cc.desuka.demo.config.Settings;
import cc.desuka.demo.service.SettingService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    private final SettingService settingService;

    public SettingsController(SettingService settingService) {
        this.settingService = settingService;
    }

    public record ThemeOption(String id, List<String> colors) {}

    private static final List<ThemeOption> THEMES =
            List.of(
                    new ThemeOption(
                            Settings.THEME_DEFAULT,
                            List.of("#0d6efd", "#198754", "#ffc107", "#dc3545")),
                    new ThemeOption(
                            Settings.THEME_WORKSHOP,
                            List.of("#2b7de9", "#16a34a", "#e6a740", "#dc2626")),
                    new ThemeOption(
                            Settings.THEME_SAPPHIRE,
                            List.of("#3b52d4", "#059669", "#ca8a04", "#dc2626")));

    @GetMapping
    public String settingsPage(Model model) {
        model.addAttribute("themes", THEMES);
        return "admin/settings";
    }

    @PostMapping("/general")
    @ResponseBody
    public ResponseEntity<Void> saveGeneral(
            @RequestParam(defaultValue = "") String siteName,
            @RequestParam(defaultValue = "false") boolean registrationEnabled,
            @RequestParam(defaultValue = "") String maintenanceBanner,
            @RequestParam(defaultValue = "30") int notificationPurgeDays) {
        String banner = maintenanceBanner != null ? maintenanceBanner : "";
        settingService.updateValue(Settings.KEY_SITE_NAME, siteName != null ? siteName : "");
        settingService.updateValue(
                Settings.KEY_REGISTRATION_ENABLED, String.valueOf(registrationEnabled));
        String previousBanner = settingService.load().getMaintenanceBanner();
        settingService.updateValue(Settings.KEY_MAINTENANCE_BANNER, banner);
        if (!banner.equals(previousBanner)) {
            settingService.updateValue(
                    Settings.KEY_MAINTENANCE_BANNER_VERSION,
                    String.valueOf(System.currentTimeMillis()));
        }
        settingService.updateValue(
                Settings.KEY_NOTIFICATION_PURGE_DAYS, String.valueOf(notificationPurgeDays));

        return ResponseEntity.ok().header("HX-Trigger", "settingsSaved").build();
    }

    @PostMapping("/theme")
    @ResponseBody
    public ResponseEntity<Void> setTheme(
            @RequestParam(defaultValue = Settings.THEME_DEFAULT) String theme) {
        if (THEMES.stream().noneMatch(t -> t.id().equals(theme))) {
            return ResponseEntity.badRequest().build();
        }

        settingService.updateValue(Settings.KEY_THEME, theme);

        return ResponseEntity.ok()
                .header("HX-Trigger", "{\"themeSaved\": {\"theme\": \"" + theme + "\"}}")
                .build();
    }
}
