package com.damage;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.HitsplatApplied;
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

	@Override
	protected void startUp() throws Exception
	{
		// Initialization code if needed
	}

	@Override
	protected void shutDown() throws Exception
	{
		// Cleanup code if needed
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			int damage = event.getHitsplat().getAmount();

			// Check the damage amount and play the appropriate sound
			if (damage >= 30)
			{
				playCustomSound("bigdamage.wav");
			}
			else if (damage >= 10)
			{
				playCustomSound("littledamage.wav");
			}
		}
	}

	private void playCustomSound(String soundFileName)
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

			// Play the sound
			clip.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
