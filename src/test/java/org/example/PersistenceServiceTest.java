package org.example;

import org.testng.annotations.Test;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Files;


public class PersistenceServiceTest {
    @Test
    public void roundTrip() throws Exception {
        var repo = new LibraryRepository(); var it = new Item("Test"); repo.put(it);
        var file = Files.createTempFile("lib", ".scol"); new PersistenceService().save(repo, file);
        var loaded = new PersistenceService().load(file);
        Assertions.assertTrue(loaded.all().stream().anyMatch(x -> x.getTitle().equals("Test")));
    }
}