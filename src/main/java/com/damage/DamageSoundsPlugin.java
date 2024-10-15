package com.damage;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@PluginDescriptor(
		name = "Damage Sounds",
		description = "Plays custom sounds based on the damage you take.",
		tags = {"combat", "sound", "damage"}
)
@Slf4j
public class DamageSoundsPlugin extends Plugin {

	@Inject
	private Client client;

	@Inject
	private DamageSoundsConfig config;

	// Map to store sound files with their corresponding damage thresholds
	private Map<String, Integer> soundMap = new HashMap<>();

	// Reference to the last played Clip to close it before playing a new one
	private Clip lastClip;

	@Override
	protected void startUp() throws Exception {
		// Define path for the damagesounds directory in the user's RuneLite folder
		Path damagesoundsDir = Paths.get(System.getProperty("user.home"), ".runelite", "damagesounds");

		// Create directory if it doesn't exist
		if (!java.nio.file.Files.exists(damagesoundsDir)) {
			java.nio.file.Files.createDirectories(damagesoundsDir);
			log.debug("Created damagesounds directory: {}", damagesoundsDir);
		}

		// Parse sound files from the config into the sound map
		soundMap = parseSoundFiles(config.soundFiles());
	}

	@Provides
	DamageSoundsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(DamageSoundsConfig.class);
	}

	@Override
	protected void shutDown() throws Exception {
		// Close the last played Clip on shutdown
		closeLastClip();
	}

	// Listener for config changes related to sound files
	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals("damagesounds") && event.getKey().equals("soundFiles")) {
			log.debug("Sound files config changed, reloading sound mappings.");
			soundMap = parseSoundFiles(config.soundFiles());
		}
	}

	// Listener for damage events (HitsplatApplied)
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event) {
		if (event.getActor() == client.getLocalPlayer()) {
			int damage = event.getHitsplat().getAmount();
			playSoundForDamage(damage);
		}
	}

	// Play the sound that corresponds to the damage amount
	private void playSoundForDamage(int damage) {
		String matchingSound = null;
		int highestThreshold = -1;

		// Find the highest threshold that matches the damage
		for (Map.Entry<String, Integer> entry : soundMap.entrySet()) {
			int threshold = entry.getValue();
			if (damage >= threshold && threshold > highestThreshold) {
				matchingSound = entry.getKey();
				highestThreshold = threshold;
			}
		}

		// Play the corresponding sound, if found
		if (matchingSound != null) {
			playCustomSound(matchingSound, config.volume());
		} else {
			log.debug("No matching sound found for damage: {}", damage);
		}
	}

	// Parse the sound file mappings from config (format: "soundFileName:damageThreshold")
	private Map<String, Integer> parseSoundFiles(String soundFiles) {
		Map<String, Integer> soundMap = new HashMap<>();
		String[] pairs = soundFiles.split(",");

		for (String pair : pairs) {
			String[] parts = pair.split(":");
			if (parts.length == 2) {
				String soundFileName = parts[0].trim();
				Integer threshold = Integer.valueOf(parts[1].trim());
				soundMap.put(soundFileName, threshold);
			}
		}

		return soundMap;
	}

	// Plays a custom sound file based on its name and the configured volume
	private void playCustomSound(String soundFileName, int volume) {
		try {
			// First, try to find the custom sound in the user's damagesounds directory
			Path customSoundPath = Paths.get(System.getProperty("user.home"), ".runelite", "damagesounds", soundFileName);
			File customSoundFile = customSoundPath.toFile();

			log.debug("Looking for custom sound file: {}", customSoundFile.getAbsolutePath());

			if (customSoundFile.exists()) {
				// Play the custom sound file if found
				playClip(AudioSystem.getAudioInputStream(customSoundFile), volume);
			} else {
				// If the custom sound isn't found, try to load a default sound from the plugin's JAR
				InputStream audioSrc = getClass().getResourceAsStream("/" + soundFileName);
				if (audioSrc != null) {
					InputStream bufferedIn = new BufferedInputStream(audioSrc);
					playClip(AudioSystem.getAudioInputStream(bufferedIn), volume);
				} else {
					log.debug("Sound file not found in JAR or custom directory: {}", soundFileName);
				}
			}
		} catch (Exception e) {
			log.error("Error playing sound: {}", soundFileName, e);
		}
	}

	// Play an audio clip with the specified volume
	private void playClip(AudioInputStream audioStream, int volume) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		closeLastClip(); // Close the previous clip if it is still open

		Clip clip = AudioSystem.getClip();
		clip.open(audioStream);

		// Adjust volume
		FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		float volumeAdjustment = 20f * (float) Math.log10(volume / 100.0);
		gainControl.setValue(volumeAdjustment);

		// Start playing the sound
		clip.start();
		lastClip = clip; // Store the reference to the current clip
	}

	// Close the last played Clip to free resources
	private void closeLastClip() {
		if (lastClip != null && lastClip.isOpen()) {
			lastClip.stop();
			lastClip.close();
		}
	}
}
