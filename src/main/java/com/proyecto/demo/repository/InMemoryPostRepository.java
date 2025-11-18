package com.proyecto.demo.repository;

import com.proyecto.demo.model.Post;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPostRepository {

    private final Map<UUID, Post> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> slugIndex = new ConcurrentHashMap<>();

    public Optional<Post> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Post> findBySlug(String slug) {
        UUID id = slugIndex.get(slug);
        return id == null ? Optional.empty() : findById(id);
    }

    public boolean existsBySlug(String slug) {
        return slugIndex.containsKey(slug);
    }

    public void save(Post post) {
        byId.put(post.getId(), post);
        if (post.getSlug() != null) {
            slugIndex.put(post.getSlug(), post.getId());
        }
    }

    public Collection<Post> findAll() {
        return byId.values();
    }

    public void deleteById(UUID id) {
        Post removed = byId.remove(id);
        if (removed != null && removed.getSlug() != null) {
            slugIndex.remove(removed.getSlug());
        }
    }
}
