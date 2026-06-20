package com.pokemmo.launcher.enums;

import java.security.PublicKey;
import java.util.stream.Stream;

import com.pokemmo.launcher.util.CryptoUtil;

/**
 * @author Kyu
 */
public enum UpdateChannel
{
	live(true),
	pts(false),
	testserver(false),
	testserver2(false);

	public static final UpdateChannel[] ENABLED_UPDATE_CHANNELS;

	static
	{
		ENABLED_UPDATE_CHANNELS = Stream.of(values()).filter(UpdateChannel::isSelectable).toArray(UpdateChannel[]::new);
	}

	private final boolean selectable;

	UpdateChannel(boolean selectable)
	{
		this.selectable = selectable;
	}

	public PublicKey getPublicKey()
	{
		return switch (this)
		{
			case testserver, testserver2 -> CryptoUtil.getTestPublicKey();
			default -> CryptoUtil.getLivePublicKey();
		};
	}

	public boolean isSelectable()
	{
		return selectable;
	}

	public String[] getMirrors()
	{
		return switch(this)
		{
			case testserver -> new String[]{
					"https://testserver.pokemmo.com",
			};
			case testserver2 -> new String[]{
					"https://testserver2.pokemmo.com",
			};
			default -> new String[]{
					"https://dl.pokemmo.com",
					"https://files.pokemmo.com",
					"https://dl.pokemmo.eu",
					"https://files.pokemmo.eu",
					"https://dl.pokemmo.download"
			};
		};
	}

	public String urlComponent()
	{
		//testserver2 uses "testserver" but a different mirror url
		if(this == UpdateChannel.testserver2)
			return testserver.urlComponent();
		return name();
	}
}
