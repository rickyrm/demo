package com.proyecto.demo.service;

import com.proyecto.demo.model.Post;
import com.proyecto.demo.model.SlugRedirect;
import com.proyecto.demo.repository.InMemoryPostRepository;
import com.proyecto.demo.repository.InMemorySlugRedirectRepository;
import com.proyecto.demo.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class PostService {
    public void savePost(Post post) {
        postRepository.save(post);
    }

    public void deletePostById(UUID id) {
        postRepository.deleteById(id);
    }

    private final InMemoryPostRepository postRepository;
    private final InMemorySlugRedirectRepository redirectRepository;

    public PostService(InMemoryPostRepository postRepository,
                       InMemorySlugRedirectRepository redirectRepository) {
        this.postRepository = postRepository;
        this.redirectRepository = redirectRepository;
    }

    public Post updatePost(UUID id, String newTitle, String newSlug, String newMeta, Post.Status newStatus, Instant newPublishedAt) {
        Post existing = postRepository.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Post not found"));

        synchronized (this) {
            String oldSlug = existing.getSlug();

            if (newTitle != null) existing.setTitle(newTitle);

            String resolvedSlug = oldSlug;
            if (newSlug != null) {
                // normalize provided slug
                String candidate = SlugUtil.slugify(newSlug);
                if (candidate.isBlank()) {
                    throw new ResponseStatusException(CONFLICT, "Provided slug is invalid after normalization");
                }

                // ensure uniqueness; if collision, append numeric suffix
                resolvedSlug = ensureUniqueSlug(candidate, id);
            }

            if (newMeta != null) existing.setMetaDescription(newMeta);

            if (newStatus != null) existing.setStatus(newStatus);
            if (newPublishedAt != null) existing.setPublishedAt(newPublishedAt);

            // if post was published and slug changed, create redirect
            if (oldSlug != null && !oldSlug.equals(resolvedSlug) && existing.getStatus() == Post.Status.PUBLISHED) {
                SlugRedirect redirect = new SlugRedirect();
                redirect.setOldSlug(oldSlug);
                redirect.setNewSlug(resolvedSlug);
                redirect.setPostId(existing.getId());
                redirectRepository.save(redirect);
            }

            // update slug index and post
            existing.setSlug(resolvedSlug);
            postRepository.save(existing);
            return existing;
        }
    }

    private String ensureUniqueSlug(String base, UUID selfId) {
        String candidate = base.length() > 100 ? truncatePreferWholeWord(base, 100) : base;
        if (!postRepository.existsBySlug(candidate) || isOwnedBy(candidate, selfId)) {
            return candidate;
        }

        // try suffixes
        for (int i = 2; i <= 1000; i++) {
            String attempt = candidate;
            String suffix = "-" + i;
            if (attempt.length() + suffix.length() > 100) {
                attempt = truncatePreferWholeWord(attempt, 100 - suffix.length());
            }
            attempt = attempt + suffix;
            if (!postRepository.existsBySlug(attempt) || isOwnedBy(attempt, selfId)) {
                return attempt;
            }
        }
        throw new ResponseStatusException(CONFLICT, "Unable to resolve unique slug after retries");
    }

    private boolean isOwnedBy(String slug, UUID selfId) {
        return postRepository.findBySlug(slug).map(p -> p.getId().equals(selfId)).orElse(false);
    }

    private String truncatePreferWholeWord(String input, int max) {
        if (input.length() <= max) return input;
        int cut = input.lastIndexOf('-', max);
        if (cut <= 0) return input.substring(0, max);
        return input.substring(0, cut);
    }
}
