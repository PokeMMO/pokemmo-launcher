package com.pokemmo.launcher.ui.shared;

import java.io.IOException;
import java.io.InputStream;

/**
 * Pure-classpath utility for accessing the application icon.
 * <p>
 * This class does <b>not</b> depend on AWT, SWT, or any UI toolkit.
 * Platform-specific icon loading is delegated to:
 * <ul>
 *   <li>{@code com.pokemmo.launcher.ui.awt.AwtIconLoader} (AWT/Swing)</li>
 *   <li>{@code com.pokemmo.launcher.ui.swt.SwtIconLoader} (SWT)</li>
 * </ul>
 * <p>
 * @author Desu
 */
public final class IconLoader
{
	/** Standard desktop icon sizes. */
	public static final int[] STANDARD_SIZES = { 16, 24, 32, 48, 64, 128, 256 };

    /**
     * Opens an {@link InputStream} for the source icon PNG.
     * <p>
     * The caller is responsible for closing the stream.
     *
     * @return a new InputStream for the icon resource
     * @throws IOException if the resource cannot be found
     */
    public static InputStream openStream(int size) throws IOException
	{
		String path = "/icons/icon_" + size + "x" + size + ".png";
		InputStream in = IconLoader.class.getResourceAsStream(path);
		if(in == null)
			throw new IOException("Resource not found: " + path);
		return in;
	}
}
