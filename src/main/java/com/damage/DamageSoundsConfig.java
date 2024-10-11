package com.damage;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("damagesounds")
public interface DamageSoundsConfig extends Config
{
	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Adjust the volume of the sounds.",
			position = 1
	)
	default int volume()
	{
		return 100; // Default volume is 100%
	}

	@ConfigItem(
			keyName = "soundFiles",
			name = "Sound Files",
			description = "List of sounds with damage thresholds (format: soundFileName:threshold, ...)",
			position = 2
	)
	default String soundFiles()
	{
		return "littledamage.wav:10,bigdamage.wav:30"; // Default sound mappings
	}
}