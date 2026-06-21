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

    /**
     * Converts bytes to a human-readable string (e.g. "1.5 MiB/s").
     * Ported from {@code MainFrame.humanReadableByteCount}.
     */
    public static String humanReadableByteCount(long bytes, boolean si)
    {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
