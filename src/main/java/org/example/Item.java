package org.example;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Item implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private ItemId id;
    private String title;
    private ItemCategory category;
    private List<String> tags = new ArrayList<>();
    private int rating;
    private Instant createdAt;
    private String pathOrUrl;
    private String description;
    private MediaKind mediaKind;

    public Item(String title) {
        // generate unique id
        this.id = new ItemId(UUID.randomUUID().toString());
        this.title = title;
        this.category = ItemCategory.NOTE;
        this.rating = 0;
        this.createdAt = Instant.now();
        this.mediaKind = MediaKind.OTHER;
    }

    public static Item newBlank() {
        return new Item("Untitled");
    }

    public ItemId getId() {
        return id;
    }

    public void setId(ItemId id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String t) {
        this.title = t;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory c) {
        this.category = c;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> t) {
        this.tags = t;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int r) {
        this.rating = r;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getPathOrUrl() {
        return pathOrUrl;
    }

    public void setPathOrUrl(String pathOrUrl) {
        this.pathOrUrl = pathOrUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public MediaKind getMediaKind() {
        return mediaKind;
    }

    public void setMediaKind(MediaKind mediaKind) {
        this.mediaKind = mediaKind;
    }

    @Override
    public String toString() {
        String cat = (category != null) ? category.name() : "UNCAT";
        return title + " [" + cat + "] ★" + rating;
    }
}
