package org.glycoinfo.application.glycanbuilder.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.EscapeDialog;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;
import org.glycoinfo.application.glycanbuilder.dataset.SNFGTable;

/**
 * Choose a monosaccharide: to add one, or to say which one a placed "Assigned" placeholder is (#252).
 *
 * <p>The monosaccharides are in tabs. Five follow SNFG's own grouping of its Table 1 - hexoses,
 * deoxyhexoses, pentoses, nonulosonic acids, and the rest - each showing its rows of the table in SNFG's
 * colour order, drawn the way this application draws them in SNFG whatever notation the canvas is in.
 * The sixth holds every residue with no SNFG symbol, by the length of its backbone. One search box
 * searches them all, and shows what it finds as a list to choose from with the keyboard.
 */
public class ResidueDesignDialog extends EscapeDialog implements ActionListener {

	private static final long serialVersionUID = 8327943157144414497L;

	/** What {@link #getReturnStatus()} says when no residue was chosen. */
	public static final String CANCEL = "Cancel";

	/** The tab of the residues with no SNFG symbol. */
	public static final String NO_SYMBOL = "No SNFG symbol";

	/** The heading a residue is listed under when its composition class is none of these. */
	static final String OTHER = "Other";

	/** How tall the symbols are drawn. */
	private static final int SYMBOL_SIZE = 26;

	/** A symbol's cell in a tab. */
	private static final Dimension CELL = new Dimension(66, 54);

	/**
	 * The SNFG tabs: each a title, the residue whose symbol is its icon, and the rows of Table 1 in it.
	 * The icon is the white symbol of the tab's first row: white is SNFG's symbol for a class rather
	 * than for one monosaccharide, and a tab is a class.
	 */
	private static final String[][] SNFG_TABS = {
			{ "Hexoses", "Hex", "Filled Circle", "Filled Square", "Crossed Square", "Divided Diamond" },
			{ "Deoxyhexoses", "dHex", "Filled Triangle", "Divided Triangle", "Flat Rectangle" },
			{ "Pentoses", "Pen", "Filled Star" },
			{ "Nonulosonic acids", "Non", "Filled Diamond", "Flat Diamond" },
			{ "Other SNFG", "Unknown", "Flat Hexagon", "Pentagon" },
	};

	/**
	 * The heading each composition class in the file is listed under, in the order they are shown. A
	 * class not here is listed under {@link #OTHER}, so a residue added to the file is never left out.
	 */
	private static final String[][] SECTIONS = {
			{ "Triose", "Tri" },
			{ "Tetrose", "Tet" },
			{ "Pentose", "Pen", "dPen", "dPenN" },
			{ "Hexose", "Hex", "MeHex", "HexNA", "GlcNA", "GalNA", "ManNA", "IdoNA", "uHexA" },
			{ "Heptose", "Hep" },
			{ "Octose", "Oct" },
			{ "Nonose", "Non" },
			{ "Decose", "Dec" },
	};

	/** The tab last open, so the dialog opens where it was left. */
	private static int lastTab = 0;

	/** A residue on offer: its type, the label it is shown by, and where it is. */
	public static final class Choice {
		private final ResidueType type;
		private final String label;
		private final String where;
		private final String searchText;
		private final boolean symbol;

		Choice(ResidueType _type, String _label, String _where, String _searchText, boolean _symbol) {
			type = _type;
			label = _label;
			where = _where;
			searchText = _searchText.toLowerCase();
			symbol = _symbol;
		}

		public ResidueType getType() { return type; }
		public String getLabel() { return label; }
		/** The tab it is in, and for SNFG its shape and colour. */
		public String getWhere() { return where; }
		/** Whether SNFG has a symbol for it. */
		public boolean hasSymbol() { return symbol; }
	}

	private final GlycanRendererAWT renderer = snfgRenderer();
	private final Map<ResidueType, Icon> icons = new HashMap<ResidueType, Icon>();
	private final List<Choice> choices = allChoices();

