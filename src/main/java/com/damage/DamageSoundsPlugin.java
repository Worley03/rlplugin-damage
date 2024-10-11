package com.damage;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

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
	private OverlayManager overlayManager;

	@Inject
	private DamageSoundsConfig config;

	@Inject
	private ConfigManager configManager;

	private Map<String, Integer> soundMap = new HashMap<>();

	@Override
	protected void startUp() throws Exception {
		// Check and create the damagesounds directory if it does not exist
		Path damagesoundsDir = Paths.get(System.getProperty("user.home"), ".runelite", "damagesounds");
		if (!java.nio.file.Files.exists(damagesoundsDir)) {
			java.nio.file.Files.createDirectories(damagesoundsDir);
			System.out.println("Created damagesounds directory: " + damagesoundsDir.toString());
		}

		// Parse sound files initially
		soundMap = parseSoundFiles(config.soundFiles());
	}
	@Provides
	DamageSoundsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(DamageSoundsConfig.class);
	}

	@Override
	protected void shutDown() throws Exception {
		// Cleanup code if needed
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals("damagesounds") && event.getKey().equals("soundFiles")) {
			System.out.println("Sound files config changed, reloading sound mappings.");
			soundMap = parseSoundFiles(config.soundFiles()); // Update sound mappings
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event) {
		if (event.getActor() == client.getLocalPlayer()) {
			int damage = event.getHitsplat().getAmount();
			playSoundForDamage(damage);
		}
	}

	private void playSoundForDamage(int damage) {
		String selectedSound = null;
		int highestThreshold = -1; // Keep track of the highest matching threshold

		// Iterate over soundMap and find the highest matching threshold
		for (Map.Entry<String, Integer> entry : soundMap.entrySet()) {
			int threshold = entry.getValue();
			if (damage >= threshold && threshold > highestThreshold) {
				selectedSound = entry.getKey();
				highestThreshold = threshold;
			}
		}

		// Play the sound with the highest threshold
		if (selectedSound != null) {
			playCustomSound(selectedSound, config.volume());
		} else {
			System.out.println("No matching sound found for damage: " + damage);
		}
	}


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

	private void playCustomSound(String soundFileName, int volume) {
		try {
			// First, check the .runelite directory for the custom sound
			Path customSoundPath = Paths.get(System.getProperty("user.home"), ".runelite", "damagesounds", soundFileName);
			File customSoundFile = customSoundPath.toFile();

			System.out.println("Looking for custom sound file: " + customSoundFile.getAbsolutePath());

			if (customSoundFile.exists()) {
				// If the custom sound exists, play it
				playClip(AudioSystem.getAudioInputStream(customSoundFile), volume);
			} else {
				// If custom sound is not found, try to load the default sound from the JAR
				InputStream audioSrc = getClass().getResourceAsStream("/" + soundFileName);
				if (audioSrc != null) {
					InputStream bufferedIn = new BufferedInputStream(audioSrc);
					playClip(AudioSystem.getAudioInputStream(bufferedIn), volume); // Play sound from the JAR
				} else {
					System.out.println("Sound file not found in JAR or custom directory: " + soundFileName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void playClip(AudioInputStream audioStream, int volume) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		Clip clip = AudioSystem.getClip();
		clip.open(audioStream);

		// Set volume
		FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		float volumeAdjustment = 20f * (float) Math.log10(volume / 100.0);
		gainControl.setValue(volumeAdjustment);

		clip.start();
	}
}
