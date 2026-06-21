package com.pokemmo.launcher.ui;

/**
 * Thread-safe UI interface for the PokeMMO Launcher.
 * <p>
 * All methods in this interface are guaranteed to be callable from any thread.
 * Implementations MUST handle dispatching to the UI thread internally using
 * {@code Display.asyncExec()}/{@code Display.syncExec()} (SWT) or
 * {@code SwingUtilities.invokeLater()}/{@code SwingUtilities.invokeAndWait()} (AWT/Swing).
 *
 * @author Kyu
 */
public interface LauncherUI
{

	/**
	 * If necessary calls open.
	 */
	default void open()
	{

	}

	/**
     * Set the status label text and optionally add a detail line.
     *
     * @param key      locale key for the status text
     * @param progress progress percentage (0-100), or 0 for indeterminate
     * @param params   format parameters for the locale string
     */
    void setStatus(String key, int progress, Object... params);

    /**
     * Append a detail line to the task output area.
     *
     * @param string   locale key or raw text
     * @param progress progress percentage (0-100), or 0 for indeterminate
     * @param params   format parameters for the locale string
     */
    void addDetail(String string, int progress, Object... params);

    /**
     * Update the progress bar position.
     *
     * @param progress progress percentage (0-100), or 0 for indeterminate
     */
    void updateProgress(int progress);

    /**
     * Update the download speed label.
     *
     * @param bytes_per_second download speed in bytes/s, or negative to clear
     */
    void updateDLSpeed(long bytes_per_second);

    /**
     * Show an informational message dialog.
     */
    void showMessage(String message, String windowTitle);

    /**
     * Show an informational message dialog with a callback.
     */
    void showMessage(String message, String windowTitle, Runnable runnable);

    /**
     * Show an error dialog.
     */
    void showError(String message, String windowTitle);

    /**
     * Show an error dialog with a callback.
     */
    void showError(String message, String windowTitle, Runnable runnable);

    /**
     * Show an error dialog with a stacktrace string.
     */
    void showErrorWithStacktrace(String message, String windowTitle, String stacktrace, Runnable runnable);

    /**
     * Show an error dialog with a throwable.
     */
    void showErrorWithStacktrace(String message, String windowTitle, Throwable throwable, Runnable runnable);

    /**
     * Show an error dialog with multiple throwables.
     */
    void showErrorWithStacktrace(String message, String windowTitle, Throwable[] throwables, Runnable runnable);

    /**
     * Show an informational detail line (shortcut for addDetail).
     */
    void showInfo(String message, Object... params);

    /**
     * Show a Yes/No confirmation dialog. Blocks the calling thread and returns the result.
     * <p>
     * Implementations MUST use synchronous dispatching (syncExec/invokeAndWait)
     * to return the boolean to the caller.
     *
     * @return true if the user clicked Yes, false otherwise
     */
    boolean showYesNoDialogue(String message, String windowTitle);

    /**
     * Enable the launch button (update completed successfully).
     */
    void setCanStart();

    /**
     * Show or hide the main window.
     */
    void setVisible(boolean visible);

    /**
     * Clean up any native resources (fonts, colors, images, etc.).
     */
    void dispose();

	/**
	 * Enter the SWT event loop (blocks until shell is disposed or System#exit).
	 * This method is empty on AWT
	 */
	default void enterEventLoop()
	{

	}

	/**
	 * This is kinda dumb, we can maybe deduplicate this class. This just exists to create the impl specific worker
	 * @param repair
	 * @param clean
	 */
	void createUpdaterWorker(boolean repair, boolean clean);
}
