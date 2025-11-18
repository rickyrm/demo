package com.proyecto.demo.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SlugRedirect {

    private UUID id;
    private String oldSlug;
    private String newSlug;
    private UUID postId;
    private Instant createdAt;

    public SlugRedirect() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOldSlug() {
        return oldSlug;
    }

    public void setOldSlug(String oldSlug) {
        this.oldSlug = oldSlug;
    }

    public String getNewSlug() {
        return newSlug;
    }

    public void setNewSlug(String newSlug) {
        this.newSlug = newSlug;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlugRedirect that = (SlugRedirect) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