	private JTextField search;
	private JPanel cards;
	private JTabbedPane tabs;
	private DefaultListModel<Choice> found;
	private JList<Choice> results;
	private JLabel resultCount;
	private JLabel status;
	private JLabel hint;

	private String return_status = CANCEL;
	private ResidueType chosen = null;

	public ResidueDesignDialog(Frame parent) {
		this(parent, false);
	}

	/**
	 * @param forPlaceholder whether the residue chosen will become a placed placeholder rather than be
	 *        added; it changes only what the dialog says
	 */
	public ResidueDesignDialog(Frame parent, boolean forPlaceholder) {
		super(parent, forPlaceholder ? "Set the placeholder's monosaccharide" : "Add a monosaccharide", true);

		initComponents(forPlaceholder);

		setResizable(true);
		setLocationRelativeTo(parent);
	}

	// --- What is on offer, apart from how it is shown

	/** The SNFG tabs, each with its rows of SNFG's Table 1, in SNFG's order. */
	public static Map<String, List<SNFGTable.Shape>> snfgTabs() {
		Map<String, List<SNFGTable.Shape>> tabs = new LinkedHashMap<String, List<SNFGTable.Shape>>();
		for (String[] tab : SNFG_TABS) {
			List<SNFGTable.Shape> shapes = new ArrayList<SNFGTable.Shape>();
			for (int i = 2; i < tab.length; i++)
				for (SNFGTable.Shape shape : SNFGTable.getShapes())
					if (shape.getName().equals(tab[i])) shapes.add(shape);
			tabs.put(tab[0], shapes);
		}
		return tabs;
	}

	/**
	 * The residues with no symbol of their own to offer, under their headings, each list sorted by
	 * name. Left out: the empty type the dictionary keeps for itself, and a name the main dictionary
	 * also has - Pse, Leg, 4eLeg - which a file reads as the residue with the SNFG symbol, and which
	 * the SNFG tabs offer.
	 */
	public static Map<String, List<ResidueType>> sections(Collection<ResidueType> types) {
		Map<String, List<ResidueType>> sections = new LinkedHashMap<String, List<ResidueType>>();
		for (String[] section : SECTIONS) sections.put(section[0], new ArrayList<ResidueType>());
		sections.put(OTHER, new ArrayList<ResidueType>());

		// the dictionary holds each type under its name and under each synonym
		Map<String, ResidueType> distinct = new TreeMap<String, ResidueType>();
		for (ResidueType type : types)
			if (!type.getName().startsWith("#") && !ResidueDictionary.hasResidueType(type.getName()))
				distinct.put(type.getName(), type);

		for (ResidueType type : distinct.values())
			sections.get(sectionOf(type.getCompositionClass())).add(type);

		for (List<ResidueType> list : sections.values())
			Collections.sort(list, new Comparator<ResidueType>() {
				public int compare(ResidueType a, ResidueType b) {
					return a.getName().compareToIgnoreCase(b.getName());
				}
			});

		sections.values().removeIf(List::isEmpty);
		return sections;
	}

	public static Map<String, List<ResidueType>> sections() {
		return sections(NonSymbolicResidueDictionary.getResidues());
	}

	static String sectionOf(String compositionClass) {
		for (String[] section : SECTIONS)
			for (int i = 1; i < section.length; i++)
				if (section[i].equals(compositionClass)) return section[0];
		return OTHER;
	}

	/** Everything on offer, in the order of the tabs. */
	public static List<Choice> allChoices() {
		List<Choice> all = new ArrayList<Choice>();
		for (Map.Entry<String, List<SNFGTable.Shape>> tab : snfgTabs().entrySet())
			for (SNFGTable.Shape shape : tab.getValue())
				for (SNFGTable.Cell cell : shape.getCells()) {
					if (cell == null || cell.getType() == null) continue;
					ResidueType type = cell.getType();
					String where = tab.getKey() + " · " + cell.getColour().getName() + " " + shape.getName();
					all.add(new Choice(type, labelOf(cell), where,
							cell.getSNFGName() + " " + type.getName() + " " + type.getDescription() + " "
									+ shape.getName() + " " + shape.getClassName() + " " + cell.getColour().getName(),
							true));
				}
		for (Map.Entry<String, List<ResidueType>> section : sections().entrySet())
			for (ResidueType type : section.getValue())
				all.add(new Choice(type, type.getName(), NO_SYMBOL + " · " + section.getKey(),
						type.getName() + " " + type.getDescription(), false));
		return all;
	}

