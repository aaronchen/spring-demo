package cc.desuka.demo.controller;

import cc.desuka.demo.service.TagQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tags")
public class TagController {

    private final TagQueryService tagQueryService;

    public TagController(TagQueryService tagQueryService) {
        this.tagQueryService = tagQueryService;
    }

    @GetMapping
    public String listTags(Model model) {
        model.addAttribute("tags", tagQueryService.getAllTags());
        return "tags/tags";
    }
}
