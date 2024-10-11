package com.damage;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

@PluginDescriptor(
		name = "Damage Sounds",
		description = "Plays custom sounds based on the damage you take.",
		tags = {"combat", "sound", "damage"}
)
public class DamageSoundsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DamageSoundsConfig config;

	@Inject
	private ConfigManager configManager;

	@Override
	protected void startUp() throws Exception
	{
		// Initialization code if needed
	}

	@Provides
	DamageSoundsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DamageSoundsConfig.class);
	}

	@Override
	protected void shutDown() throws Exception
	{
		// Cleanup code if needed
	}

	@Subscribe
	public void onHitsplatApplied(net.runelite.api.events.HitsplatApplied event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			int damage = event.getHitsplat().getAmount();

			// Check the damage amount and play the appropriate sound
			if (damage >= 30)
			{
				playCustomSound("bigdamage.wav", config.volume());
			}
			else if (damage >= 10)
			{
				playCustomSound("littledamage.wav", config.volume());
			}
		}
	}

	private void playCustomSound(String soundFileName, int volume)
	{
		try
		{
			// Load the sound file from the resources folder
			InputStream audioSrc = getClass().getResourceAsStream("/" + soundFileName);
			InputStream bufferedIn = new BufferedInputStream(audioSrc);
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);

			// Get a sound clip resource
			Clip clip = AudioSystem.getClip();
			clip.open(audioStream);

			// Set volume
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float volumeAdjustment = 20f * (float) Math.log10(volume / 100.0);
			gainControl.setValue(volumeAdjustment);

			// Play the sound
			clip.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
