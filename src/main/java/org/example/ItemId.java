// src/main/java/org/example/ItemId.java
package org.example;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class ItemId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String value;

    public ItemId(String v) {
        this.value = v;
    }

    public String value() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
