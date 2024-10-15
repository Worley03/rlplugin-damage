package com.damage;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("damagesounds")
public interface DamageSoundsConfig extends Config
{
	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Adjust the volume of all sounds.",
			position = 1
	)
	default int volume()
	{
		return 100; // Default volume is 100%
	}

	@ConfigItem(
			keyName = "soundFiles",
			name = "Sound Files",
			description = "FORMAT: soundfile1.wav:10,soundfile2.wav:20",
			position = 2
	)
	default String soundFiles()
	{
		return "littledamage.wav:10,bigdamage.wav:30"; // Default sound mappings
	}
}