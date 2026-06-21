package com.pokemmo.launcher.ui.swt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pokemmo.launcher.Launcher;
import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.enums.PokeMMOLocale;
import com.pokemmo.launcher.enums.UpdateChannel;
import com.pokemmo.launcher.ui.LauncherUI;
import com.pokemmo.launcher.ui.shared.UiBridge;
import com.pokemmo.launcher.updater.UpdaterSwtWorker;
import com.pokemmo.launcher.util.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * SWT main window implementing {@link LauncherUI}.
 * <p>
 * All {@code LauncherUI} methods are thread-safe. UI thread dispatching
 * is handled internally via {@link Display#asyncExec(Runnable)} and
 * {@link Display#syncExec(Runnable)}.
 *
 * @author Kyu
 */
public class MainShell implements LauncherUI
{
    private final Shell shell;
    private final Launcher parent;
    private final Display display;

    // --- UI widgets ---
    private LocaleAwareLabel statusLabel;
    private LocaleAwareTextArea detailArea;
    private Label dlSpeedLabel;
    private ProgressBar progressBar;
    private LocaleAwareButton configButton;
    private LocaleAwareButton launchButton;

    // --- Config dialog ---
    private Shell configShell;
    private Combo localeCombo;
    private Combo updateChannelCombo;
    private Spinner networkThreadsSpinner;
    private Spinner memoryMaxSpinner;

    // --- Resource tracking ---
    private Font monospacedFont;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    /**
     * Constructor. Builds the UI but does not open the shell.
     */
    public MainShell(Launcher parent, Display display)
    {
        this.parent = parent;
        this.display = display;

        // --- Shell ---
        shell = new Shell(display, SWT.SHELL_TRIM);
        shell.setText(Config.getString("main.title"));
        shell.setSize(480, 280);
        centerShell(shell);

        GridLayout shellLayout = new GridLayout(1, false);
        shellLayout.marginWidth = 0;
        shellLayout.marginHeight = 0;
        shellLayout.verticalSpacing = 0;
        shell.setLayout(shellLayout);

        // --- Top Composite: status, dlSpeed (row 1) ---
        Composite topComposite = new Composite(shell, SWT.NONE);
        topComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout topLayout = new GridLayout(2, false);
        topLayout.marginWidth = 5;
        topLayout.marginHeight = 5;
        topComposite.setLayout(topLayout);

        statusLabel = new LocaleAwareLabel(topComposite, SWT.NONE, "main.loading");
        statusLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));

        dlSpeedLabel = new Label(topComposite, SWT.RIGHT);
        GridData dlGridData = new GridData(SWT.END, SWT.CENTER, false, false);
        dlGridData.widthHint = 90;
        dlGridData.minimumWidth = 60;
        dlSpeedLabel.setLayoutData(dlGridData);
        dlSpeedLabel.setText("");

        // --- Progress Composite: progress bar (row 2) ---
        Composite progressComposite = new Composite(shell, SWT.NONE);
        progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout progressLayout = new GridLayout(1, false);
        progressLayout.marginWidth = 5;
        progressLayout.marginHeight = 0;
        progressComposite.setLayout(progressLayout);

        progressBar = new ProgressBar(progressComposite, SWT.SMOOTH);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setState(SWT.INDETERMINATE);

        // --- Center Composite: scrolled detail area ---
        Composite centerComposite = new Composite(shell, SWT.NONE);
        centerComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        centerComposite.setLayout(new GridLayout(1, false));

        detailArea = new LocaleAwareTextArea(centerComposite, SWT.BORDER);
        detailArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        monospacedFont = SwtUtil.createMonospacedFont(display, 14);
        detailArea.setFont(monospacedFont);

        // --- Bottom Composite: config + launch buttons ---
        Composite bottomComposite = new Composite(shell, SWT.NONE);
        bottomComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout bottomLayout = new GridLayout(2, false);
        bottomLayout.marginWidth = 5;
        bottomLayout.marginHeight = 5;
        bottomComposite.setLayout(bottomLayout);

        configButton = new LocaleAwareButton(bottomComposite, SWT.PUSH, "config.title.window");
        configButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
        configButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                openConfigDialog();
            }
        });

        launchButton = new LocaleAwareButton(bottomComposite, SWT.PUSH, "main.launch");
        launchButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        launchButton.setEnabled(false);
        launchButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                parent.launchGame();
            }
        });

        if (Launcher.HIDE_CONFIG)
        {
            configButton.setVisible(false);
        }

        // Cancel background downloads on close
        shell.addShellListener(new ShellAdapter()
        {
            @Override
            public void shellClosed(ShellEvent e)
            {
                parent.cancelUpdater();
                dispose();
            }
        });

        // Register this MainShell as the UI bridge error callback
        UiBridge.setErrorCallback(this::showError);
    }

    // ========= LauncherUI implementation =========

    @Override
    public void setStatus(final String key, int progress, Object... params)
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(() -> setStatus(key, progress, params));
            return;
        }
        statusLabel.setTextKey(key, params);
        statusLabel.getParent().layout();
        addDetail(key, progress, params);
    }

    @Override
    public void addDetail(final String string, final int progress, Object... params)
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(() -> addDetail(string, progress, params));
            return;
        }

        if (progress > 0)
        {
            progressBar.setState(SWT.NORMAL);
            progressBar.setSelection(progress);
        }
        else
        {
            progressBar.setState(SWT.INDETERMINATE);
            progressBar.setSelection(0);
        }

        if (string != null)
        {
            detailArea.appendLocaleStr(string, params);
            detailArea.appendLocaleStr("\n");
        }
    }

    @Override
    public void updateProgress(int progress)
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(() -> updateProgress(progress));
            return;
        }

        if (progress > 0)
        {
            progressBar.setState(SWT.NORMAL);
            progressBar.setSelection(progress);
        }
        else
        {
            progressBar.setState(SWT.INDETERMINATE);
            progressBar.setSelection(0);
        }
    }

    @Override
    public void updateDLSpeed(long bytes_per_second)
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(() -> updateDLSpeed(bytes_per_second));
            return;
        }

        if (bytes_per_second < 0)
        {
            dlSpeedLabel.setText("");
            return;
        }

        dlSpeedLabel.setText(Util.humanReadableByteCount(bytes_per_second, false) + "/s");
    }

    @Override
    public void showMessage(String message, String windowTitle)
    {
        showMessage(message, windowTitle, null);
    }

    @Override
    public void showMessage(final String message, final String windowTitle, final Runnable runnable)
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(() -> showMessage(message, windowTitle, runnable));
            return;
        }

        MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
        box.setText(windowTitle != null ? windowTitle : "");
        box.setMessage(message);
        box.open();

        if (runnable != null)
        {
            executorService.execute(runnable);
        }
    }

    @Override
    public void showError(String message, String windowTitle)
    {
        showError(message, windowTitle, null);
    }

    @Override
    public void showError(final String message, final String windowTitle, final Runnable runnable)
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(() -> showError(message, windowTitle, runnable));
            return;
        }

        MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
        box.setText(windowTitle != null ? windowTitle : "");
        box.setMessage(message);
        box.open();

        if (runnable != null)
        {
            executorService.execute(runnable);
        }
    }

    @Override
    public void showErrorWithStacktrace(final String message, final String windowTitle,
                                        final String stacktrace, final Runnable runnable)
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(() -> showErrorWithStacktrace(message, windowTitle, stacktrace, runnable));
            return;
        }

        Shell dialog = new Shell(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
        dialog.setText(windowTitle != null ? windowTitle : "");
        dialog.setLayout(new GridLayout(1, false));

        Label msgLabel = new Label(dialog, SWT.WRAP);
        msgLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        msgLabel.setText(message);

        Text stackText = new Text(dialog, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
        GridData stackData = new GridData(SWT.FILL, SWT.FILL, true, true);
        stackData.widthHint = 500;
        stackData.heightHint = 200;
        stackText.setLayoutData(stackData);
        stackText.setText(stacktrace != null ? stacktrace : "");
        stackText.setEditable(false);

        Button okButton = new Button(dialog, SWT.PUSH);
        okButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        okButton.setText("OK");
        okButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                dialog.close();
                if (runnable != null)
                {
                    executorService.execute(runnable);
                }
            }
        });

        dialog.setSize(550, 300);
        centerShell(dialog);
        dialog.open();
    }

    @Override
    public void showErrorWithStacktrace(final String message, final String windowTitle,
                                        final Throwable throwable, final Runnable runnable)
    {
        String stacktrace = parent.getStacktraceString(throwable);
        showErrorWithStacktrace(message, windowTitle, stacktrace, runnable);
    }

    @Override
    public void showErrorWithStacktrace(final String message, final String windowTitle,
                                        final Throwable[] throwables, final Runnable runnable)
    {
        String stacktrace = parent.getStacktraceString(throwables);
        showErrorWithStacktrace(message, windowTitle, stacktrace, runnable);
    }

    @Override
    public void showInfo(final String message, Object... params)
    {
        if (display.getThread() != Thread.currentThread())
        {
            Object[] captured = params;
            display.asyncExec(() -> showInfo(message, captured));
            return;
        }

        addDetail(message, 90, params);
    }

    @Override
    public boolean showYesNoDialogue(final String message, final String windowTitle)
    {
        // Must use syncExec to return the boolean to the calling thread
        if (display.getThread() != Thread.currentThread())
        {
            final boolean[] result = new boolean[1];
            display.syncExec(() ->
            {
                MessageBox box = new MessageBox(shell,
                        SWT.YES | SWT.NO | SWT.ICON_QUESTION);
                box.setText(windowTitle != null ? windowTitle : "");
                box.setMessage(message);
                result[0] = (box.open() == SWT.YES);
            });
            return result[0];
        }

        MessageBox box = new MessageBox(shell,
                SWT.YES | SWT.NO | SWT.ICON_QUESTION);
        box.setText(windowTitle != null ? windowTitle : "");
        box.setMessage(message);
        return box.open() == SWT.YES;
    }

    @Override
    public void setCanStart()
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(this::setCanStart);
            return;
        }

        launchButton.setEnabled(true);
        launchButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                parent.launchGame();
            }
        });

        if (Launcher.QUICK_AUTOSTART)
        {
            parent.launchGame();
        }
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (display.getThread() != Thread.currentThread())
        {
            display.asyncExec(() -> setVisible(visible));
            return;
        }
        shell.setVisible(visible);
    }

    @Override
    public void dispose()
    {
        if (monospacedFont != null && !monospacedFont.isDisposed())
        {
            monospacedFont.dispose();
            monospacedFont = null;
        }
        executorService.shutdown();
        if (configShell != null && !configShell.isDisposed())
        {
            configShell.dispose();
        }
        if (shell != null && !shell.isDisposed())
        {
            shell.dispose();
        }
    }

    // ========= Public API for shell management =========

    public void open()
    {
        shell.open();
    }

    public boolean isDisposed()
    {
        return shell == null || shell.isDisposed();
    }

    public Shell getShell()
    {
        return shell;
    }

    // ========= Config dialog =========

    private void openConfigDialog()
    {
        if (configShell != null && !configShell.isDisposed())
        {
            configShell.setVisible(true);
            return;
        }

        configShell = new Shell(shell, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
        configShell.setText(Config.getString("config.title.window"));
        configShell.setSize(500, 340);
        centerOverParent(configShell);

        GridLayout configLayout = new GridLayout(2, false);
        configLayout.marginWidth = 15;
        configLayout.marginHeight = 5;
        configShell.setLayout(configLayout);

        // --- Locale ---
        LocaleAwareLabel localeLabel = new LocaleAwareLabel(configShell, SWT.NONE, "config.title.language");
        localeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        localeCombo = new Combo(configShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        localeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        String[] localeItems = new String[PokeMMOLocale.ENABLED_LANGUAGES.length];
        for (int i = 0; i < PokeMMOLocale.ENABLED_LANGUAGES.length; i++)
        {
            localeItems[i] = PokeMMOLocale.ENABLED_LANGUAGES[i].getDisplayName();
        }
        localeCombo.setItems(localeItems);
        int selectedLocaleIndex = -1;
        for (int i = 0; i < PokeMMOLocale.ENABLED_LANGUAGES.length; i++)
        {
            if (PokeMMOLocale.ENABLED_LANGUAGES[i] == Config.ACTIVE_LOCALE)
            {
                selectedLocaleIndex = i;
                break;
            }
        }
        if (selectedLocaleIndex >= 0)
        {
            localeCombo.select(selectedLocaleIndex);
        }
        localeCombo.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                int idx = localeCombo.getSelectionIndex();
                if (idx >= 0 && idx < PokeMMOLocale.ENABLED_LANGUAGES.length)
                {
                    Config.changeLocale(PokeMMOLocale.ENABLED_LANGUAGES[idx]);
                }
            }
        });

        if (PokeMMOLocale.ENABLED_LANGUAGES.length < 2)
        {
            localeCombo.setEnabled(false);
        }

        // --- Network threads ---
        LocaleAwareLabel networkThreadsLabel = new LocaleAwareLabel(configShell, SWT.NONE, "config.title.dl_threads");
        networkThreadsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        networkThreadsSpinner = new Spinner(configShell, SWT.BORDER | SWT.SINGLE);
        networkThreadsSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        networkThreadsSpinner.setMinimum(1);
        networkThreadsSpinner.setMaximum(Config.NETWORK_THREADS_MAX);
        networkThreadsSpinner.setSelection(Config.NETWORK_THREADS);
        networkThreadsSpinner.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Config.NETWORK_THREADS = networkThreadsSpinner.getSelection();
                Config.save();
            }
        });

        // --- Update channel ---
        LocaleAwareLabel updateChannelLabel = new LocaleAwareLabel(configShell, SWT.NONE, "config.title.update_channel");
        updateChannelLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        updateChannelCombo = new Combo(configShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        updateChannelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        String[] channelItems = new String[UpdateChannel.ENABLED_UPDATE_CHANNELS.length];
        for (int i = 0; i < UpdateChannel.ENABLED_UPDATE_CHANNELS.length; i++)
        {
            channelItems[i] = UpdateChannel.ENABLED_UPDATE_CHANNELS[i].name();
        }
        updateChannelCombo.setItems(channelItems);
        int selectedChannelIndex = -1;
        for (int i = 0; i < UpdateChannel.ENABLED_UPDATE_CHANNELS.length; i++)
        {
            if (UpdateChannel.ENABLED_UPDATE_CHANNELS[i] == Config.UPDATE_CHANNEL)
            {
                selectedChannelIndex = i;
                break;
            }
        }
        if (selectedChannelIndex >= 0)
        {
            updateChannelCombo.select(selectedChannelIndex);
        }
        updateChannelCombo.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                int idx = updateChannelCombo.getSelectionIndex();
                if (idx >= 0 && idx < UpdateChannel.ENABLED_UPDATE_CHANNELS.length)
                {
                    Config.UPDATE_CHANNEL = UpdateChannel.ENABLED_UPDATE_CHANNELS[idx];
                    parent.doUpdate(false);
                    Config.save();
                }
            }
        });

        if (UpdateChannel.ENABLED_UPDATE_CHANNELS.length <= 1)
        {
            updateChannelCombo.setEnabled(false);
        }

        // --- Advanced label ---
        Label advancedLabel = new Label(configShell, SWT.NONE);
        advancedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        advancedLabel.setText(Config.getString("config.title.advanced"));

        Label dummy1 = new Label(configShell, SWT.NONE);
        dummy1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // --- Memory max ---
        LocaleAwareLabel memoryMaxLabel = new LocaleAwareLabel(configShell, SWT.NONE, "config.mem.max");
        memoryMaxLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        memoryMaxSpinner = new Spinner(configShell, SWT.BORDER | SWT.SINGLE);
        memoryMaxSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        memoryMaxSpinner.setMinimum(Config.JOPTS_XMX_VAL_MIN);
        memoryMaxSpinner.setMaximum(Config.JOPTS_XMX_VAL_MAX);
        memoryMaxSpinner.setIncrement(128);
        memoryMaxSpinner.setSelection(Config.HARD_MAX_MEMORY_MB);
        memoryMaxSpinner.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Config.HARD_MAX_MEMORY_MB = (short) memoryMaxSpinner.getSelection();
                Config.save();
            }
        });

        // --- Open client folder ---
        LocaleAwareButton openClientFolder = new LocaleAwareButton(configShell, SWT.PUSH, "config.title.open_client_folder");
        openClientFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        openClientFolder.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Util.open(parent.getPokemmoDir());
            }
        });

        Label dummy2 = new Label(configShell, SWT.NONE);
        dummy2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // --- Repair client ---
        LocaleAwareButton repairClientFolder = new LocaleAwareButton(configShell, SWT.PUSH, "config.title.repair_client");
        repairClientFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        repairClientFolder.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (showYesNoDialogue(Config.getString("status.game_repair_prompt"),
                        Config.getString("config.title.repair_client")))
                {
                    configShell.setVisible(false);
                    new UpdaterSwtWorker(parent, MainShell.this, true, true).execute();
                }
            }
        });

        Label dummy3 = new Label(configShell, SWT.NONE);
        dummy3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // --- Shell listeners ---
        configShell.addShellListener(new ShellAdapter()
        {
            @Override
            public void shellActivated(ShellEvent e)
            {
                centerOverParent(configShell);
            }

            @Override
            public void shellDeactivated(ShellEvent e)
            {
                if (!configShell.isDisposed())
                {
                    Config.save();
                }
            }
        });

        configShell.setDefaultButton(openClientFolder);
        // Shell non-resizable by construction (DIALOG_TRIM may include resize; this is acceptable)
        configShell.open();
    }

    // ========= Helpers =========

    private void centerShell(Shell target)
    {
        Rectangle monitorBounds = display.getPrimaryMonitor().getBounds();
        Rectangle shellBounds = target.getBounds();
        int x = monitorBounds.x + (monitorBounds.width - shellBounds.width) / 2;
        int y = monitorBounds.y + (monitorBounds.height - shellBounds.height) / 2;
        target.setLocation(x, y);
    }

    private void centerOverParent(Shell target)
    {
        Rectangle parentBounds = shell.getBounds();
        Rectangle targetBounds = target.getBounds();
        int x = parentBounds.x + (parentBounds.width - targetBounds.width) / 2;
        int y = parentBounds.y + (parentBounds.height - targetBounds.height) / 2;
        target.setLocation(x, y);
    }
}
