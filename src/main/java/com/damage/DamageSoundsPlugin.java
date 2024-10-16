package com.damage;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@PluginDescriptor(
		name = "Damage Sounds",
		description = "Plays custom sounds based on the damage you take.",
		tags = {"combat", "sound", "damage"}
)
public class DamageSoundsPlugin extends Plugin {

	@Inject
	private Client client;

	@Inject
	private DamageSoundsConfig config;

	private static final Logger log = LoggerFactory.getLogger(DamageSoundsPlugin.class);

	// Map to store sound files with their corresponding damage thresholds
	private Map<String, Integer> soundMap = new HashMap<>();

	@Override
	protected void startUp() throws Exception {
		// Define path for the damagesounds directory in the user's RuneLite folder
		Path damagesoundsDir = Paths.get(System.getProperty("user.home"), ".runelite", "damagesounds");

		// Create directory if it doesn't exist
		if (!java.nio.file.Files.exists(damagesoundsDir)) {
			java.nio.file.Files.createDirectories(damagesoundsDir);
			log.debug("Created damagesounds directory: {}", damagesoundsDir.toString());
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
		// Any shutdown or cleanup logic if necessary
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
		AudioInputStream audioStream = null;
		Clip clip = null;
		try {
			// First, try to find the custom sound in the user's damagesounds directory
			Path customSoundPath = Paths.get(System.getProperty("user.home"), ".runelite", "damagesounds", soundFileName);
			File customSoundFile = customSoundPath.toFile();

			log.debug("Looking for custom sound file: {}", customSoundFile.getAbsolutePath());

			if (customSoundFile.exists()) {
				// Play the custom sound file if found
				audioStream = AudioSystem.getAudioInputStream(customSoundFile);
				clip = playClip(audioStream, volume);
			} else {
				// If the custom sound isn't found, try to load a default sound from the plugin's JAR
				InputStream audioSrc = getClass().getResourceAsStream("/" + soundFileName);
				if (audioSrc != null) {
					InputStream bufferedIn = new BufferedInputStream(audioSrc);
					audioStream = AudioSystem.getAudioInputStream(bufferedIn);
					clip = playClip(audioStream, volume);
				} else {
					log.debug("Sound file not found in JAR or custom directory: {}", soundFileName);
				}
			}

			// Add a listener to close the clip after it finishes playing
			if (clip != null) {
				Clip finalClip = clip;
				clip.addLineListener(event -> {
					if (event.getType() == LineEvent.Type.STOP) {
						finalClip.close();  // Close the clip after it finishes playing
					}
				});
			}

		} catch (Exception e) {
			log.error("Error playing sound file: {}", soundFileName, e);
		} finally {
			// Ensure that the AudioInputStream is closed after use
			if (audioStream != null) {
				try {
					audioStream.close();
				} catch (IOException e) {
					log.error("Error closing AudioInputStream for sound file: {}", soundFileName, e);
				}
			}
		}
	}

	// Play an audio clip with the specified volume
	private Clip playClip(AudioInputStream audioStream, int volume) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		Clip clip = AudioSystem.getClip();
		clip.open(audioStream);

		// Adjust volume
		FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		float volumeAdjustment = 20f * (float) Math.log10(volume / 100.0);
		gainControl.setValue(volumeAdjustment);

		// Start playing the sound
		clip.start();

		return clip;  // Return the clip so it can be closed later
	}
}