package com.pokemmo.launcher.ui.swt;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * Utility helpers for SWT UI code.
 *
 * @author Kyu
 */
public final class SwtUtil
{
    private SwtUtil()
    {
    }

    /**
     * Creates a monospaced font with the given height. The caller is responsible for
     * disposing the returned {@link Font}.
     */
    public static Font createMonospacedFont(Display display, int height)
    {
        FontData[] fontData = display.getFontList("monospace", true);
        if (fontData.length > 0)
        {
            return new Font(display, fontData[0].getName(), height, org.eclipse.swt.SWT.NORMAL);
        }
        // fallback: "Courier New" typically available everywhere
        return new Font(display, "Courier New", height, org.eclipse.swt.SWT.NORMAL);
    }

}
