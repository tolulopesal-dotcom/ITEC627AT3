// src/main/java/org/example/SearchService.java
package org.example;

import java.util.*;

public class SearchService {

    // search and rank results by relevance
    public List<ItemId> rankedSearch(LibraryRepository repo, String query) {
        if (query == null || query.isBlank()) return List.of();

        String[] words = query.toLowerCase(Locale.ROOT).split("\\s+");
        Map<ItemId, Integer> scoreMap = new HashMap<>();

        // score each item based on keyword matches
        for (String w : words) {
            Set<ItemId> hits = repo.keywordIndex().get(w);
            if (hits == null) continue;
            for (ItemId id : hits) {
                // boost score for popular tags
                int scoreBump = 1 + repo.tagFrequency().getOrDefault(w, 0);
                scoreMap.put(id, scoreMap.getOrDefault(id, 0) + scoreBump);
            }
        }

        // sort results by score (highest first)
        PriorityQueue<Map.Entry<ItemId,Integer>> pq =
                new PriorityQueue<>((a,b) -> Integer.compare(b.getValue(), a.getValue()));

        pq.addAll(scoreMap.entrySet());

        List<ItemId> ordered = new ArrayList<>();
        while (!pq.isEmpty()) {
            ordered.add(pq.poll().getKey());
        }
        return ordered;
    }
}
