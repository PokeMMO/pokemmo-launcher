package com.pokemmo.launcher.ui.swt;

import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.shared.LocaleAwareElementManager;
import com.pokemmo.launcher.ui.shared.LocaleAwareInterface;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * SWT {@link Label} that automatically updates its text when the locale changes.
 *
 * @author Kyu
 */
public class LocaleAwareLabel extends Label implements LocaleAwareInterface
{
    private String key;
    private Object[] params;

    public LocaleAwareLabel(Composite parent, int style, String key)
    {
        super(parent, style);
        this.key = key;
        this.params = new Object[0];
        super.setText(Config.getString(key));
        LocaleAwareElementManager.instance.addElement(this);
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
        super.setToolTipText(Config.getString(tooltip, params));
    }

    @Override
    public void updateLocale()
    {
        super.setText(Config.getString(key, params));
    }

    @Override
    public void dispose()
    {
        LocaleAwareElementManager.instance.removeElement(this);
        super.dispose();
    }
}