	/**
	 * What a search finds, best first: a name that is what was typed, then one that starts with it,
	 * then one that contains it, then a description, shape or colour that does. Nothing for nothing.
	 */
	public static List<Choice> search(List<Choice> _choices, String _text) {
		final String text = _text.trim().toLowerCase();
		List<Choice> found = new ArrayList<Choice>();
		if (text.isEmpty()) return found;
		for (Choice choice : _choices)
			if (choice.searchText.contains(text)) found.add(choice);
		// a stable sort, so equals keep the order of the tabs
		Collections.sort(found, new Comparator<Choice>() {
			public int compare(Choice a, Choice b) {
				return rank(a, text) - rank(b, text);
			}
		});
		return found;
	}

	public static List<Choice> search(String _text) {
		return search(allChoices(), _text);
	}

	private static int rank(Choice choice, String text) {
		String label = choice.label.toLowerCase();
		String name = choice.type.getName().toLowerCase();
		if (label.equals(text) || name.equals(text)) return 0;
		if (label.startsWith(text) || name.startsWith(text)) return 1;
		if (label.contains(text) || name.contains(text)) return 2;
		return 3;
	}

	/** SNFG's name; for the generic white symbols this application's, as SNFG names them by their class. */
	private static String labelOf(SNFGTable.Cell cell) {
		return cell.getColour() == SNFGTable.getColours().get(0) ? cell.getTypeName() : cell.getSNFGName();
	}

	/**
	 * A renderer of its own, in SNFG, so the symbols are SNFG's whatever notation the canvas is drawn
	 * in. It loads its own styles and leaves the workspace's alone.
	 */
	static GlycanRendererAWT snfgRenderer() {
		GlycanRendererAWT renderer = new GlycanRendererAWT();
		renderer.getGraphicOptions().NOTATION = GraphicOptions.NOTATION_SNFG;
		return renderer;
	}

	/** The symbol of a residue as this application draws it in SNFG, at a size. */
	public static java.awt.Image snfgImage(ResidueType type, int size) {
		return snfgRenderer().getResidueRenderer().getImage(type, size);
	}

	/** The symbol of a residue, as this application draws it in SNFG. */
	public static Icon snfgIcon(ResidueType type) {
		return snfgRenderer().getResidueRenderer().getIcon(type, SYMBOL_SIZE);
	}

	private Icon iconOf(ResidueType type) {
		Icon icon = icons.get(type);
		if (icon == null) {
			icon = renderer.getResidueRenderer().getIcon(type, SYMBOL_SIZE);
			icons.put(type, icon);
		}
		return icon;
	}

	// --- How it is shown

