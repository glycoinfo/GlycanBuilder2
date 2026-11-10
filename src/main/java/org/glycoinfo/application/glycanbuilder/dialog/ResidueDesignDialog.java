package org.glycoinfo.application.glycanbuilder.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.util.EscapeDialog;
import org.glycoinfo.application.glycanbuilder.dataset.NonSymbolicResidueDictionary;

/**
 * The monosaccharides with no symbol of their own, to add one or to say which one a placed "Assigned"
 * placeholder is (#252).
 *
 * <p>Every residue in {@code non_symbolic_residue_types} is offered, grouped by the length of its
 * backbone. They used to be 36 buttons written out by hand, which left out the rest of the file -
 * araHex, the heptoses, the nonuloses - and this dialog was not on any menu.
 */
public class ResidueDesignDialog extends EscapeDialog implements ActionListener {

	private static final long serialVersionUID = 8327943157144414497L;

	/** What {@link #getReturnStatus()} says when no residue was chosen. */
	public static final String CANCEL = "Cancel";

	/** The heading a residue is listed under when its composition class is none of these. */
	static final String OTHER = "Other";

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

	private final Map<String, List<JButton>> buttonsBySection = new LinkedHashMap<String, List<JButton>>();
	private final Map<String, JPanel> panelsBySection = new LinkedHashMap<String, JPanel>();
	private final Map<String, Component> blocksBySection = new LinkedHashMap<String, Component>();
	private JTextField filter;
	private String return_status = CANCEL;

	public ResidueDesignDialog(Frame parent) {
		super(parent, "Non-symbolic monosaccharides", true);

		initComponents();

		setResizable(true);
		setLocationRelativeTo(parent);
	}

	/**
	 * The residues to offer, under their headings, each list sorted by name. The empty type the
	 * dictionary keeps for itself is left out.
	 */
	public static Map<String, List<ResidueType>> sections(Collection<ResidueType> types) {
		Map<String, List<ResidueType>> sections = new LinkedHashMap<String, List<ResidueType>>();
		for (String[] section : SECTIONS) sections.put(section[0], new ArrayList<ResidueType>());
		sections.put(OTHER, new ArrayList<ResidueType>());

		// the dictionary holds each type under its name and under each synonym
		Map<String, ResidueType> distinct = new TreeMap<String, ResidueType>();
		for (ResidueType type : types)
			if (!type.getName().startsWith("#")) distinct.put(type.getName(), type);

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

	private void initComponents() {
		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		for (Map.Entry<String, List<ResidueType>> section : sections().entrySet()) {
			JLabel heading = new JLabel(section.getKey());
			heading.setFont(heading.getFont().deriveFont(Font.BOLD));
			heading.setAlignmentX(Component.LEFT_ALIGNMENT);

			JPanel buttons = new JPanel(new GridLayout(0, 4, 4, 4));
			buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
			List<JButton> sectionButtons = new ArrayList<JButton>();
			for (ResidueType type : section.getValue()) {
				JButton button = new JButton(type.getName());
				button.setActionCommand(type.getName());
				button.setToolTipText(type.getDescription());
				button.addActionListener(this);
				buttons.add(button);
				sectionButtons.add(button);
			}

			// no taller than its buttons need, whatever height the list has to spare
			JPanel block = new JPanel(new BorderLayout(0, 2)) {
				private static final long serialVersionUID = 1L;
				@Override
				public Dimension getMaximumSize() {
					return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
				}
			};
			block.setAlignmentX(Component.LEFT_ALIGNMENT);
			block.add(heading, BorderLayout.NORTH);
			block.add(buttons, BorderLayout.CENTER);
			// the space below a section is its own, so it goes when a filter hides the section
			block.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
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
				+ scroll.getVerticalScrollBar().getPreferredSize().width + 4, 460));

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
				closeDialog();
			}
		});

		pack();
	}

	private void applyFilter() {
		String text = filter.getText().trim().toLowerCase();
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
		return CANCEL.equals(return_status);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		return_status = e.getActionCommand();
		closeDialog();
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
