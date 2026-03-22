package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.TagRequest;
import cc.desuka.demo.dto.TagResponse;
import cc.desuka.demo.mapper.TagMapper;
import cc.desuka.demo.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tags")
public class TagApiController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    public TagApiController(TagService tagService, TagMapper tagMapper) {
        this.tagService = tagService;
        this.tagMapper = tagMapper;
    }

    // GET /api/tags
    @GetMapping
    public List<TagResponse> getAllTags() {
        return tagMapper.toResponseList(tagService.getAllTags());
    }

    // GET /api/tags/1
    @GetMapping("/{id}")
    public TagResponse getTagById(@PathVariable Long id) {
        return tagMapper.toResponse(tagService.getTagById(id));
    }

    // POST /api/tags
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(@Valid @RequestBody TagRequest request) {
        return tagMapper.toResponse(tagService.createTag(tagMapper.toEntity(request)));
    }

    // DELETE /api/tags/1
    // Hibernate removes the task_tags join table rows for this tag automatically.
    // Tasks that had this tag simply lose it — they are not deleted.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
    }
}
