// src/main/java/org/example/Memento.java
package org.example;

import java.io.Serial;
import java.io.Serializable;

public class Memento implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Item snapshot;
    private final boolean deleted;

    public Memento(Item snapshot, boolean deleted) {
        this.snapshot = snapshot;
        this.deleted = deleted;
    }

    public Item snapshot() { return snapshot; }
    public boolean deleted() { return deleted; }
}