	private void initComponents(boolean forPlaceholder) {
		// what choosing does, and the search
		JLabel what = new JLabel(forPlaceholder
				? "Choose what the selected placeholder is. It keeps its place, links and anything set on it."
				: "Choose a monosaccharide to add.");
		search = new JTextField(24);
		search.setToolTipText("Search every tab by name, description, shape or colour");
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { searchChanged(); }
			public void removeUpdate(DocumentEvent e) { searchChanged(); }
			public void changedUpdate(DocumentEvent e) { searchChanged(); }
		});
		bindSearchKeys();
		JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		searchRow.add(new JLabel("Search"));
		searchRow.add(search);
		hint = new JLabel("↑↓ to move, Enter to choose");
		hint.setVisible(false);
		hint.setForeground(Color.GRAY);
		hint.setFont(hint.getFont().deriveFont(11f));
		searchRow.add(hint);

		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
		top.add(what, BorderLayout.NORTH);
		top.add(searchRow, BorderLayout.CENTER);

		// the tabs, and the results that replace them while there is a search
		tabs = new JTabbedPane();
		for (Map.Entry<String, List<SNFGTable.Shape>> tab : snfgTabs().entrySet())
			tabs.addTab(tab.getKey(), iconOf(ResidueDictionary.findResidueType(tabSymbol(tab.getKey()))),
					padded(snfgTab(tab.getValue())));
		tabs.addTab(NO_SYMBOL, iconOf(ResidueDictionary.findResidueType("Assigned")), noSymbolTab());
		tabs.setSelectedIndex(Math.min(lastTab, tabs.getTabCount() - 1));
		tabs.addChangeListener(e -> lastTab = tabs.getSelectedIndex());

		cards = new JPanel(new CardLayout());
		cards.add(tabs, "tabs");
		cards.add(resultsView(), "results");

		// what the residue under the pointer is, and the way out
		status = new JLabel(" ");
		status.setForeground(Color.DARK_GRAY);
		JButton cancel = new JButton(CANCEL);
		cancel.setActionCommand(CANCEL);
		cancel.addActionListener(this);
		JPanel bottom = new JPanel(new BorderLayout(8, 0));
		bottom.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));
		bottom.add(status, BorderLayout.CENTER);
		bottom.add(cancel, BorderLayout.EAST);

		JPanel center = new JPanel(new BorderLayout());
		center.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		center.add(cards, BorderLayout.CENTER);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(top, BorderLayout.NORTH);
		getContentPane().add(center, BorderLayout.CENTER);
		getContentPane().add(bottom, BorderLayout.SOUTH);

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowOpened(WindowEvent evt) {
				search.requestFocusInWindow();
			}
			public void windowClosing(WindowEvent evt) {
				choose(null);
			}
		});

		pack();
		setMinimumSize(getSize());
	}

	/** The residue whose symbol is a tab's icon: for an SNFG tab, the white symbol of its class. */
	public static String tabSymbol(String tab) {
		for (String[] t : SNFG_TABS)
			if (t[0].equals(tab)) return t[1];
		return "Assigned";
	}

	/** An SNFG tab: its rows of Table 1, each headed by its class and shape, in SNFG's colour order. */
	private JPanel snfgTab(List<SNFGTable.Shape> shapes) {
		// the colours none of these rows uses are left out, so a tab is no wider than it needs
		List<Integer> columns = new ArrayList<Integer>();
		for (int i = 0; i < SNFGTable.getColours().size(); i++)
			for (SNFGTable.Shape shape : shapes)
				if (shape.getCells().get(i) != null && !columns.contains(i)) columns.add(i);
		Collections.sort(columns);

		JPanel table = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = -1;
		for (SNFGTable.Shape shape : shapes) {
			c.gridy++;
			c.gridx = 0;
			JLabel header = new JLabel("<html><div style='width:112px'><b>" + shape.getClassName()
					+ "</b><br><font color=gray>" + shape.getName() + "</font></div></html>");
			header.setFont(header.getFont().deriveFont(11f));
			header.setPreferredSize(new Dimension(148, CELL.height));
			table.add(header, c);

			for (int column : columns) {
				c.gridx++;
				SNFGTable.Cell cell = shape.getCells().get(column);
				if (cell == null) {
					JLabel empty = new JLabel();
					empty.setPreferredSize(CELL);
					table.add(empty, c);
				} else {
					table.add(symbolCell(cell), c);
				}
			}
		}
		return table;
	}

	private JComponent symbolCell(SNFGTable.Cell cell) {
		ResidueType type = cell.getType();
		String where = cell.getColour().getName() + " " + cell.getShape().getName();
		if (type == null) {
			JLabel absent = new JLabel(cell.getSNFGName(), SwingConstants.CENTER);
			absent.setPreferredSize(CELL);
			absent.setForeground(Color.LIGHT_GRAY);
			absent.setFont(absent.getFont().deriveFont(10f));
			absent.setToolTipText(cell.getSNFGName() + " (" + where + ") is not in this application's dictionary");
			return absent;
		}
		String description = cell.getSNFGName()
				+ (cell.getSNFGName().equals(type.getName()) ? "" : " (" + type.getName() + " here)")
				+ " — " + type.getDescription() + " · " + where;
		JButton button = cellButton(type, labelOf(cell), iconOf(type), description);
		button.setPreferredSize(CELL);
		return button;
	}

	/**
	 * The residues with no SNFG symbol: the lengths of backbone listed down the side, with a count, and
	 * the residues of the one chosen beside them.
	 */
	private JComponent noSymbolTab() {
		final CardLayout pages = new CardLayout();
		final JPanel shown = new JPanel(pages);
		DefaultListModel<String> backbones = new DefaultListModel<String>();
		final List<String> keys = new ArrayList<String>();

		for (Map.Entry<String, List<ResidueType>> section : sections().entrySet()) {
			JPanel buttons = new JPanel(new GridLayout(0, 3, 4, 2));
			for (ResidueType type : section.getValue()) {
				JButton button = cellButton(type, type.getName(), null, type.getName() + " — " + type.getDescription());
				button.setHorizontalAlignment(SwingConstants.LEFT);
				buttons.add(button);
			}
			JPanel holder = new JPanel(new BorderLayout());
			holder.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
			holder.add(buttons, BorderLayout.NORTH);
			JScrollPane scroll = new JScrollPane(holder);
			scroll.setBorder(null);
			scroll.getVerticalScrollBar().setUnitIncrement(16);
			shown.add(scroll, section.getKey());

			keys.add(section.getKey());
			backbones.addElement(section.getKey() + "  (" + section.getValue().size() + ")");
		}

		final JList<String> side = new JList<String>(backbones);
		side.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		side.setFixedCellHeight(24);
		side.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		side.addListSelectionListener(e -> {
			if (side.getSelectedIndex() >= 0) pages.show(shown, keys.get(side.getSelectedIndex()));
		});
		side.setSelectedIndex(Math.max(0, keys.indexOf("Hexose")));
		JScrollPane sideScroll = new JScrollPane(side);
		sideScroll.setPreferredSize(new Dimension(130, 10));

		JLabel note = new JLabel("SNFG draws these as a white pentagon, with a letter explained beneath the structure.");
		note.setForeground(Color.GRAY);
		note.setFont(note.getFont().deriveFont(11f));
		note.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		JPanel body = new JPanel(new BorderLayout(6, 0));
		body.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
		body.add(sideScroll, BorderLayout.WEST);
		body.add(shown, BorderLayout.CENTER);

		JPanel tab = new JPanel(new BorderLayout());
		tab.add(note, BorderLayout.NORTH);
		tab.add(body, BorderLayout.CENTER);
		return tab;
	}

	/** The results of a search: a list to choose from with the keyboard or a double click. */
	private JComponent resultsView() {
		found = new DefaultListModel<Choice>();
		results = new JList<Choice>(found);
		results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		results.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean selected, boolean focused) {
				Choice choice = (Choice) value;
				JLabel label = (JLabel) super.getListCellRendererComponent(list, "", index, selected, focused);
				String grey = selected ? "" : " color=gray";
				label.setText("<html><b>" + choice.label + "</b>"
						+ (choice.label.equals(choice.type.getName()) ? "" : " (" + choice.type.getName() + ")")
						+ " &nbsp;<font" + grey + ">" + choice.type.getDescription() + "</font><br>"
						+ "<font size=-2" + grey + ">" + choice.where + "</font></html>");
				label.setIcon(iconOf(choice.type));
				label.setIconTextGap(10);
				label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
				return label;
			}
		});
		results.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && results.getSelectedValue() != null)
					choose(results.getSelectedValue().type);
			}
		});

		resultCount = new JLabel(" ");
		resultCount.setForeground(Color.GRAY);
		resultCount.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

		JPanel view = new JPanel(new BorderLayout());
		view.add(resultCount, BorderLayout.NORTH);
		view.add(new JScrollPane(results), BorderLayout.CENTER);
		return view;
	}

	private void searchChanged() {
		List<Choice> matches = search(choices, search.getText());
		found.clear();
		for (Choice choice : matches) found.addElement(choice);
		if (!matches.isEmpty()) results.setSelectedIndex(0);

		boolean searching = !search.getText().trim().isEmpty();
		resultCount.setText(matches.isEmpty() ? "Nothing matches “" + search.getText().trim() + "”"
				: matches.size() + (matches.size() == 1 ? " match" : " matches"));
		hint.setVisible(searching && !matches.isEmpty());
		((CardLayout) cards.getLayout()).show(cards, searching ? "results" : "tabs");
	}

	/** Up and down move through the results, Enter chooses, Escape clears the search before it closes. */
	private void bindSearchKeys() {
		search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "next");
		search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "previous");
		search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "choose");
		search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
		search.getActionMap().put("next", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) { move(1); }
		});
		search.getActionMap().put("previous", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) { move(-1); }
		});
		search.getActionMap().put("choose", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				if (results.getSelectedValue() != null) choose(results.getSelectedValue().type);
			}
		});
		search.getActionMap().put("clear", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				if (search.getText().isEmpty()) choose(null);
				else search.setText("");
			}
		});
	}

	private void move(int by) {
		if (found.isEmpty()) return;
		int next = Math.max(0, Math.min(found.size() - 1, results.getSelectedIndex() + by));
		results.setSelectedIndex(next);
		results.ensureIndexIsVisible(next);
	}

	/**
	 * A flat button: the symbol and its name, outlined while the pointer or the keyboard is on it, with
	 * its description shown at the foot of the dialog.
	 */
	private JButton cellButton(final ResidueType type, String label, Icon icon, final String description) {
		final JButton button = new JButton(label, icon);
		button.setFont(button.getFont().deriveFont(icon == null ? 12f : 10f));
		if (icon != null) {
			button.setVerticalTextPosition(SwingConstants.BOTTOM);
			button.setHorizontalTextPosition(SwingConstants.CENTER);
		}
		button.setToolTipText(description);
		button.setContentAreaFilled(false);
		button.setFocusPainted(false);
		button.setOpaque(false);
		button.setRolloverEnabled(true);

		final Border rest = BorderFactory.createEmptyBorder(4, 6, 4, 6);
		final Border lit = BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(HIGHLIGHT_EDGE, 1, true), BorderFactory.createEmptyBorder(3, 5, 3, 5));
		button.setBorder(rest);
		button.getModel().addChangeListener(e -> light(button, button.getModel().isRollover() || button.hasFocus(), rest, lit, description));
		button.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent e) { light(button, true, rest, lit, description); }
			public void focusLost(java.awt.event.FocusEvent e) { light(button, button.getModel().isRollover(), rest, lit, description); }
		});
		button.addActionListener(e -> choose(type));
		return button;
	}

	private static final Color HIGHLIGHT_EDGE = new Color(66, 133, 244);
	private static final Color HIGHLIGHT_FILL = new Color(232, 240, 254);

	private void light(JButton button, boolean on, Border rest, Border lit, String description) {
		button.setBorder(on ? lit : rest);
		button.setOpaque(on);
		button.setBackground(on ? HIGHLIGHT_FILL : null);
		if (on) status.setText(description);
	}

	private static JComponent padded(JComponent content) {
		JPanel padded = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		padded.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		padded.add(content);
		return padded;
	}

	// --- The answer

	private void choose(ResidueType type) {
		chosen = type;
		return_status = (type == null) ? CANCEL : type.getName();
		setVisible(false);
		dispose();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		choose(null);
	}

	public boolean isCanceled() {
		return chosen == null;
	}

	/**
	 * The residue chosen, or null when none was. The type itself and not its name: Pse, Leg and 4eLeg
	 * are names in both dictionaries.
	 */
	public ResidueType getChosenType() {
		return chosen;
	}

	/** The name of the residue chosen, or {@link #CANCEL}. */
	public String getReturnStatus() {
		return this.return_status;
	}
}
