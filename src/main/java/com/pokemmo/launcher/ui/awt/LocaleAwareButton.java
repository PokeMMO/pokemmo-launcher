package com.pokemmo.launcher.ui.awt;

import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.shared.LocaleAwareElementManager;
import com.pokemmo.launcher.ui.shared.LocaleAwareInterface;

import javax.swing.*;

/**
 * @author Kyu
 */
public class LocaleAwareButton extends JButton implements LocaleAwareInterface
{
	private String key;
	private Object[] params;
	private String tooltip;

	public LocaleAwareButton(String key)
	{
		this.key = key;
		this.params = new Object[0];
		this.tooltip = "";
		init(key, null);

		LocaleAwareElementManager.instance.addElement(this);
	}

	@Override
	protected void init(String text, Icon icon)
	{
		if(text != null)
		{
			super.setText(Config.getString(text));
		}

		// Set the UI
		updateUI();

		setAlignmentX(LEFT_ALIGNMENT);
		setAlignmentY(CENTER_ALIGNMENT);
	}

	@Override
	public void setTextKey(String key, Object... params)
	{
		this.key = key;
		this.params = params;
		super.setText(Config.getString(key, params));
	}

	@Override
	public void setText(String key)
	{
		throw new UnsupportedOperationException("Must use locale-aware constructor");
	}

	@Override
	public void setToolTipKey(String key, Object... params)
	{
		this.tooltip = key;
		super.setToolTipText(Config.getString(key, params));
	}

	@Override
	public void setToolTipText(String key)
	{
		throw new UnsupportedOperationException("Must use locale-aware tooltip constructor");
	}

	@Override
	public void updateLocale()
	{
		super.setText(Config.getString(key, params));
		super.setToolTipText(Config.getString(tooltip));
	}
}
