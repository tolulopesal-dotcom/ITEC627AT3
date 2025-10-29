// src/main/java/org/example/IndexService.java
package org.example;

import java.util.*;


public class IndexService {

    // rebuild search index from scratch
    public void reindex(LibraryRepository repo) {
        repo.keywordIndex().clear();
        repo.tagFrequency().clear();

        for (Item it : repo.all()) {
            // index title and description words
            indexText(repo.keywordIndex(), it.getId(), it.getTitle());
            indexText(repo.keywordIndex(), it.getId(), it.getDescription());

            // index tags and count frequency
            for (String tag : it.getTags()) {
                String norm = norm(tag);
                repo.tagFrequency().put(
                        norm,
                        repo.tagFrequency().getOrDefault(norm, 0) + 1
                );
                repo.keywordIndex()
                        .computeIfAbsent(norm, k -> new HashSet<>())
                        .add(it.getId());
            }
        }
    }

    // split text into words and add to index
    private void indexText(Map<String, Set<ItemId>> kwIndex, ItemId id, String text) {
        if (text == null) return;
        for (String raw : text.split("[^A-Za-z0-9]+")) {
            if (raw.isBlank()) continue;
            String norm = norm(raw);
            kwIndex.computeIfAbsent(norm, k -> new HashSet<>()).add(id);
        }
    }

    private String norm(String s) {
        return s.toLowerCase(Locale.ROOT).trim();
    }
}
