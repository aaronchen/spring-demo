package cc.desuka.demo.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class GlobalBindingConfig {

    // Registers a StringTrimmerEditor for all String fields processed by WebDataBinder.
    // StringTrimmerEditor(true) does two things: trims whitespace, and converts empty strings to null.
    //
    // Affected (WebDataBinder-based binding):
    //   - @ModelAttribute fields (form submissions bound to DTOs, e.g. TaskRequest, AdminUserRequest)
    //   - @RequestParam values (query parameters and form fields bound to method arguments)
    //   - @PathVariable values (URL path segments)
    //
    // NOT affected (these bypass WebDataBinder):
    //   - @RequestBody (JSON deserialized by Jackson — uses its own ObjectMapper, not WebDataBinder)
    //   - Values read directly from HttpServletRequest (e.g. request.getParameter())
    //
    // Effect: " hello " → "hello", "  " → null, "" → null
    // This means @NotBlank catches blank/empty input naturally — no manual .trim() needed in controllers.
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
