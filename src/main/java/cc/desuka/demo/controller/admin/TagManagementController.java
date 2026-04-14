package cc.desuka.demo.controller.admin;

import cc.desuka.demo.dto.TagRequest;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.service.TagQueryService;
import cc.desuka.demo.service.TagService;
import cc.desuka.demo.util.HtmxUtils;
import cc.desuka.demo.util.HtmxUtils.ToastType;
import cc.desuka.demo.util.Messages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/tags")
public class TagManagementController {

    private final TagQueryService tagQueryService;
    private final TagService tagService;
    private final Messages messages;

    public TagManagementController(
            TagQueryService tagQueryService, TagService tagService, Messages messages) {
        this.tagQueryService = tagQueryService;
        this.tagService = tagService;
        this.messages = messages;
    }

    @GetMapping
    public String listTags(Model model, HttpServletRequest request) {
        populateModel(model);
        return htmxOrFull(request);
    }

    @PostMapping
    public String createTag(
            @Valid @ModelAttribute TagRequest tagRequest,
            BindingResult result,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (result.hasErrors()) {
            model.addAttribute("tagName", tagRequest.getName());
            populateModel(model);
            return htmxOrFull(request);
        }

        tagService.createTag(new Tag(tagRequest.getName()));
        response.setHeader(
                "HX-Trigger",
                HtmxUtils.toastTrigger(
                        messages.get("toast.admin.tags.created"), ToastType.SUCCESS));
        populateModel(model);
        return htmxOrFull(request);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return HtmxUtils.triggerEvent("tagDeleted");
    }

    private void populateModel(Model model) {
        List<Tag> tags = tagQueryService.getAllTags();
        Map<Long, Integer> taskCounts = new LinkedHashMap<>();
        for (Tag tag : tags) {
            taskCounts.put(tag.getId(), tagQueryService.countTasksByTagId(tag.getId()));
        }
        model.addAttribute("tags", tags);
        model.addAttribute("taskCounts", taskCounts);
        if (!model.containsAttribute("tagRequest")) {
            model.addAttribute("tagRequest", new TagRequest());
        }
    }

    private String htmxOrFull(HttpServletRequest request) {
        if (HtmxUtils.isHtmxRequest(request)) {
            return "admin/tag-table";
        }
        return "admin/tags";
    }
}
