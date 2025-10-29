// src/main/java/org/example/TodoTask.java
package org.example;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

public class TodoTask implements Serializable, Comparable<TodoTask> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String title;
    private final Instant createdAt;
    private final LocalDate dueDate;

    public TodoTask(String title, Instant createdAt, LocalDate dueDate) {
        this.title = title;
        this.createdAt = createdAt;
        this.dueDate = dueDate;
    }

    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public LocalDate getDueDate() { return dueDate; }

    public String formatForList() {
        return dueDate == null
                ? (title + " (no deadline)")
                : (title + " (due " + dueDate + ")");
    }

    // sort by due date (earliest first)
    @Override
    public int compareTo(TodoTask other) {
        if (this.dueDate == null && other.dueDate == null) {
            return this.createdAt.compareTo(other.createdAt);
        }
        // tasks without deadlines go to the end
        if (this.dueDate == null) return 1;
        if (other.dueDate == null) return -1;
        return this.dueDate.compareTo(other.dueDate);
    }
}
