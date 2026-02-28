package net.sf.l2jdev.forms;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.undo.UndoManager;

public class JPopupTextArea extends JTextArea
{
	private static final long serialVersionUID = 1L;

	private static final String COPY = "Copy (Ctrl+C)";
	private static final String CUT = "Cut (Ctrl+X)";
	private static final String PASTE = "Paste (Ctrl+V)";
	private static final String DELETE = "Delete";
	private static final String SELECT_ALL = "Select all (Ctrl+A)";
	private static final String GOTO = "Go to line (Ctrl+G)";
	private static final String FIND = "Find (Ctrl+F)";
	private static final String FIND_NEXT = "Find next (F3)";

	protected final UndoManager _manager;

	// Find state
	private String _lastQuery = null;
	private int _lastIndex = 0;

	// Highlight
	private final Highlighter.HighlightPainter _painter =
		new DefaultHighlighter.DefaultHighlightPainter(new Color(60, 80, 120));

	public JPopupTextArea()
	{
		_manager = new UndoManager();
		getDocument().addUndoableEditListener(_manager);

		setFont(new Font("Consolas", Font.PLAIN, 13));

		addPopupMenu();
		bindKeys();
		installCaretCleaner();
	}

	public void discardAllEdits()
	{
		_manager.discardAllEdits();
	}

	public void cleanUp()
	{
		setText("");
		removeAll();
		discardAllEdits();
		clearHighlights();
		_lastQuery = null;
		_lastIndex = 0;
	}

