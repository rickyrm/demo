package com.proyecto.demo.repository;

import com.proyecto.demo.model.SlugRedirect;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySlugRedirectRepository {

    private final Map<UUID, SlugRedirect> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> oldSlugIndex = new ConcurrentHashMap<>();

    public void save(SlugRedirect redirect) {
        byId.put(redirect.getId(), redirect);
        if (redirect.getOldSlug() != null) {
            oldSlugIndex.put(redirect.getOldSlug(), redirect.getId());
        }
    }

    public SlugRedirect findByOldSlug(String oldSlug) {
        UUID id = oldSlugIndex.get(oldSlug);
        return id == null ? null : byId.get(id);
    }

    public Collection<SlugRedirect> findAll() {
        return byId.values();
    }
}
