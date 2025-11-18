package com.proyecto.demo.web;

import com.proyecto.demo.model.Post;

import java.time.Instant;

public class UpdatePostRequest {

    private String title;
    private String slug;
    private String metaDescription;
    private Post.Status status;
    private Instant publishedAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public Post.Status getStatus() {
        return status;
    }

    public void setStatus(Post.Status status) {
        this.status = status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
