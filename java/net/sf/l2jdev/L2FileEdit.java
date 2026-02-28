package net.sf.l2jdev;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import net.sf.l2jdev.actions.ActionTask;
import net.sf.l2jdev.actions.MassRecryptor;
import net.sf.l2jdev.actions.MassTxtPacker;
import net.sf.l2jdev.actions.MassTxtUnpacker;
import net.sf.l2jdev.actions.OpenDat;
import net.sf.l2jdev.actions.SaveDat;
import net.sf.l2jdev.actions.SaveTxt;
import net.sf.l2jdev.clientcryptor.crypt.DatCrypter;
import net.sf.l2jdev.config.ConfigDebug;
import net.sf.l2jdev.config.ConfigWindow;
import net.sf.l2jdev.forms.JPopupTextArea;
import net.sf.l2jdev.tools.DumpDecryptedServerName;
import net.sf.l2jdev.util.Util;
import net.sf.l2jdev.xml.CryptVersionParser;
import net.sf.l2jdev.xml.DescriptorParser;

public class L2FileEdit extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOGGER = Logger.getLogger(L2FileEdit.class.getName());
	
	// EXCLUSIVO SAMURAI
	private static final String APP_TITLE = "Samurai File Editor";
	private static final String CHRONICLE_FIXED = "Samurai - Crow";
	private static final String SOURCE_ENCRYPT_TYPE_STR = "Source";
	
	// UI strings
	private static final String OPEN_STR = "Open & Decrypt";
	private static final String SAVE_TXT_STR = "Save TXT";
	private static final String SAVE_DAT_STR = "Save & Encrypt";
	private static final String UNPACK_ALL_STR = "Unpack all (folder)";
	private static final String PACK_ALL_STR = "Pack all (folder)";
	private static final String PATCH_ALL_STR = "Patch all (folder)";
	private static final String DUMP_SERVERNAME_STR = "Dump ServerName Bytes";
	private static final String ABORT_STR = "Abort";
	private static final String SELECT_STR = "Select";
	private static final String FILE_SELECT_STR = "File select";
	
	public static boolean DEV_MODE = false;
	
	private static SplashScreen _splashScreen = null;
	
	private static JTextArea _logTextArea;
	private static JTextArea _errorTextArea;
	private static JTextArea _programTextArea;
	
	private final ExecutorService _executorService = Executors.newCachedThreadPool();
	
	private final JPopupTextArea _editor;
	private final LineNumberingTextArea _lineNumbers;
	
	private final JButton _btnOpen;
	private final JButton _btnSaveTxt;
	private final JButton _btnSaveDat;
	
	private final JButton _btnUnpackAll;
	private final JButton _btnPackAll;
	private final JButton _btnPatchAll;
	private final JButton _btnDumpServerName;
	
	private final JButton _btnAbort;
	private final JProgressBar _progressBar;
	
	private final ArrayList<JPanel> _actionPanels = new ArrayList<>();
	
	private File _currentFileWindow = null;
	private ActionTask _progressTask = null;
	
	public static void main(String[] args)
	{
		// Create log folder.
		final File logFolder = new File(".", "log");
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory.
		try (InputStream is = new FileInputStream(new File("./config/log.cfg")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, null, e);
		}
		
		_splashScreen = SplashScreen.getSplashScreen();
		DEV_MODE = Util.contains((Object[]) args, (Object) "-dev");
		
		ConfigWindow.load();
		ConfigDebug.load();
		
		CryptVersionParser.getInstance().parse();
		DescriptorParser.getInstance().parse();
		
		// Dark Nimbus overrides
		applyDarkTheme();
		
		EventQueue.invokeLater(L2FileEdit::new);
	}
	
	public L2FileEdit()
	{
		setTitle(APP_TITLE + "  |  " + CHRONICLE_FIXED);
		
		setMinimumSize(new Dimension(1100, 680));
		setSize(new Dimension(ConfigWindow.WINDOW_WIDTH, ConfigWindow.WINDOW_HEIGHT));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);
		
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent evt)
			{
				ConfigWindow.save("WINDOW_HEIGHT", String.valueOf(getHeight()));
				ConfigWindow.save("WINDOW_WIDTH", String.valueOf(getWidth()));
				System.exit(0);
			}
		});
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().setBackground(Theme.BG);
		
		// ===== Header (top) =====
		final JPanel header = new JPanel(new BorderLayout());
		header.setBackground(Theme.BG2);
		header.setBorder(new EmptyBorder(10, 12, 10, 12));
		
		final JLabel title = new JLabel(APP_TITLE);
		title.setForeground(Theme.FG);
		title.setFont(Theme.FONT_TITLE);
		
		final JLabel subtitle = new JLabel("Chronicle: " + CHRONICLE_FIXED + "   •   Encrypt: " + SOURCE_ENCRYPT_TYPE_STR);
		subtitle.setForeground(Theme.FG_DIM);
		subtitle.setFont(Theme.FONT_UI);
		
		final JPanel titleBox = new JPanel();
		titleBox.setOpaque(false);
		titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
		titleBox.add(title);
		titleBox.add(Box.createVerticalStrut(2));
		titleBox.add(subtitle);
		
		header.add(titleBox, BorderLayout.WEST);
		
		// Progress + abort (top-right)
		final JPanel progressBox = new JPanel();
		progressBox.setOpaque(false);
		progressBox.setLayout(new BoxLayout(progressBox, BoxLayout.X_AXIS));
		
		_progressBar = new JProgressBar(0, 100);
		_progressBar.setPreferredSize(new Dimension(280, 20));
		_progressBar.setStringPainted(true);
		_progressBar.setValue(0);
		_progressBar.setBorder(BorderFactory.createEmptyBorder());
		_progressBar.setForeground(Theme.ACCENT);
		_progressBar.setBackground(Theme.BG3);
		
		_btnAbort = new JButton(ABORT_STR);
		styleButton(_btnAbort, Theme.DANGER);
		_btnAbort.setEnabled(false);
		_btnAbort.addActionListener(this::abortActionPerformed);
		
		progressBox.add(_progressBar);
		progressBox.add(Box.createHorizontalStrut(10));
		progressBox.add(_btnAbort);
		
		header.add(progressBox, BorderLayout.EAST);
		
		getContentPane().add(header, BorderLayout.NORTH);
		
		// ===== Sidebar (left) =====
		final JPanel sidebar = new JPanel();
		sidebar.setBackground(Theme.BG2);
		sidebar.setBorder(new EmptyBorder(12, 12, 12, 12));
		sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
		sidebar.setPreferredSize(new Dimension(260, 10));
		
		// Group: File
		sidebar.add(sectionLabel("File"));
		_btnOpen = new JButton(OPEN_STR);
		styleButton(_btnOpen, Theme.ACCENT);
		_btnOpen.addActionListener(this::openSelectFileWindow);
		sidebar.add(_btnOpen);
		
		sidebar.add(Box.createVerticalStrut(8));
		
		_btnSaveDat = new JButton(SAVE_DAT_STR);
		styleButton(_btnSaveDat, Theme.ACCENT2);
		_btnSaveDat.setEnabled(false);
		_btnSaveDat.addActionListener(this::saveDatActionPerformed);
		sidebar.add(_btnSaveDat);
		
		sidebar.add(Box.createVerticalStrut(8));
		
		_btnSaveTxt = new JButton(SAVE_TXT_STR);
		styleButton(_btnSaveTxt, Theme.ACCENT2);
		_btnSaveTxt.setEnabled(false);
		_btnSaveTxt.addActionListener(this::saveTxtActionPerformed);
		sidebar.add(_btnSaveTxt);
		
		sidebar.add(Box.createVerticalStrut(16));
		
		// Group: Tools
		sidebar.add(sectionLabel("Tools"));
		_btnUnpackAll = new JButton(UNPACK_ALL_STR);
		styleButton(_btnUnpackAll, Theme.BTN);
		_btnUnpackAll.addActionListener(this::massTxtUnpackActionPerformed);
		sidebar.add(_btnUnpackAll);
		
		sidebar.add(Box.createVerticalStrut(8));
		
		_btnPackAll = new JButton(PACK_ALL_STR);
		styleButton(_btnPackAll, Theme.BTN);
		_btnPackAll.addActionListener(this::massTxtPackActionPerformed);
		sidebar.add(_btnPackAll);
		
		sidebar.add(Box.createVerticalStrut(8));
		
		_btnPatchAll = new JButton(PATCH_ALL_STR);
		styleButton(_btnPatchAll, Theme.BTN);
		_btnPatchAll.addActionListener(this::massRecryptActionPerformed);
		sidebar.add(_btnPatchAll);
		
		sidebar.add(Box.createVerticalStrut(16));
		
		// Group: Debug
		sidebar.add(sectionLabel("Debug"));
		_btnDumpServerName = new JButton(DUMP_SERVERNAME_STR);
		styleButton(_btnDumpServerName, Theme.BTN);
		_btnDumpServerName.addActionListener(this::dumpDecryptedServerNameActionPerformed);
		sidebar.add(_btnDumpServerName);
		
		sidebar.add(Box.createVerticalGlue());
		
		final JLabel footer = new JLabel("Ready");
		footer.setForeground(Theme.FG_DIM);
		footer.setFont(Theme.FONT_UI.deriveFont(12f));
		sidebar.add(footer);
		
		// Track panels for enable/disable logic
		_actionPanels.add(sidebar);
		
		// ===== Main area (right) =====
		final JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplit.setResizeWeight(0.72);
		mainSplit.setOneTouchExpandable(true);
		mainSplit.setBorder(BorderFactory.createEmptyBorder());
		mainSplit.setBackground(Theme.BG);
		
		_editor = new JPopupTextArea();
		_editor.setFont(Theme.FONT_MONO);
		_editor.setBackground(Theme.EDITOR_BG);
		_editor.setForeground(Theme.EDITOR_FG);
		_editor.setCaretColor(Theme.EDITOR_FG);
		_editor.setSelectionColor(Theme.SELECTION);
		_editor.setSelectedTextColor(Theme.EDITOR_FG);
		_editor.setTabSize(4);
		
		((AbstractDocument) _editor.getDocument()).setDocumentFilter(new DocumentFilter()
		{
			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
			{
				final String replacedText = (text == null) ? null : text.replace("\r\n", "\n");
				super.replace(fb, offset, length, replacedText, attrs);
			}
		});
		
		_lineNumbers = new LineNumberingTextArea(_editor);
		_lineNumbers.setFont(Theme.FONT_MONO.deriveFont(12f));
		_lineNumbers.setBackground(Theme.BG3);
		_lineNumbers.setForeground(Theme.FG_DIM);
		_lineNumbers.setEditable(false);
		_lineNumbers.setBorder(new EmptyBorder(8, 8, 8, 8));
		_editor.getDocument().addDocumentListener(_lineNumbers);
		
		final JScrollPane editorScroll = new JScrollPane(_editor);
		editorScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
		editorScroll.setRowHeaderView(_lineNumbers);
		editorScroll.getViewport().setBackground(Theme.EDITOR_BG);
		
		mainSplit.setTopComponent(editorScroll);
		
		// Bottom tabs (Log / Error / Program)
		final JTabbedPane tabs = new JTabbedPane();
		tabs.setBackground(Theme.BG2);
		tabs.setForeground(Theme.FG);
		tabs.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
		
		_logTextArea = new JTextArea();
		styleLogArea(_logTextArea);
		
		_errorTextArea = new JTextArea();
		styleLogArea(_errorTextArea);
		_errorTextArea.setForeground(new Color(255, 140, 140));
		
		_programTextArea = new JTextArea();
		styleLogArea(_programTextArea);
		_programTextArea.setForeground(new Color(170, 200, 255));
		
		tabs.addTab("Log", wrapScroll(_logTextArea));
		tabs.addTab("Error", wrapScroll(_errorTextArea));
		tabs.addTab("Program", wrapScroll(_programTextArea));
		
		mainSplit.setBottomComponent(tabs);
		
		// ===== Root split (left sidebar + main) =====
		final JSplitPane rootSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		rootSplit.setResizeWeight(0.0);
		rootSplit.setOneTouchExpandable(true);
		rootSplit.setBorder(BorderFactory.createEmptyBorder());
		rootSplit.setLeftComponent(sidebar);
		rootSplit.setRightComponent(mainSplit);
		
		getContentPane().add(rootSplit, BorderLayout.CENTER);
		
		// Icons
		try
		{
			final List<Image> icons = new ArrayList<>();
			icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "16x16.png").getImage());
			icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "32x32.png").getImage());
			setIconImages(icons);
		}
		catch (Exception e)
		{
			// ignore
		}
		
		pack();
		if (_splashScreen != null)
			_splashScreen.close();
		
		setVisible(true);
		toFront();
	}
	
	// ====== Public helpers (used by actions) ======
	
	public JPopupTextArea getTextPaneMain()
	{
		return _editor;
	}
	
	public static void addLogConsole(String log, boolean isLog)
	{
		if (isLog)
			LOGGER.info(log);
		
		appendTo(_logTextArea, log);
		
		// Heurística simples: se tiver "error" joga no Error tab também.
		final String lower = (log == null) ? "" : log.toLowerCase();
		if (lower.contains("error") || lower.contains("exception") || lower.contains("failed"))
			appendTo(_errorTextArea, log);
	}
	
	private static void appendTo(JTextArea area, String line)
	{
		if (area == null)
			return;
		
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(() -> area.append(line + "\n"));
		else
			area.append(line + "\n");
	}
	
	public void setEditorText(String text)
	{
		_lineNumbers.cleanUp();
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(() -> _editor.setText(text));
		else
			_editor.setText(text);
	}
	
	// ====== Actions ======
	
	private void massTxtPackActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
			return;
		
		final JFileChooser fileopen = new JFileChooser();
		fileopen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileopen.setAcceptAllFileFilterUsed(false);
		fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY_PACK));
		fileopen.setPreferredSize(new Dimension(720, 620));
		
		final int ret = fileopen.showDialog(null, SELECT_STR);
		if (ret == JFileChooser.APPROVE_OPTION)
		{
			_currentFileWindow = fileopen.getSelectedFile();
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY_PACK", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Selected folder: " + _currentFileWindow.getPath(), true);
			
			_progressTask = new MassTxtPacker(this, CHRONICLE_FIXED, _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void massTxtUnpackActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
			return;
		
		final JFileChooser fileopen = new JFileChooser();
		fileopen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileopen.setAcceptAllFileFilterUsed(false);
		fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY_UNPACK));
		fileopen.setPreferredSize(new Dimension(720, 620));
		
		final int ret = fileopen.showDialog(null, SELECT_STR);
		if (ret == JFileChooser.APPROVE_OPTION)
		{
			_currentFileWindow = fileopen.getSelectedFile();
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY_UNPACK", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Selected folder: " + _currentFileWindow.getPath(), true);
			
			_progressTask = new MassTxtUnpacker(this, CHRONICLE_FIXED, _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void massRecryptActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
			return;
		
		final JFileChooser fileopen = new JFileChooser();
		fileopen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileopen.setAcceptAllFileFilterUsed(false);
		fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY));
		fileopen.setPreferredSize(new Dimension(720, 620));
		
		final int ret = fileopen.showDialog(null, SELECT_STR);
		if (ret == JFileChooser.APPROVE_OPTION)
		{
			_currentFileWindow = fileopen.getSelectedFile();
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Selected folder: " + _currentFileWindow.getPath(), true);
			
			_progressTask = new MassRecryptor(this, _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void openSelectFileWindow(ActionEvent evt)
	{
		if (_progressTask != null)
			return;
		
		final JFileChooser fileopen = new JFileChooser();
		fileopen.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileopen.setMultiSelectionEnabled(false);
		fileopen.setAcceptAllFileFilterUsed(false);
		
		fileopen.setFileFilter(new FileNameExtensionFilter(".dat", "dat"));
		fileopen.setFileFilter(new FileNameExtensionFilter(".ini", "ini"));
		fileopen.setFileFilter(new FileNameExtensionFilter(".txt", "txt"));
		fileopen.setFileFilter(new FileNameExtensionFilter(".htm", "htm"));
		fileopen.setFileFilter(new FileNameExtensionFilter(".dat, .ini, .txt, .htm", "dat", "ini", "txt", "htm"));
		
		fileopen.setSelectedFile(new File(ConfigWindow.LAST_FILE_SELECTED));
		fileopen.setPreferredSize(new Dimension(720, 620));
		
		final int ret = fileopen.showDialog(null, FILE_SELECT_STR);
		if (ret == JFileChooser.APPROVE_OPTION)
		{
			_currentFileWindow = fileopen.getSelectedFile();
			ConfigWindow.save("LAST_FILE_SELECTED", _currentFileWindow.getAbsolutePath());
			
			addLogConsole("---------------------------------------", true);
			addLogConsole("Open file: " + _currentFileWindow.getName(), true);
			
			_progressTask = new OpenDat(this, CHRONICLE_FIXED, _currentFileWindow);
			_executorService.execute(_progressTask);
		}
	}
	
	private void saveTxtActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
			return;
		
		final JFileChooser fileSave = new JFileChooser();
		fileSave.setCurrentDirectory(new File(ConfigWindow.FILE_SAVE_CURRENT_DIRECTORY));
		
		if (_currentFileWindow == null)
		{
			addLogConsole("No open file!", true);
			return;
		}
		
		fileSave.setSelectedFile(new File(_currentFileWindow.getName().split("\\.")[0] + ".txt"));
		fileSave.setFileFilter(new FileNameExtensionFilter(".txt", "txt"));
		fileSave.setAcceptAllFileFilterUsed(false);
		fileSave.setPreferredSize(new Dimension(720, 620));
		
		final int ret = fileSave.showSaveDialog(null);
		if (ret == JFileChooser.APPROVE_OPTION)
		{
			_progressTask = new SaveTxt(this, fileSave.getSelectedFile());
			_executorService.execute(_progressTask);
		}
	}
	
	private void saveDatActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
			return;
		
		if (_currentFileWindow == null)
		{
			addLogConsole("Error saving dat. No file name.", true);
			return;
		}
		
		_progressTask = new SaveDat(this, _currentFileWindow, CHRONICLE_FIXED);
		_executorService.execute(_progressTask);
	}
	
	private void abortActionPerformed(ActionEvent evt)
	{
		if (_progressTask == null)
			return;
		
		_progressTask.abort();
		addLogConsole("---------------------------------------", true);
		addLogConsole("Progress aborted.", true);
	}
	
	public void dumpDecryptedServerNameActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
			return;
		
		addLogConsole("---------------------------------------", true);
		addLogConsole("Dumping decrypted bytes of ServerName-eu.dat...", true);
		
		_progressTask = new ActionTask(this)
		{
			@Override
			protected void action()
			{
				try
				{
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					DumpDecryptedServerName.main(new String[0]);
					L2FileEdit.addLogConsole("DumpDecryptedServerName finished. Check ServerName-eu.dec in application folder.", true);
				}
				catch (Exception e)
				{
					L2FileEdit.addLogConsole("DumpDecryptedServerName error: " + e.getMessage(), true);
				}
				finally
				{
					SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
				}
			}
		};
		
		_executorService.execute(_progressTask);
	}
	
	// ====== Encrypt selection logic (kept; UI removed) ======
	
	public DatCrypter getEncryptor(File file)
	{
		DatCrypter crypter = null;
		final String encryptorName = ConfigWindow.CURRENT_ENCRYPT;
		
		// Sem UI: se estiver vazio ou Source, detecta do arquivo.
		if (encryptorName == null || encryptorName.trim().isEmpty() || encryptorName.equalsIgnoreCase(SOURCE_ENCRYPT_TYPE_STR) || encryptorName.equalsIgnoreCase("."))
		{
			final DatCrypter lastDatDecryptor = OpenDat.getLastDatCrypter(file);
			
			if (lastDatDecryptor != null)
			{
				crypter = CryptVersionParser.getInstance().getEncryptKey(lastDatDecryptor.getName());
				if (crypter == null)
					addLogConsole("Not found " + lastDatDecryptor.getName() + " encryptor of the file: " + _currentFileWindow.getName(), true);
			}
		}
		else
		{
			crypter = CryptVersionParser.getInstance().getEncryptKey(encryptorName);
			if (crypter == null)
				addLogConsole("Not found " + encryptorName + " encryptor of the file: " + _currentFileWindow.getName(), true);
		}
		return crypter;
	}
	
	// ====== Task lifecycle (called by ActionTask subclasses) ======
	
	public void onStartTask()
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		_progressBar.setValue(0);
		checkButtons();
	}
	
	public void onProgressTask(int val)
	{
		_progressBar.setValue(val);
	}
	
	public void onStopTask()
	{
		_progressTask = null;
		_progressBar.setValue(100);
		checkButtons();
		Toolkit.getDefaultToolkit().beep();
		setCursor(null);
	}
	
	public void onAbortTask()
	{
		if (_progressTask == null)
			return;
		
		_progressTask = null;
		setCursor(null);
		checkButtons();
	}
	
	private void checkButtons()
	{
		final boolean busy = (_progressTask != null);
		
		_actionPanels.forEach(p -> {
			for (Component c : p.getComponents())
				c.setEnabled(!busy);
		});
		
		// Save buttons depend on open file when not busy
		if (!busy)
		{
			_btnSaveTxt.setEnabled(_currentFileWindow != null);
			_btnSaveDat.setEnabled(_currentFileWindow != null);
		}
		else
		{
			_btnSaveTxt.setEnabled(false);
			_btnSaveDat.setEnabled(false);
		}
		
		_btnAbort.setEnabled(busy);
	}
	
	// ====== UI helpers ======
	
	private static void styleButton(JButton b, Color accent)
	{
		b.setFocusPainted(false);
		b.setFont(Theme.FONT_UI);
		b.setForeground(Theme.FG);
		b.setBackground(accent);
		b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(8, 10, 8, 10)));
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
	}
	
	private static JLabel sectionLabel(String text)
	{
		final JLabel l = new JLabel(text.toUpperCase());
		l.setForeground(Theme.FG_DIM);
		l.setFont(Theme.FONT_UI.deriveFont(Font.BOLD, 12f));
		l.setBorder(new EmptyBorder(0, 0, 8, 0));
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}
	
	private static JScrollPane wrapScroll(JTextArea a)
	{
		final JScrollPane sp = new JScrollPane(a);
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp.getViewport().setBackground(Theme.EDITOR_BG);
		return sp;
	}
	
	private static void styleLogArea(JTextArea a)
	{
		a.setEditable(false);
		a.setLineWrap(true);
		a.setWrapStyleWord(true);
		a.setFont(Theme.FONT_MONO.deriveFont(12f));
		a.setBackground(Theme.EDITOR_BG);
		a.setForeground(new Color(130, 220, 255));
		a.setCaretColor(Theme.EDITOR_FG);
		a.setBorder(new EmptyBorder(10, 10, 10, 10));
	}
	
	private static void applyDarkTheme()
	{
		try
		{
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
			{
				if ("Nimbus".equals(info.getName()))
				{
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
		
		// Nimbus overrides (dark)
		UIManager.put("control", Theme.BG2);
		UIManager.put("info", Theme.BG2);
		UIManager.put("nimbusBase", Theme.BG3);
		UIManager.put("nimbusBlueGrey", Theme.BG3);
		UIManager.put("nimbusLightBackground", Theme.BG);
		UIManager.put("text", Theme.FG);
		UIManager.put("Menu.foreground", Theme.FG);
		UIManager.put("MenuItem.foreground", Theme.FG);
		UIManager.put("Label.foreground", Theme.FG);
		UIManager.put("OptionPane.background", Theme.BG2);
		UIManager.put("Panel.background", Theme.BG2);
	}
	
	private static final class Theme
	{
		static final Color BG = new Color(18, 18, 20);
		static final Color BG2 = new Color(24, 24, 28);
		static final Color BG3 = new Color(34, 34, 40);
		
		static final Color BORDER = new Color(60, 60, 70);
		
		static final Color FG = new Color(230, 230, 235);
		static final Color FG_DIM = new Color(160, 160, 170);
		
		static final Color EDITOR_BG = new Color(16, 16, 18);
		static final Color EDITOR_FG = new Color(220, 220, 225);
		static final Color SELECTION = new Color(60, 80, 120);
		
		static final Color ACCENT = new Color(70, 110, 200);
		static final Color ACCENT2 = new Color(55, 85, 160);
		static final Color BTN = new Color(42, 42, 50);
		static final Color DANGER = new Color(160, 60, 60);
		
		static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
		static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
		static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);
	}
	
	// ====== Line numbers ======
	
	private static class LineNumberingTextArea extends JTextArea implements DocumentListener
	{
		private static final long serialVersionUID = 1L;
		
		private final JTextArea textArea;
		private int lastLines;
		
		public LineNumberingTextArea(JTextArea area)
		{
			this.textArea = area;
			this.lastLines = 0;
			setText("1\n");
		}
		
		public void cleanUp()
		{
			setText("");
			lastLines = 0;
		}
		
		private void updateText()
		{
			final int lines = textArea.getLineCount();
			if (lines == lastLines)
				return;
			
			lastLines = lines;
			
			final StringBuilder sb = new StringBuilder(lines * 3);
			for (int i = 1; i <= lines; i++)
				sb.append(i).append('\n');
			
			setText(sb.toString());
		}
		
		@Override
		public void insertUpdate(DocumentEvent e)
		{
			updateText();
		}
		
		@Override
		public void removeUpdate(DocumentEvent e)
		{
			updateText();
		}
		
		@Override
		public void changedUpdate(DocumentEvent e)
		{
			updateText();
		}
	}
	
}