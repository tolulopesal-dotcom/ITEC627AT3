package org.example;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class LibraryRepository implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // main storage for all items
    private final List<Item> items = new ArrayList<>();

    // stack for recently viewed items
    private final Deque<ItemId> recentStack = new ArrayDeque<>();

    // stack for undo functionality
    private final Deque<Memento> undoStack = new ArrayDeque<>();

    // priority queue sorts tasks by due date
    private final PriorityQueue<TodoTask> taskQueue = new PriorityQueue<>();

    // search index: word -> item ids
    private final Map<String, Set<ItemId>> keywordIndex = new HashMap<>();

    // counts how often each tag appears
    private final Map<String, Integer> tagFrequency = new HashMap<>();

    public List<Item> all() {
        return items;
    }

    public Optional<Item> find(ItemId id) {
        return items.stream().filter(i -> i.getId().equals(id)).findFirst();
    }

    public void add(Item it) {
        items.add(it);
    }

    public void remove(ItemId id) {
        items.removeIf(i -> i.getId().equals(id));
    }

    public Deque<ItemId> recentlyViewed() {
        return recentStack;
    }

    public Deque<Memento> undoStack() {
        return undoStack;
    }

    public PriorityQueue<TodoTask> taskQueue() {
        return taskQueue;
    }

    public List<TodoTask> viewAllTasksByPriority() {
        return new ArrayList<>(taskQueue);
    }

    public Map<String, Set<ItemId>> keywordIndex() {
        return keywordIndex;
    }

    public Map<String, Integer> tagFrequency() {
        return tagFrequency;
    }

    // restore item from memento snapshot
    public void apply(Memento m) {
        Item snap = m.snapshot();

        if (m.deleted()) {
            // bring back deleted item
            remove(snap.getId());
            items.add(snap);
        } else {
            // restore old field values
            find(snap.getId()).ifPresent(current -> {
                current.setTitle(snap.getTitle());
                current.setCategory(snap.getCategory());
                current.setTags(new ArrayList<>(snap.getTags()));
                current.setRating(snap.getRating());
                current.setPathOrUrl(snap.getPathOrUrl());
                current.setDescription(snap.getDescription());
                current.setCreatedAt(snap.getCreatedAt());
                current.setMediaKind(snap.getMediaKind());
            });
        }
    }

    public LibraryRepository knownPaths() {
        return null;
    }
}
