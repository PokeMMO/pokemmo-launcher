package com.pokemmo.launcher.ui.swt;

import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.shared.LocaleAwareElementManager;
import com.pokemmo.launcher.ui.shared.LocaleAwareInterface;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * SWT {@link Button} (push style) that automatically updates its text when the locale changes.
 *
 * @author Kyu
 */
public class LocaleAwareButton extends Button implements LocaleAwareInterface
{
    private String key;
    private Object[] params;
    private String tooltipKey;
    private Object[] tooltipParams;

    public LocaleAwareButton(Composite parent, int style, String key)
    {
        super(parent, style);
        this.key = key;
        this.params = new Object[0];
        this.tooltipKey = "";
        this.tooltipParams = new Object[0];
        super.setText(Config.getString(key));
        LocaleAwareElementManager.instance.addElement(this);
    }

    @Override
    protected void checkSubclass()
    {
        // Allow subclassing of SWT Button
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
        this.tooltipKey = tooltip;
        this.tooltipParams = params;
        super.setToolTipText(Config.getString(tooltip, params));
    }

    @Override
    public void updateLocale()
    {
        super.setText(Config.getString(key, params));
        if (tooltipKey != null && !tooltipKey.isEmpty())
        {
            super.setToolTipText(Config.getString(tooltipKey, tooltipParams));
        }
        Composite parent = getParent();
        if (parent != null && !parent.isDisposed())
        {
            parent.layout();
        }
    }

    @Override
    public void dispose()
    {
        LocaleAwareElementManager.instance.removeElement(this);
        super.dispose();
    }
}
