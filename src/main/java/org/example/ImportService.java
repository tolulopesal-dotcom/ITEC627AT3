package org.example;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ImportService {

    // file types we support
    private static final List<String> ALLOWED_EXT = List.of(
            ".txt", ".md", ".pdf", ".mp3", ".mp4"
    );

    // recursively import files from folder
    public int importFolder(LibraryRepository repo, File rootDir, IndexService indexService) {
        if (rootDir == null || !rootDir.exists()) {
            return 0;
        }

        // track existing files to avoid duplicates
        Set<String> knownPaths = repo.all().stream()
                .map(Item::getPathOrUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int importedCount = walkRecursive(repo, rootDir, knownPaths);

        // rebuild index after import
        indexService.reindex(repo);

        return importedCount;
    }

    // walk directory tree using stack to avoid recursion limits
    private int walkRecursive(LibraryRepository repo, File start, Set<String> knownPaths) {
        int count = 0;

        Deque<File> stack = new ArrayDeque<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            File node = stack.pop();

            if (node.isDirectory()) {
                // add subdirectories to stack
                File[] kids = node.listFiles();
                if (kids != null) {
                    for (File k : kids) {
                        stack.push(k);
                    }
                }
            } else {
                String nameLower = node.getName().toLowerCase(Locale.ROOT);

                // check if file type is supported
                boolean allowed = ALLOWED_EXT.stream().anyMatch(nameLower::endsWith);
                if (!allowed) {
                    continue;
                }

                String absPath = node.getAbsolutePath();
                if (knownPaths.contains(absPath)) {
                    continue;
                }

                // figure out category from file extension
                ItemCategory category;
                MediaKind mediaKind;
                if (nameLower.endsWith(".mp3")) {
                    category = ItemCategory.AUDIO;
                    mediaKind = MediaKind.AUDIO;
                } else if (nameLower.endsWith(".mp4")) {
                    category = ItemCategory.VIDEO;
                    mediaKind = MediaKind.VIDEO;
                } else if (nameLower.endsWith(".pdf")) {
                    category = ItemCategory.DOCUMENT;
                    mediaKind = MediaKind.OTHER;
                } else {
                    category = ItemCategory.NOTE;
                    mediaKind = MediaKind.OTHER;
                }

                Item item = new Item(node.getName());
                item.setCategory(category);
                item.setMediaKind(mediaKind);
                item.setPathOrUrl(absPath);
                item.setRating(0);
                item.setTags(List.of(extensionOf(node.getName())));
                item.setCreatedAt(Instant.now());
                item.setDescription("");

                repo.add(item);
                knownPaths.add(absPath);
                count++;
            }
        }

        return count;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            return filename.substring(dot).toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
