package com.proyecto.demo.web;

import com.proyecto.demo.model.Post;
import com.proyecto.demo.model.SlugRedirect;
import com.proyecto.demo.repository.InMemorySlugRedirectRepository;
import com.proyecto.demo.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final InMemorySlugRedirectRepository redirectRepository;

    public PostController(PostService postService, InMemorySlugRedirectRepository redirectRepository) {
        this.postService = postService;
        this.redirectRepository = redirectRepository;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable("id") UUID id, @RequestBody UpdatePostRequest req) {
        Post updated = postService.updatePost(id, req.getTitle(), req.getSlug(), req.getMetaDescription(), req.getStatus(), req.getPublishedAt());

        Map<String, Object> body = new HashMap<>();
        body.put("id", updated.getId());
        body.put("slug", updated.getSlug());

        // If there is a redirect for an old slug, inform the caller
        SlugRedirect redirect = redirectRepository.findByOldSlug(req.getSlug() == null ? "" : req.getSlug());
        if (redirect != null) {
            body.put("redirect", Map.of("oldSlug", redirect.getOldSlug(), "newSlug", redirect.getNewSlug()));
        }

        return ResponseEntity.status(HttpStatus.OK).body(body);
    }
}
