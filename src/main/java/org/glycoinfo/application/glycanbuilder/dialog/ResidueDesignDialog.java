package org.glycoinfo.application.glycanbuilder.dialog;

import java.awt.BorderLayout;
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
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
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
 * The monosaccharides, to add one or to say which one a placed "Assigned" placeholder is (#252).
 *
 * <p>Two parts. The first is SNFG's own Table 1, its twelve shapes by ten colours, each symbol drawn
 * the way this application draws it in SNFG whatever notation the canvas is in. The second is every
 * residue in {@code non_symbolic_residue_types} that has no symbol of its own, grouped by the length of
 * its backbone; SNFG draws each of those as a white pentagon with a letter, so they are listed by name.
 *
 * <p>The non-symbolic list used to be 36 buttons written out by hand, and this dialog was on no menu.
 */
public class ResidueDesignDialog extends EscapeDialog implements ActionListener {

	private static final long serialVersionUID = 8327943157144414497L;

	/** What {@link #getReturnStatus()} says when no residue was chosen. */
	public static final String CANCEL = "Cancel";

	/** The heading of the SNFG table. */
	static final String SNFG = "SNFG";

	/** The heading a residue is listed under when its composition class is none of these. */
	static final String OTHER = "Other";

	/** How tall the symbols in the table are drawn. */
	private static final int SYMBOL_SIZE = 26;

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

	private final Map<JButton, ResidueType> typeOf = new IdentityHashMap<JButton, ResidueType>();
	private final Map<JButton, String> searchTextOf = new IdentityHashMap<JButton, String>();
	private final List<JButton> snfgButtons = new ArrayList<JButton>();
	private Component snfgBlock;
	private final Map<String, List<JButton>> buttonsBySection = new LinkedHashMap<String, List<JButton>>();
	private final Map<String, JPanel> panelsBySection = new LinkedHashMap<String, JPanel>();
	private final Map<String, Component> blocksBySection = new LinkedHashMap<String, Component>();
	private JTextField filter;
	private String return_status = CANCEL;
	private ResidueType chosen = null;

	public ResidueDesignDialog(Frame parent) {
		super(parent, "Monosaccharides", true);

		initComponents();

		setResizable(true);
		setLocationRelativeTo(parent);
	}

	/**
	 * The residues with no symbol of their own to offer, under their headings, each list sorted by
	 * name. Left out: the empty type the dictionary keeps for itself, and a name the main dictionary
	 * also has - Pse, Leg, 4eLeg - which a file reads as the residue with the SNFG symbol, and which
	 * the SNFG table offers.
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

	/**
	 * A renderer of its own, in SNFG, so the table shows SNFG's symbols whatever notation the canvas
	 * is drawn in. It loads its own styles and leaves the workspace's alone.
	 */
	static GlycanRendererAWT snfgRenderer() {
		GlycanRendererAWT renderer = new GlycanRendererAWT();
		renderer.getGraphicOptions().NOTATION = GraphicOptions.NOTATION_SNFG;
		return renderer;
	}

	/** The symbol of a residue, as this application draws it in SNFG. */
	public static Icon snfgIcon(ResidueType type) {
		return snfgRenderer().getResidueRenderer().getIcon(type, SYMBOL_SIZE);
	}

	private void initComponents() {
		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		snfgBlock = section(SNFG + " (Table 1 of the Symbol Nomenclature for Glycans)", snfgTable());
		list.add(snfgBlock);

		JLabel note = new JLabel("No symbol of their own: drawn as a white pentagon with a letter");
		note.setFont(note.getFont().deriveFont(Font.ITALIC));
		note.setAlignmentX(Component.LEFT_ALIGNMENT);
		note.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
		list.add(note);

		for (Map.Entry<String, List<ResidueType>> section : sections().entrySet()) {
			JPanel buttons = new JPanel(new GridLayout(0, 4, 4, 4));
			List<JButton> sectionButtons = new ArrayList<JButton>();
			for (ResidueType type : section.getValue()) {
				JButton button = button(type, type.getName(), type.getDescription());
				buttons.add(button);
				sectionButtons.add(button);
			}
			Component block = section(section.getKey(), buttons);
			list.add(block);

			buttonsBySection.put(section.getKey(), sectionButtons);
			panelsBySection.put(section.getKey(), buttons);
			blocksBySection.put(section.getKey(), block);
		}

		JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		// as wide as the list, so it scrolls one way only
		scroll.setPreferredSize(new Dimension(list.getPreferredSize().width
				+ scroll.getVerticalScrollBar().getPreferredSize().width + 4, 560));

		filter = new JTextField(20);
		filter.setToolTipText("Show only the residues whose name or description contains this");
		filter.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { applyFilter(); }
			public void removeUpdate(DocumentEvent e) { applyFilter(); }
			public void changedUpdate(DocumentEvent e) { applyFilter(); }
		});
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("Filter"));
		top.add(filter);

		JButton cancel = new JButton(CANCEL);
		cancel.setActionCommand(CANCEL);
		cancel.addActionListener(this);
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottom.add(cancel);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(top, BorderLayout.NORTH);
		getContentPane().add(scroll, BorderLayout.CENTER);
		getContentPane().add(bottom, BorderLayout.SOUTH);

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				return_status = CANCEL;
				chosen = null;
				closeDialog();
			}
		});

		pack();
	}

	/** SNFG's Table 1: a row for each shape, a column for each colour, as SNFG lays it out. */
	private JPanel snfgTable() {
		GlycanRendererAWT renderer = snfgRenderer();
		Font small = new JLabel().getFont().deriveFont(10f);

		JPanel table = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(1, 1, 1, 1);
		c.fill = GridBagConstraints.BOTH;

		c.gridy = 0;
		c.gridx = 0;
		table.add(new JLabel(""), c);
		for (SNFGTable.Colour colour : SNFGTable.getColours()) {
			c.gridx++;
			JLabel header = new JLabel(colour.getName(), SwingConstants.CENTER);
			header.setFont(small.deriveFont(Font.BOLD));
			table.add(header, c);
		}

		for (SNFGTable.Shape shape : SNFGTable.getShapes()) {
			c.gridy++;
			c.gridx = 0;
			JLabel header = new JLabel("<html><b>" + shape.getName() + "</b><br>" + shape.getClassName() + "</html>");
			header.setFont(small);
			header.setPreferredSize(new Dimension(124, header.getPreferredSize().height));
			table.add(header, c);

			for (SNFGTable.Cell cell : shape.getCells()) {
				c.gridx++;
				if (cell == null) {
					table.add(new JLabel(""), c);
					continue;
				}
				ResidueType type = cell.getType();
				// SNFG's name; the generic white symbols by this application's, as SNFG names them by
				// their class, which the row already gives
				String label = cell.getColour() == SNFGTable.getColours().get(0) ? cell.getTypeName() : cell.getSNFGName();
				JButton button;
				if (type == null) {
					button = new JButton(cell.getSNFGName());
					button.setEnabled(false);
					button.setToolTipText(cell.getSNFGName() + " (" + cell.getColour().getName() + " "
							+ shape.getName() + ") is not in this application's dictionary");
				} else {
					String tooltip = "<html><b>" + cell.getSNFGName() + "</b>"
							+ (cell.getSNFGName().equals(type.getName()) ? "" : " - " + type.getName() + " here")
							+ "<br>" + type.getDescription() + "<br>" + cell.getColour().getName() + " "
							+ shape.getName() + "</html>";
					button = button(type, label, tooltip);
					button.setIcon(renderer.getResidueRenderer().getIcon(type, SYMBOL_SIZE));
					searchTextOf.put(button, (cell.getSNFGName() + " " + type.getName() + " " + type.getDescription()
							+ " " + shape.getName() + " " + shape.getClassName() + " " + cell.getColour().getName()).toLowerCase());
				}
				button.setText(label);
				button.setFont(small);
				button.setVerticalTextPosition(SwingConstants.BOTTOM);
				button.setHorizontalTextPosition(SwingConstants.CENTER);
				button.setMargin(new Insets(2, 2, 2, 2));
				button.setPreferredSize(new Dimension(80, 52));
				snfgButtons.add(button);
				table.add(button, c);
			}
		}
		return table;
	}

	private JButton button(ResidueType type, String label, String tooltip) {
		JButton button = new JButton(label);
		button.setActionCommand(type.getName());
		button.setToolTipText(tooltip);
		button.addActionListener(this);
		typeOf.put(button, type);
		return button;
	}

	/** A heading and what is under it, no taller than it needs, whatever height the list has to spare. */
	private static Component section(String title, JPanel content) {
		JLabel heading = new JLabel(title);
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));

		JPanel block = new JPanel(new BorderLayout(0, 2)) {
			private static final long serialVersionUID = 1L;
			@Override
			public Dimension getMaximumSize() {
				return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
			}
		};
		block.setAlignmentX(Component.LEFT_ALIGNMENT);
		// the space below a section is its own, so it goes when a filter hides the section
		block.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		block.add(heading, BorderLayout.NORTH);
		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		left.add(content);
		block.add(left, BorderLayout.CENTER);
		return block;
	}

	private void applyFilter() {
		String text = filter.getText().trim().toLowerCase();

		// The table keeps its shape - it is SNFG's layout that makes it readable - so a symbol that
		// does not match is greyed rather than taken out.
		boolean anySymbol = false;
		for (JButton button : snfgButtons) {
			if (!typeOf.containsKey(button)) continue;
			boolean shown = text.isEmpty() || searchTextOf.get(button).contains(text);
			button.setEnabled(shown);
			anySymbol |= shown;
		}
		snfgBlock.setVisible(anySymbol);

		for (Map.Entry<String, List<JButton>> section : buttonsBySection.entrySet()) {
			// rebuilt rather than hidden: a grid keeps the cell of a hidden button
			JPanel panel = panelsBySection.get(section.getKey());
			panel.removeAll();
			for (JButton button : section.getValue()) {
				String description = button.getToolTipText() == null ? "" : button.getToolTipText();
				if (text.isEmpty() || button.getText().toLowerCase().contains(text)
						|| description.toLowerCase().contains(text))
					panel.add(button);
			}
			blocksBySection.get(section.getKey()).setVisible(panel.getComponentCount() > 0);
		}
		getContentPane().validate();
		getContentPane().repaint();
	}

	public boolean isCanceled() {
		return chosen == null;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		chosen = (source instanceof JButton) ? typeOf.get(source) : null;
		return_status = (chosen == null) ? CANCEL : chosen.getName();
		closeDialog();
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

	private void closeDialog() {
		setVisible(false);
		dispose();
	}
}
