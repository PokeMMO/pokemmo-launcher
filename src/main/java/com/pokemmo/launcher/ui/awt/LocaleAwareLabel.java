package com.pokemmo.launcher.ui.awt;

import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.shared.LocaleAwareElementManager;
import com.pokemmo.launcher.ui.shared.LocaleAwareInterface;

import javax.swing.*;

/**
 * @author Kyu
 */
public class LocaleAwareLabel extends JLabel implements LocaleAwareInterface
{
	private String key;
	private Object[] params;

	public LocaleAwareLabel(String key)
	{
		this.key = key;
		this.params = new Object[0];
		super.setText(Config.getString(key));

		LocaleAwareElementManager.instance.addElement(this);
	}

	public void updateLocale()
	{
		super.setText(Config.getString(key, params));
	}

	@Override
	public void setTextKey(String key, Object... params)
	{
		this.key = key;
		this.params = params;
		super.setText(Config.getString(key, params));
	}

	@Override
	public void setToolTipKey(String tooltip, Object... params)
	{
		// Empty
	}

	@Override
	public void setText(String text)
	{
		super.setText(Config.getString(key));
	}
}
