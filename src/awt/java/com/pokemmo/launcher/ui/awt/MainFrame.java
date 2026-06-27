package com.pokemmo.launcher.ui.awt;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pokemmo.launcher.Launcher;
import com.pokemmo.launcher.config.Config;
import com.pokemmo.launcher.enums.PokeMMOLocale;
import com.pokemmo.launcher.enums.UpdateChannel;
import com.pokemmo.launcher.ui.LauncherUI;
import com.pokemmo.launcher.ui.shared.UiBridge;
import com.pokemmo.launcher.util.Util;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

/**
 * @author Kyu
 */
public class MainFrame extends JFrame implements ActionListener, LauncherUI
{
	private static final Font FONT_MONOSPACED = new Font(Font.MONOSPACED, Font.PLAIN, 14);

	private final Launcher parent;

	protected final LocaleAwareLabel status;
	protected final JLabel dlSpeed;

	protected LocaleAwareButton launchGame;
	protected LocaleAwareButton configLauncher;

	private final JProgressBar progressBar;
	private final LocaleAwareTextArea taskOutput;

	private final ExecutorService executorService;

	private static MainFrame instance;

	private final JDialog configWindow;

	public static MainFrame getInstance()
	{
		return instance;
	}

	public MainFrame(Launcher parent)
	{
		instance = this;

		this.parent = parent;
		this.executorService = Executors.newFixedThreadPool(1);

		UIManager.getLookAndFeelDefaults().put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(java.awt.event.WindowEvent e)
			{
				parent.cancelUpdater();
				dispose();
				System.exit(0);
			}
		});

		status = new LocaleAwareLabel("main.loading");
		dlSpeed = new JLabel("");

		/**
		 * Progress Bar initialization
		 */
		{
			progressBar = new JProgressBar(0, 100);
			progressBar.setValue(0);
			progressBar.setStringPainted(false);
			progressBar.setIndeterminate(true);
		}

		/**
		 * TaskOutput text area
		 */
		{
			taskOutput = new LocaleAwareTextArea(5, 20);
			taskOutput.setMargin(new Insets(5, 5, 5, 5));
			taskOutput.setEditable(false);
			taskOutput.setFont(FONT_MONOSPACED);

			DefaultCaret caret = (DefaultCaret) taskOutput.getCaret();
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		}

		/**
		 * Top Bar (status + dlSpeed row, progress bar row below)
		 */
		JPanel topContainer = new JPanel(new BorderLayout());
		{
			JPanel statusRow = new JPanel(new BorderLayout());
			statusRow.add(status, BorderLayout.WEST);
			statusRow.add(dlSpeed, BorderLayout.EAST);
			topContainer.add(statusRow, BorderLayout.NORTH);
			topContainer.add(progressBar, BorderLayout.SOUTH);
		}

		/**
		 * Configuration popup window
		 */
		configWindow = new JDialog(this, Config.getString("config.title.window"), true);
		{
			JPanel config_panel = new JPanel(new GridLayout(10, 2));
			config_panel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
			{
				LocaleAwareLabel localeLabel = new LocaleAwareLabel("config.title.language");
				JComboBox<PokeMMOLocale> localeList = new JComboBox<>(PokeMMOLocale.ENABLED_LANGUAGES);
				localeList.setSelectedItem(Config.ACTIVE_LOCALE);
				localeList.addActionListener((event) -> Config.changeLocale((PokeMMOLocale) localeList.getSelectedItem()));

				if(localeList.getModel().getSize() < 2)
				{
					localeList.setEnabled(false);
				}

				config_panel.add(localeLabel);
				config_panel.add(localeList);

				LocaleAwareLabel networkThreadsLabel = new LocaleAwareLabel("config.title.dl_threads");
				SpinnerNumberModel networkThreadsModel = new SpinnerNumberModel(Config.NETWORK_THREADS, 1, Config.NETWORK_THREADS_MAX, 1);
				JSpinner networkThreadsSpinner = new JSpinner(networkThreadsModel);
				networkThreadsSpinner.addChangeListener((event) -> {
					Config.NETWORK_THREADS = networkThreadsModel.getNumber().intValue();
					Config.save();
				});

				config_panel.add(networkThreadsLabel);
				config_panel.add(networkThreadsSpinner);

				LocaleAwareLabel updateChannelLabel = new LocaleAwareLabel("config.title.update_channel");
				JComboBox<UpdateChannel> updateChannelList = new JComboBox<>(UpdateChannel.values());
				updateChannelList.setSelectedItem(Config.UPDATE_CHANNEL);
				updateChannelList.addActionListener((event) -> {
					Config.UPDATE_CHANNEL = (UpdateChannel) updateChannelList.getSelectedItem();
					parent.doUpdate(false);
					Config.save();
				});

				updateChannelList.setEnabled(UpdateChannel.ENABLED_UPDATE_CHANNELS.length > 1);

				config_panel.add(updateChannelLabel);
				config_panel.add(updateChannelList);

				config_panel.add(new LocaleAwareLabel("config.title.advanced"));
				config_panel.add(new JLabel("")); // Dummy widget to fulfill our column requirements

				LocaleAwareLabel memoryMaxLabel = new LocaleAwareLabel("config.mem.max");
				SpinnerNumberModel memoryMaxModel = new SpinnerNumberModel(Config.HARD_MAX_MEMORY_MB, Config.JOPTS_XMX_VAL_MIN, Config.JOPTS_XMX_VAL_MAX, 128);
				JSpinner memoryMaxSpinner = new JSpinner(memoryMaxModel);

				memoryMaxSpinner.addChangeListener((event) ->
				{
					Config.HARD_MAX_MEMORY_MB = memoryMaxModel.getNumber().shortValue();
					Config.save();
				});

				config_panel.add(memoryMaxLabel);
				config_panel.add(memoryMaxSpinner);

				LocaleAwareButton openClientFolder = new LocaleAwareButton("config.title.open_client_folder");
				openClientFolder.addActionListener((event) -> Util.open(parent.getPokemmoDir()));

				config_panel.add(openClientFolder);
				config_panel.add(new JLabel("")); // Dummy widget to fulfill our column requirements

				LocaleAwareButton repairClientFolder = new LocaleAwareButton("config.title.repair_client");
				repairClientFolder.addActionListener((event) ->
				{
					if(showYesNoDialogue(Config.getString("status.game_repair_prompt"), Config.getString("config.title.repair_client")))
					{
						configWindow.setVisible(false);
						new UpdaterSwingWorker(parent, MainFrame.this, true).execute();
					}
				});

				config_panel.add(repairClientFolder);
				config_panel.add(new JLabel("")); // Dummy widget to fulfill our column requirements
			}

			configWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			configWindow.add(config_panel);
			configWindow.setTitle(Config.getString("config.title.window"));
			configWindow.setSize(500, 340);
			configWindow.setResizable(false);

			configWindow.addWindowListener(new WindowListener()
			{
				@Override
				public void windowOpened(WindowEvent e) { }
				@Override
				public void windowClosing(WindowEvent e) { }
				@Override
				public void windowClosed(WindowEvent e) { }
				@Override
				public void windowIconified(WindowEvent e) { }
				@Override
				public void windowDeiconified(WindowEvent e) { }

				@Override
				public void windowActivated(WindowEvent e)
				{
					Point p = MainFrame.this.getLocationOnScreen();
					configWindow.setLocation(p.x + ((MainFrame.this.getWidth())/2) - 200, p.y + (MainFrame.this.getHeight()/2 - 125));
				}

				@Override
				public void windowDeactivated(WindowEvent e)
				{
					Config.save();
				}
			});
		}

		/**
		 * Bottom Bar
		 */
		JPanel bottom_panel = new JPanel(new BorderLayout(0, 0));
		{
			configLauncher = new LocaleAwareButton("config.title.window");
			configLauncher.addActionListener((event) -> configWindow.setVisible(true));

			launchGame = new LocaleAwareButton("main.launch");
			launchGame.setEnabled(false);

			if(!Launcher.UPDATER_MODE)
				bottom_panel.add(configLauncher, BorderLayout.WEST);
			bottom_panel.add(launchGame, BorderLayout.EAST);
		}

		/**
		 * Add our widgets
		 */
		{
			add(topContainer, BorderLayout.PAGE_START);
			add(new JScrollPane(taskOutput), BorderLayout.CENTER);
			add(bottom_panel, BorderLayout.PAGE_END);
		}

		pack();
		setSize(600, 400);
		setLocationRelativeTo(null);
		if(Launcher.flatpak != null || Launcher.snapcraft != null)
			setTitle(Config.getString("main.title"));
		else
			setTitle(Config.getString("updater.title"));
		setResizable(false);

		try
		{
			List<Image> icons = AwtIconLoader.getImages();
			setIconImages(icons);
		}
		catch (Exception e)
		{
			System.err.println("Failed to load application icon: " + e.getMessage());
		}

		setVisible(!Launcher.QUICK_AUTOSTART);

		// Register this MainFrame as the UI bridge error callback
		UiBridge.setErrorCallback(this::showError);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if("exit".equals(e.getActionCommand()))
		{
			System.exit(Launcher.EXIT_CODE_SUCCESS);
		}
	}

	@Override
	public void setStatus(final String string, int progress, Object... params)
	{
		if(!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(() -> status.setTextKey(string));
		else
			status.setTextKey(string);

		addDetail(string, progress, params);
	}

	@Override
	public void addDetail(final String string, final int progress, Object... params)
	{
		if(!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(() -> addDetailPrivate(string, progress, params));
		else
			addDetailPrivate(string, progress, params);
	}

	protected void addDetailPrivate(String string, int progress, Object... params)
	{
		if(progress > 0)
		{
			progressBar.setIndeterminate(false);
			progressBar.setValue(progress);
		}
		else
		{
			progressBar.setIndeterminate(true);
			progressBar.setValue(progress);
		}

		if(string != null)
		{
			taskOutput.appendLocaleStr(string, params);
			taskOutput.appendLocaleStr("\n");
		}

		validate();
	}

	@Override
	public void updateProgress(int progress)
	{
		if(!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> updateProgress(progress));
			return;
		}

		if(progress > 0)
		{
			progressBar.setIndeterminate(false);
			progressBar.setValue(progress);
		}
		else
		{
			progressBar.setIndeterminate(true);
			progressBar.setValue(progress);
		}
	}

	@Override
	public void updateDLSpeed(long bytes_per_second)
	{
		if(!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> updateDLSpeed(bytes_per_second));
			return;
		}

		if(bytes_per_second < 0)
		{
			dlSpeed.setText("");
			return;
		}

		dlSpeed.setText(Util.humanReadableByteCount(bytes_per_second, false) + "/s");
	}

	@Override
	public void showMessage(String message, String window_title)
	{
		showMessage(message, window_title, null);
	}

	@Override
	public void showMessage(String message, String window_title, Runnable runnable)
	{
		showMessage(message, window_title, JOptionPane.INFORMATION_MESSAGE, runnable);
	}

	@Override
	public void showError(String message, String window_title)
	{
		showError(message, window_title, null);
	}

	@Override
	public void showError(String message, String window_title, Runnable runnable)
	{
		showMessage(message, window_title, JOptionPane.ERROR_MESSAGE, runnable);
	}

	@Override
	public void showErrorWithStacktrace(String message, String window_title, String stacktrace, Runnable runnable)
	{
		showMessageWithTextArea(message, window_title, stacktrace, JOptionPane.ERROR_MESSAGE, runnable);
	}

	@Override
	public void showErrorWithStacktrace(String message, String window_title, Throwable throwable, Runnable runnable)
	{
		showMessageWithTextArea(message, window_title, parent.getStacktraceString(throwable), JOptionPane.ERROR_MESSAGE, runnable);
	}

	@Override
	public void showErrorWithStacktrace(String message, String window_title, Throwable[] throwables, Runnable runnable)
	{
		showMessageWithTextArea(message, window_title, parent.getStacktraceString(throwables), JOptionPane.ERROR_MESSAGE, runnable);
	}

	@Override
	public void showInfo(String message, Object... params)
	{
		addDetail(message, 90, params);
	}

	@Override
	public boolean showYesNoDialogue(String message, String window_title)
	{
		return JOptionPane.showConfirmDialog(this, message, window_title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}

	public void showMessage(String message, String window_title, int information_code, Runnable runnable)
	{
		JOptionPane.showMessageDialog(this, message, window_title, information_code);
		if(runnable != null)
			executorService.execute(runnable);
	}

	public void showMessageWithTextArea(String message, String window_title, String textAreaContents, int information_code, Runnable runnable)
	{
		JPanel jp = new JPanel();
		jp.setLayout(new BorderLayout(0, 20));

		JTextArea jta = new JTextArea(textAreaContents);
		JScrollPane scroll = new JScrollPane(jta);

		scroll.setPreferredSize(new Dimension(500, 200));
		JLabel msg = new JLabel(message);
		msg.setHorizontalAlignment(JLabel.LEFT);

		jp.add(msg, BorderLayout.PAGE_START);
		jp.add(scroll, BorderLayout.CENTER);

		JOptionPane.showMessageDialog(this, jp, window_title, information_code);

		if(runnable != null)
			executorService.execute(runnable);
	}

	@Override
	public void setCanStart()
	{
		launchGame.setEnabled(true);

		if(Launcher.QUICK_AUTOSTART)
		{
			parent.launchGame();
		}
		else
		{
			launchGame.addActionListener((event) -> parent.launchGame());
		}
	}

	@Override
	public void dispose()
	{
		executorService.shutdown();
		if (configWindow != null)
		{
			configWindow.dispose();
		}
		super.dispose();
	}

	@Override
	public void createUpdaterWorker(boolean repair)
	{
		new UpdaterSwingWorker(parent, this, repair).execute();
	}
}
