// src/main/java/org/example/MediaService.java
package org.example;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class MediaService {

    private MediaPlayer currentPlayer;

    // setup media player for file
    public void prepare(String pathOrUrl) {
        // clean up old player
        if (currentPlayer != null) {
            currentPlayer.dispose();
            currentPlayer = null;
        }
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return;
        }

        // create new player from file path
        Media media = new Media(new java.io.File(pathOrUrl).toURI().toString());
        currentPlayer = new MediaPlayer(media);
    }

    // attach player to UI component
    public void apply(MediaView view) {
        view.setMediaPlayer(currentPlayer);
    }
}
