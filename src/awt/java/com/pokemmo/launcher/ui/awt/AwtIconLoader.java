package com.pokemmo.launcher.ui.awt;

import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.pokemmo.launcher.ui.shared.IconLoader;

import javax.imageio.ImageIO;

/**
 * AWT/Swing-specific icon loading.
 * <p>
 * Decodes the raw PNG bytes from {@link IconLoader} into
 * {@link java.awt.Image} instances suitable for
 * {@link java.awt.Window#setIconImages}.
 *
 * @author Desu
 */
public final class AwtIconLoader
{
    /**
     * Returns a list of {@code java.awt.Image} instances at all standard sizes,
     * appropriate for {@link java.awt.Window#setIconImages(java.util.List)}.
     */
    public static List<Image> getImages()
    {
        return getImages(IconLoader.STANDARD_SIZES);
    }

	private static List<Image> getImages(int... sizes)
	{
		List<Image> images = new ArrayList<>(sizes.length);
		for (int size : sizes)
		{
			try(InputStream in = IconLoader.openStream(size))
			{
				images.add(ImageIO.read(in));
			}
			catch(Exception e)
			{
				throw new RuntimeException("Failed to create SWT icon images", e);
			}
		}

		return images;
	}
}
