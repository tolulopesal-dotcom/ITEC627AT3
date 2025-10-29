// src/main/java/org/example/PersistenceService.java
package org.example;

import java.io.*;

public class PersistenceService {

    private static final String MAGIC = "SCOL";
    private static final int VERSION = 1;

    // save library to binary file
    public void saveTo(File file, LibraryRepository repo) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             DataOutputStream dos = new DataOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            // write file header
            dos.writeUTF(MAGIC);
            dos.writeInt(VERSION);

            oos.writeObject(repo);
            oos.flush();
        }
    }

    // load library from binary file
    public void loadFrom(File file, LibraryRepository repo) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            // check file header
            String magic = dis.readUTF();
            int ver = dis.readInt();
            if (!MAGIC.equals(magic)) {
                throw new IOException("Not a Smart Collections file.");
            }
            if (ver > VERSION) {
                throw new IOException("Unsupported version: " + ver);
            }

            LibraryRepository loaded = (LibraryRepository) ois.readObject();

            // copy data into existing repo instance
            repo.all().clear();
            repo.all().addAll(loaded.all());

            repo.recentlyViewed().clear();
            repo.recentlyViewed().addAll(loaded.recentlyViewed());

            repo.undoStack().clear();
            repo.undoStack().addAll(loaded.undoStack());

            repo.taskQueue().clear();
            repo.taskQueue().addAll(loaded.taskQueue());

            repo.keywordIndex().clear();
            repo.keywordIndex().putAll(loaded.keywordIndex());

            repo.tagFrequency().clear();
            repo.tagFrequency().putAll(loaded.tagFrequency());
        }
    }
}
