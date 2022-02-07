package com.anas.jconsoleaudioplayer.player.players;

import com.anas.jconsoleaudioplayer.player.Extension;
import com.anas.jconsoleaudioplayer.player.Player;
import com.anas.jconsoleaudioplayer.player.PlayersAdaptor;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class WAVPlayer extends Player {
    private Clip clip;
    private AudioInputStream audioInputStream;
    private boolean isLooping, isMuted, paused, userStopped, running;
    private double soundLevel, soundLevelBeforeMute;

    /**
     * Constructor for WavePlayer
     */
    public WAVPlayer(PlayersAdaptor playersAdaptor) {
        super(playersAdaptor);
        isLooping = false;
        soundLevel = 0.500;
        isMuted = false;
        paused = false;
        userStopped = false;
        running = false;
    }

    /**
     * Plays the song in the playlist
     * @throws LineUnavailableException if line is unavailable
     * @throws IOException              if file is not found
     */
    @Override
    public void play(File audioFile) throws Exception {
        if (!running) {
            try {
                audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            userStopped = false;

            // Set the volume
            setVolume(soundLevel);

            clip.start();
            running = true;
            clip.addLineListener(event -> {
                if (!paused && !userStopped || clip.getFramePosition() == clip.getFrameLength()) {
                    super.sendEvent(event.getType());
                    running = false;
                }
            });
        }
    }

    @Override
    public void run() {
//        try {
//            play();
//        } catch (LineUnavailableException | IOException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Stops the song
     */
    @Override
    public void stop() {
        clip.setMicrosecondPosition(0L);
        clip.stop();
        clip.close();
        userStopped = true;
        running = false;
    }

    /**
     * Pauses the song
     */
    @Override
    public void pause() {
        clip.stop();
        paused = true;
    }

    /**
     * Resumes the playing of the song
     */
    @Override
    public void resume() {
        clip.start();
        paused = false;
    }

    /**
     * Enable and disable looping of the song
     */
    @Override
    public void loop() {
        if (isLooping) {
            clip.loop(0); // stop looping
            isLooping = false;
        } else {
            clip.loop(Clip.LOOP_CONTINUOUSLY); // start looping
            isLooping = true;
        }
    }

    /**
     * Mute and unmute the song
     */
    @Override
    public void mute() {
        if (isMuted) {
            setVolume(soundLevelBeforeMute);
            isMuted = false;
        } else {
            soundLevelBeforeMute = soundLevel;
            setVolume(0.0);
            isMuted = true;
        }
    }

    /**
     * Get the current volume of the song
     * @return the volume
     */
    @Override
    public double getVolume() {
        return soundLevel;
    }

    /**
     * Set the volume of the song
     * @param volume the volume of the song
     */
    @Override
    public void setVolume(double volume) {
        if (volume < 0.0 || volume > 1.0) {
            System.out.println("Volume must be between 0 and 1, volume = " + volume);
            return;
        }
        soundLevel = volume;
        if (clip != null) {
            userStopped = true;
            if (!paused)
                clip.stop();
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(db);
            if (!paused)
                clip.start();
        }
    }

    @Override
    public boolean isSupportedFile(File file) {
        return file.getName().toUpperCase().endsWith(Extension.WAV.name());
    }

    /**
     * Stop and close the player
     */
    @Override
    public void exit() {
        stop();
        clip.close();
    }
}
