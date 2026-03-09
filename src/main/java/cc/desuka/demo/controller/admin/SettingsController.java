package cc.desuka.demo.controller.admin;

import cc.desuka.demo.config.Settings;
import cc.desuka.demo.service.SettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    private final SettingService settingService;

    public SettingsController(SettingService settingService) {
        this.settingService = settingService;
    }

    public record ThemeOption(String id, List<String> colors) {}

    private static final List<ThemeOption> THEMES = List.of(
            new ThemeOption("",
                    List.of("#0d6efd", "#198754", "#ffc107", "#dc3545")),
            new ThemeOption("workshop",
                    List.of("#4fb5ee", "#4db17f", "#e6a740", "#f44336")),
            new ThemeOption("indigo",
                    List.of("#6366f1", "#14b8a6", "#f59e0b", "#f43f5e"))
    );

    @GetMapping
    public String settingsPage(Model model) {
        model.addAttribute("themes", THEMES);
        return "admin/settings";
    }

    @PostMapping("/general")
    @ResponseBody
    public ResponseEntity<Void> saveGeneral(@RequestParam(defaultValue = "") String siteName,
                                            @RequestParam(defaultValue = "false") boolean registrationEnabled,
                                            @RequestParam(defaultValue = "") String maintenanceBanner) {
        settingService.updateValue(Settings.KEY_SITE_NAME, siteName.isBlank() ? null : siteName.trim());
        settingService.updateValue(Settings.KEY_REGISTRATION_ENABLED, String.valueOf(registrationEnabled));
        settingService.updateValue(Settings.KEY_MAINTENANCE_BANNER, maintenanceBanner.isBlank() ? null : maintenanceBanner.trim());

        return ResponseEntity.ok()
                .header("HX-Trigger", "settingsSaved")
                .build();
    }

    @PostMapping("/theme")
    @ResponseBody
    public ResponseEntity<Void> setTheme(@RequestParam(defaultValue = "") String theme) {
        String value = theme.isBlank() ? null : theme;
        settingService.updateValue(Settings.KEY_THEME, value);

        return ResponseEntity.ok()
                .header("HX-Trigger",
                        "{\"themeSaved\": {\"theme\": " +
                        (value != null ? "\"" + value + "\"" : "null") + "}}")
                .build();
    }
}
