package com.pokemmo.launcher.ui.swt;

import java.util.ArrayList;
import java.util.List;

import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.ui.shared.LocaleAwareElementManager;
import com.pokemmo.launcher.ui.shared.LocaleAwareInterface;
import com.pokemmo.launcher.ui.shared.LocaleAwareStringBundle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

/**
 * SWT {@link StyledText} (multi-line, wrapped, scrollable) that supports locale-aware
 * content re-resolution and properly respects system dark mode colors on macOS.
 *
 * @author Kyu
 */
public class LocaleAwareTextArea extends StyledText implements LocaleAwareInterface
{
    private final List<LocaleAwareStringBundle> appendedLines = new ArrayList<>();

    public LocaleAwareTextArea(Composite parent, int style)
    {
        super(parent, style | SWT.V_SCROLL);
        setWordWrap(true);
        setEditable(false);
        setLeftMargin(6);
        setRightMargin(6);
        setTopMargin(6);
        setBottomMargin(6);
        LocaleAwareElementManager.instance.addElement(this);
    }

    @Override
    protected void checkSubclass()
    {
        // Allow subclassing of SWT StyledText
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

        // Auto-scroll to bottom
        setTopIndex(getLineCount() - 1);
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
