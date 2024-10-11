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
			description = "Adjust the volume of the sounds.",
			position = 1
	)
	default int volume()
	{
		return 100; // Default volume is 100%
	}
}
