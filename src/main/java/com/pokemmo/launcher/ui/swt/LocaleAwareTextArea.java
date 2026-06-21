package com.pokemmo.launcher.ui.swt;

import java.util.ArrayList;
import java.util.List;

import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.shared.LocaleAwareElementManager;
import com.pokemmo.launcher.ui.shared.LocaleAwareInterface;
import com.pokemmo.launcher.ui.shared.LocaleAwareStringBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * SWT {@link Text} (multi-line, wrapped, scrollable) that supports locale-aware
 * content re-resolution.
 *
 * @author Kyu
 */
public class LocaleAwareTextArea extends Text implements LocaleAwareInterface
{
    private final List<LocaleAwareStringBundle> appendedLines = new ArrayList<>();

    public LocaleAwareTextArea(Composite parent, int style)
    {
        super(parent, style | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        setEditable(false);
        LocaleAwareElementManager.instance.addElement(this);
    }

    @Override
    public void setTextKey(String key, Object... params)
    {
        // Default
    }

    @Override
    public void setToolTipKey(String tooltip, Object... params)
    {
        // Default
    }

    /**
     * Append a locale-aware string to the text area. The string is resolved
     * immediately and also stored for re-resolution on locale change.
     */
    public void appendLocaleStr(String str, Object... params)
    {
        var bundle = new LocaleAwareStringBundle(str, params);

        if (str.matches("\\n"))
        {
            super.append(str);
            appendedLines.add(bundle);
            return;
        }

        String resolved = Config.getString(str, params);
        appendedLines.add(bundle);
        super.append(resolved);
    }

    @Override
    public void append(String str)
    {
        throw new UnsupportedOperationException("Use locale-aware appendLocaleStr");
    }

    @Override
    public void updateLocale()
    {
        setText("");
        appendedLines.forEach(s ->
        {
            if (Config.hasString(s.getKey()))
            {
                super.append(Config.getString(s.getKey(), s.getParams()));
            }
            else
            {
                super.append(s.getKey());
            }
        });
    }

    @Override
    public void dispose()
    {
        LocaleAwareElementManager.instance.removeElement(this);
        super.dispose();
    }
}
