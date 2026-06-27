package com.pokemmo.launcher.ui.swt;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.pokemmo.launcher.ui.shared.IconLoader;

/**
 * SWT-specific icon loading for the PokeMMO launcher.
 * The caller is responsible for disposing the returned {@link Image} objects.
 * @author Desu
 */
public final class SwtIconLoader
{
    private SwtIconLoader() { }

    /**
     * Returns a list of SWT {@link Image} objects at all standard sizes,
     * appropriate for {@link org.eclipse.swt.widgets.Shell#setImages(java.util.List)}.
     * <p>
     * The caller must dispose each returned {@code Image} when no longer needed
     * (e.g. when the shell is disposed).
     *
     * @param display the SWT Display
     * @return list of Images from smallest to largest size
     * @throws IllegalStateException if the source icon could not be loaded
     */
    public static Image[] getImages(Display display)
    {
        return getImages(display, IconLoader.STANDARD_SIZES);
    }

    /**
     * Returns a list of SWT {@link Image} objects at the specified sizes.
     *
     * @param display the SWT Display
     * @param sizes   desired pixel sizes
     * @return list of Images in the same order as {@code sizes}
     * @throws IllegalStateException if the source icon could not be loaded
     */
    public static Image[] getImages(Display display, int... sizes)
    {
        List<Image> images = new ArrayList<>(sizes.length);
		for (int size : sizes)
		{
			try(InputStream in = IconLoader.openStream(size))
			{
				images.add(new Image(display, in));
			}
			catch(Exception e)
			{
				throw new RuntimeException("Failed to create SWT icon images", e);
			}
		}
		return images.toArray(Image[]::new);
    }
}
