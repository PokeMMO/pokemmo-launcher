package com.pokemmo.launcher.ui.swt;

import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.shared.LocaleAwareElementManager;
import com.pokemmo.launcher.ui.shared.LocaleAwareInterface;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * SWT {@link Button} (check style) that automatically updates its tooltip when the locale changes.
 *
 * @author Kyu
 */
public class LocaleAwareCheckbox extends Button implements LocaleAwareInterface
{
    private String tooltipKey;
    private Object[] tooltipParams;

    public LocaleAwareCheckbox(Composite parent, int style)
    {
        super(parent, style | SWT.CHECK);
        this.tooltipKey = "";
        this.tooltipParams = new Object[0];
        LocaleAwareElementManager.instance.addElement(this);
    }

    @Override
    public void setTextKey(String key, Object... params)
    {
        // Not supported for checkboxes in the current design
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
        if (tooltipKey != null && !tooltipKey.isEmpty())
        {
            super.setToolTipText(Config.getString(tooltipKey, tooltipParams));
        }
    }

    @Override
    public void dispose()
    {
        LocaleAwareElementManager.instance.removeElement(this);
        super.dispose();
    }
}