	private void bindKeys()
	{
		final Action undo = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_manager.canUndo())
					_manager.undo();
			}
		};

		final Action redo = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (_manager.canRedo())
					_manager.redo();
			}
		};

		final InputMap imap = getInputMap();
		imap.put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
		imap.put(KeyStroke.getKeyStroke("ctrl Y"), "redo");
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "findNext");
		imap.put(KeyStroke.getKeyStroke("ctrl F"), "find");
		imap.put(KeyStroke.getKeyStroke("ctrl G"), "goto");

		final ActionMap amap = getActionMap();
		amap.put("undo", undo);
		amap.put("redo", redo);
		amap.put("find", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				searchDialog();
			}
		});
		amap.put("goto", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				goToLine();
			}
		});
		amap.put("findNext", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				findNext();
			}
		});
	}

	private void addPopupMenu()
	{
		final JPopupMenu menu = new JPopupMenu();
		stylePopup(menu);

		menu.add(styledItem(getActionMap().get("copy-to-clipboard"), COPY));
		menu.add(styledItem(getActionMap().get("cut-to-clipboard"), CUT));
		menu.add(styledItem(getActionMap().get("paste-from-clipboard"), PASTE));

		final JMenuItem deleteItem = new JMenuItem();
		deleteItem.setAction(getActionMap().get("delete-previous"));
		deleteItem.setText(DELETE);
		styleMenuItem(deleteItem);
		menu.add(deleteItem);

		menu.add(new JSeparator());

		menu.add(styledItem(getActionMap().get("select-all"), SELECT_ALL));

		final JMenuItem gotoItem = new JMenuItem(GOTO);
		styleMenuItem(gotoItem);
		gotoItem.addActionListener(_ -> goToLine());
		menu.add(gotoItem);

		final JMenuItem findItem = new JMenuItem(FIND);
		styleMenuItem(findItem);
		findItem.addActionListener(_ -> searchDialog());
		menu.add(findItem);

		final JMenuItem nextItem = new JMenuItem(FIND_NEXT);
		styleMenuItem(nextItem);
		nextItem.addActionListener(_ -> findNext());
		menu.add(nextItem);

		add(menu);

		addMouseListener(new PopupTriggerMouseListener(menu, this));
	}

	private static JMenuItem styledItem(Action action, String text)
	{
		final JMenuItem item = new JMenuItem();
		item.setAction(action);
		item.setText(text);
		styleMenuItem(item);
		return item;
	}

	protected void goToLine()
	{
		final String input = JOptionPane.showInputDialog(new Frame(), "Line number:", "Go to line", JOptionPane.QUESTION_MESSAGE);
		if (input == null)
			return;

		final int line;
		try
		{
			line = Integer.parseInt(input.trim());
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(new Frame(), "Enter a valid line number", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (line <= 0)
		{
			JOptionPane.showMessageDialog(new Frame(), "Enter a valid line number", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try
		{
			final int offset = getLineStartOffset(line - 1);
			requestFocusInWindow();
			setCaretPosition(offset);
			scrollCaretIntoView();
		}
		catch (BadLocationException e)
		{
			JOptionPane.showMessageDialog(new Frame(), "Line number does not exist", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void searchDialog()
	{
		final String q = JOptionPane.showInputDialog(new Frame(), "Search string:", "Find", JOptionPane.QUESTION_MESSAGE);
		if (q == null)
			return;

		final String query = q.trim();
		if (query.isEmpty())
		{
			JOptionPane.showMessageDialog(new Frame(), "Enter a non-empty string", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		_lastQuery = query;
		_lastIndex = Math.max(getSelectionEnd(), 0);
		findNext();
	}

	private void findNext()
	{
		if (_lastQuery == null || _lastQuery.isEmpty())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		clearHighlights();

		final String text = getText();
		if (text == null || text.isEmpty())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int idx = text.indexOf(_lastQuery, _lastIndex);
		if (idx < 0)
		{
			// wrap
			idx = text.indexOf(_lastQuery, 0);
			if (idx < 0)
			{
				Toolkit.getDefaultToolkit().beep();
				return;
			}
		}

		final int start = idx;
		final int end = idx + _lastQuery.length();

		try
		{
			getHighlighter().addHighlight(start, end, _painter);
		}
		catch (BadLocationException e)
		{
			// ignore
		}

		requestFocusInWindow();
		setCaretPosition(start);
		moveCaretPosition(end);
		getCaret().setSelectionVisible(true);

		_lastIndex = end;
		scrollCaretIntoView();
	}

	@SuppressWarnings("deprecation")
	private void scrollCaretIntoView()
	{
		try
		{
			final Point p = modelToView(getCaretPosition()).getLocation();
			scrollRectToVisible(getVisibleRect().union(new java.awt.Rectangle(p.x, p.y, 1, getFontMetrics(getFont()).getHeight())));
		}
		catch (Exception e)
		{
			// ignore (view may be null momentarily)
		}
	}

	private void clearHighlights()
	{
		final Highlighter h = getHighlighter();
		if (h != null)
			h.removeAllHighlights();
	}

	private void installCaretCleaner()
	{
		addCaretListener(new CaretListener()
		{
			@Override
			public void caretUpdate(CaretEvent e)
			{
				// Se o usuário move o caret manualmente, não apaga automaticamente;
				// mantém highlight até nova busca.
			}
		});
	}

	private static class PopupTriggerMouseListener extends MouseAdapter
	{
		private final JPopupMenu popup;
		private final JComponent component;

		public PopupTriggerMouseListener(JPopupMenu popup, JComponent component)
		{
			this.popup = popup;
			this.component = component;
		}

		private void showMenuIfPopupTrigger(MouseEvent e)
		{
			if (e.isPopupTrigger())
				popup.show(component, e.getX() + 3, e.getY() + 3);
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			showMenuIfPopupTrigger(e);
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			showMenuIfPopupTrigger(e);
		}
	}

	// ===== Popup theme =====

	private static void stylePopup(JPopupMenu menu)
	{
		menu.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));
		menu.setBackground(new Color(24, 24, 28));
	}

	private static void styleMenuItem(JMenuItem item)
	{
		item.setOpaque(true);
		item.setBackground(new Color(24, 24, 28));
		item.setForeground(new Color(230, 230, 235));
		item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		item.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

		// Hover effect
		item.addChangeListener(_ ->
		{
			if (!item.isEnabled())
				return;

			if (item.getModel().isArmed() || item.getModel().isSelected())
				item.setBackground(new Color(42, 42, 50));
			else
				item.setBackground(new Color(24, 24, 28));
		});
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		// Garante contraste visual quando disabled
		if (!enabled)
		{
			setForeground(new Color(140, 140, 150));
		}
	}
}