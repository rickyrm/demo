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
    @PostMapping("")
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> req) {
        String title = (String) req.getOrDefault("title", "");
        String content = (String) req.getOrDefault("content", "");
        String metaDescription = (String) req.getOrDefault("metaDescription", "");
        String slugInput = (String) req.get("slug");

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setMetaDescription(metaDescription);
        post.setStatus(Post.Status.DRAFT);

        String slug = slugInput != null ? com.proyecto.demo.util.SlugUtil.slugify(slugInput) : com.proyecto.demo.util.SlugUtil.slugify(title);
        post.setSlug(slug);

        // Persist post in DB first
        postService.savePost(post);

        // Prepare Markdown file
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: \"" + title.replace("\"", "'") + "\"\n");
        sb.append("slug: \"" + slug + "\"\n");
        sb.append("metaDescription: \"" + metaDescription.replace("\"", "'") + "\"\n");
        sb.append("status: " + post.getStatus() + "\n");
        sb.append("publishedAt: " + (post.getPublishedAt() != null ? post.getPublishedAt() : "") + "\n");
        sb.append("---\n\n");
        sb.append(content);

        // Write file atomically
        java.nio.file.Path dir = java.nio.file.Paths.get("content/posts");
        java.nio.file.Path file = dir.resolve(slug + ".md");
        try {
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(file, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Rollback DB if file write fails
            postService.deletePostById(post.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to persist post file", "details", e.getMessage()));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", post.getId());
        response.put("slug", slug);
        response.put("file", file.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private final PostService postService;
    private final InMemorySlugRedirectRepository redirectRepository;

    public PostController(PostService postService, InMemorySlugRedirectRepository redirectRepository) {
        this.postService = postService;
        this.redirectRepository = redirectRepository;
    }

    @PostMapping("/slugify")
    public ResponseEntity<Map<String, String>> slugifyTitle(@RequestBody Map<String, String> req) {
        String title = req.getOrDefault("title", "");
        String slug = com.proyecto.demo.util.SlugUtil.slugify(title);
        Map<String, String> response = new HashMap<>();
        response.put("slug", slug);
        return ResponseEntity.ok(response);
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
