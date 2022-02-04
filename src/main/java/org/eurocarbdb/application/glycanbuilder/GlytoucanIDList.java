package org.eurocarbdb.application.glycanbuilder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

/**
 * Glycan Structure ID or Hash Key list
 * @author GIC 20211220
 */
public class GlytoucanIDList {

	/** Read CSV and Show GlycanIdList */

	private static File file;
	private static final String accessionIdByHashKeyBetaApi = "https://sparqlist.beta.glycosmos.org/sparqlist/trace/gtc_select_acc_by_hashkey?hash=";
	private static final String accessionIdByHashKeyRealApi = "https://sparqlist.glycosmos.org/sparqlist/trace/hash2gtcids?hash=";
	
	/**
	 * Show GlycanIdList  
	 */
	public static void showGlycanIdList(Frame pFrame) {
		JDialog frame = new JDialog(pFrame, "GlyTouCanID List", true);
		JButton idButton = new JButton("GetID");
		JButton downloadButton = new JButton("Download");
		frame.getContentPane().removeAll();
		idButton.setBounds(60, 120, 120, 50);
		downloadButton.setBounds(60, 120, 120, 50);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setOpaque(true);
		buttonPanel.add(idButton);
		buttonPanel.add(downloadButton);
		buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		DefaultTableModel model = readCSV();
		if (model.getRowCount() == 0) {
			idButton.setEnabled(false);
			downloadButton.setEnabled(false);
		}
		JPanel container = new JPanel();
		container.repaint();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.add(glycanlistTable());
		container.add(Box.createRigidArea(new Dimension(20, 20)));
		container.add(buttonPanel);
		downloadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Choose save file path");
				fileChooser.setFileFilter(new FileNameExtensionFilter("CSV file", ".csv"));
				String description = fileChooser.getFileFilter().getDescription();
				int option = fileChooser.showSaveDialog(frame);
				file = fileChooser.getSelectedFile();

				if (option == JFileChooser.APPROVE_OPTION) {
					exportCSV(description);
					fileChooser.setVisible(false);

				}

			}
		});
		idButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					getGlytoucanID();
				} catch (IOException | CsvException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (Throwable e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		frame.getContentPane().add(container);
		frame.setPreferredSize(new Dimension(800, 390));
		
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setResizable(true);
		frame.setLocationRelativeTo(pFrame);// align center
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	/**
	 * parsing a CSV file  
	 */
	public static DefaultTableModel readCSV() {
		List<String> idlist = new ArrayList<>();
		List<String> sequencelist = new ArrayList<>();
		List<String> betaOrReal = new ArrayList<>();

		String column[] = { "#","ID (or) HashKey", "Sequence", "Environment" };
		try {
			CSVReader reader = GlycanStructureAndChangeUser.readCSVFile();
			try {

				List<String[]> csvBody = reader.readAll();
				for (int i = csvBody.size() - 1; i >= 0; i--) {
					String[] strArray = csvBody.get(i);
					idlist.add(strArray[0]);
					sequencelist.add(strArray[1]);
					betaOrReal.add(strArray[2]);
				}
				reader.close();

			} catch (CsvException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Object obj[][] = new Object[idlist.size()][4];
			for (int i = 0; i < idlist.size(); i++) {
				obj[i][0] = i+1;
				obj[i][1] = idlist.get(i);
				obj[i][2] = sequencelist.get(i).replaceAll("\\:", "\\,");
				obj[i][3] = betaOrReal.get(i).toString();
			}

			model = new DefaultTableModel(obj, column) {
				public boolean isCellEditable(int row, int column) {
					return false;// This causes all cells to be not editable
				}

			};

		} catch (IOException e) {
			e.printStackTrace();
		}
		return model;

	}

	/** total line count from csv */
	public static long countLineCSV(String fileName) {
		Path path = Paths.get(fileName);
		long lines = 0;
		try {
			lines = Files.lines(path).count();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;

	}

	/** download glycanid list from table */
	public static void exportCSV(String description) {
		String filepath;
		if (description.equals("CSV file")) {
			filepath = file.toString() + ".csv";
		} else {
			filepath = file.toString();
		}
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		String[] glycanList = new String[4];
		ArrayList<String[]> list = new ArrayList<>();
		for (int i = 0; model.getRowCount() > i; i++) {
			for (int j = 0; j < model.getColumnCount(); j++) {
				glycanList[j] = model.getValueAt(i, j).toString();

			}
			list.add(glycanList);
			glycanList = new String[4];

		}
		try (CSVWriter writer = new CSVWriter(new FileWriter(filepath))) {
			String[] header = { "NO", "GlyToucanID", "Sequence", "Environment" };
			writer.writeNext(header);
			writer.writeAll(list);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}


	/** table pagination */
	private final static int itemsPerPage = 10;
	private static int maxPageIndex;
	private static int currentPageIndex = 1;
	private static DefaultTableModel model = readCSV();
	private static TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(readCSV());
	private static JTable table = new JTable();
	private final static JLabel label = new JLabel();
	private final static JButton firstbtn = new JButton(new AbstractAction("|<") {
		@Override
		public void actionPerformed(ActionEvent e) {
			currentPageIndex = 1;
			paginateFilterAndButton();
		}
	});
	private final static JButton prevbtn = new JButton(new AbstractAction("<") {
		@Override
		public void actionPerformed(ActionEvent e) {
			currentPageIndex -= 1;
			paginateFilterAndButton();
		}
	});
	private final static JButton nextbtn = new JButton(new AbstractAction(">") {
		@Override
		public void actionPerformed(ActionEvent e) {
			currentPageIndex += 1;
			paginateFilterAndButton();
		}
	});
	private final static JButton lastbtn = new JButton(new AbstractAction(">|") {
		@Override
		public void actionPerformed(ActionEvent e) {
			currentPageIndex = maxPageIndex;
			paginateFilterAndButton();
		}
	});

	/** Create Glycanlist Table */
	public static JComponent glycanlistTable() {

		firstbtn.setPreferredSize(new Dimension(50, 20));
		prevbtn.setPreferredSize(new Dimension(50, 20));
		nextbtn.setPreferredSize(new Dimension(50, 20));
		lastbtn.setPreferredSize(new Dimension(50, 20));

		table.repaint();
		table.setFillsViewportHeight(true);
		model.fireTableDataChanged();
		model = readCSV();

		JPanel labelPanel = new JPanel();
		labelPanel.add(label);
		labelPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));

		JPanel paginationPanel = new JPanel(new GridLayout(1, 4, 3, 2));
		for (JComponent r : Arrays.asList(firstbtn, prevbtn, labelPanel, nextbtn, lastbtn)) {
			paginationPanel.add(r);
		}
		paginationPanel.setLayout(new BoxLayout(paginationPanel, BoxLayout.X_AXIS));

		JPanel btnpanel = new JPanel(new BorderLayout());
		btnpanel.add(paginationPanel, BorderLayout.EAST);

		int rowCount = model.getRowCount();

		if (rowCount == 0) {
			firstbtn.disable();
			prevbtn.disable();
			nextbtn.disable();
			lastbtn.disable();
			label.disable();

		}
		int v = rowCount % itemsPerPage == 0 ? 0 : 1;
		maxPageIndex = rowCount / itemsPerPage + v;
		model.fireTableDataChanged();
		model.fireTableRowsUpdated(0, table.getRowCount());
		table = new JTable(model);
		JTableHeader tableHeader = table.getTableHeader();
		Font headerFont = new Font("Verdana", Font.BOLD, 12);
		tableHeader.setFont(headerFont);
		tableHeader.setReorderingAllowed(false);
		table.updateUI();
		sorter = new TableRowSorter<TableModel>(model);
		table.setRowSorter(sorter);
		sorter.setSortable(0, false);
		sorter.setSortable(1, false);
		sorter.setSortable(2, false);
		paginateFilterAndButton();
		table.getColumnModel().getColumn(0).setMaxWidth(35);
		table.getColumnModel().getColumn(1).setPreferredWidth(200);
		table.getColumnModel().getColumn(2).setPreferredWidth(250);
		table.getColumnModel().getColumn(3).setPreferredWidth(20);
		table.setRowHeight(25);
		// Disable table's cell selection.
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setCellSelectionEnabled(false);
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.add(new JScrollPane(table));
		mainPanel.add(btnpanel, BorderLayout.SOUTH);
		return mainPanel;
	}

	/** Paging Button Enable Disable */
	private static void paginateFilterAndButton() {

		sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
			@Override
			public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
				int ti = currentPageIndex - 1;
				int ei = entry.getIdentifier();
				return ti * itemsPerPage <= ei && ei < ti * itemsPerPage + itemsPerPage;
			}
		});

		firstbtn.setEnabled(currentPageIndex > 1);
		prevbtn.setEnabled(currentPageIndex > 1);
		nextbtn.setEnabled(currentPageIndex < maxPageIndex);
		lastbtn.setEnabled(currentPageIndex < maxPageIndex);
		label.setText(Integer.toString(currentPageIndex));
	}

	/** end table pagination */

	/** get accession id from hash key */
	public static void getGlytoucanID() throws Exception {
		File glycanIDDirectory = GlycanStructureAndChangeUser.getConfigurationDirectory();
		File glytoucanIDFilepath = new File(glycanIDDirectory.getPath() + File.separator + "glytoucanIdList.csv");
		CSVReader reader = GlycanStructureAndChangeUser.readCSVFile();
		List<String[]> csvBody = reader.readAll(); // replace accessionid of hash key

		File idlistBackupFile = null;
		String glytoucanId = null;
		ArrayList<String> testOrRealArr = new ArrayList<>();
		ArrayList<String> hashKeyArr = new ArrayList<>();
		ArrayList<String> glycanArr = new ArrayList<>();
		ArrayList<String> csvIdArr = new ArrayList<>();
		ArrayList<String> csvTestRealArr = new ArrayList<>();
		ArrayList<String> csvHashKeyArr = new ArrayList<>();
		csvIdArr.clear();
		csvHashKeyArr.clear();
		csvTestRealArr.clear();
		String glycan = "";

		for (int k = 0; k < csvBody.size(); k++) {
			String[] strArray = csvBody.get(k);
			if (strArray[0].length() > 10) {
				if (!testOrRealArr.contains(strArray[2].toString() + "," + strArray[0].toString())) {
					hashKeyArr.add(strArray[0].toString());
					testOrRealArr.add(strArray[2].toString() + "," + strArray[0].toString());
				}
			}
		}
		for (int i = 0; i < hashKeyArr.size(); i++) {
			csvHashKeyArr.add(hashKeyArr.get(i));
			String strTestOrReal = testOrRealArr.get(i).split(",")[0];
			csvTestRealArr.add(strTestOrReal);
			StringBuilder outputString = new StringBuilder();
			String apiUrl;
			if (strTestOrReal.equalsIgnoreCase("beta")) {
					apiUrl = accessionIdByHashKeyBetaApi;
			} else {
				apiUrl = accessionIdByHashKeyRealApi;
			}
			
			if (!getAccessionIdByHashKey(apiUrl, hashKeyArr.get(i), outputString)) {
				return;
			}
			glytoucanId = outputString.toString();
			
			csvIdArr.add(glytoucanId);
			glycanArr.add(glytoucanId);

		}
		// Read existing file
		idlistBackupFile = new File(glycanIDDirectory.getPath() + File.separator + "glytoucanIdListOldDataBackup.csv");
		if (!idlistBackupFile.exists()) {
			FileUtils.copy(glytoucanIDFilepath, idlistBackupFile);
		}
		for (int k = 0; k < csvBody.size(); k++) {
			String[] strArray = csvBody.get(k);
			for (int j = 0; j < strArray.length; j++) {
				for (int l = 0; l < csvHashKeyArr.size(); l++) {
					if (strArray[0].equals(csvHashKeyArr.get(l))
							&& strArray[2].equalsIgnoreCase(csvTestRealArr.get(l))) {

						strArray[0] = csvIdArr.get(l);

					}
				}
			}
		}
		csvHashKeyArr.clear();
		csvIdArr.clear();
		csvTestRealArr.clear();
		reader.close(); // Write to CSV file which is open
		CSVWriter writer = new CSVWriter(new FileWriter(glytoucanIDFilepath));
		writer.writeAll(csvBody);
		writer.flush();
		writer.close();
		Long beforecount = countLineCSV(idlistBackupFile + "");
		Long aftercount = countLineCSV(glytoucanIDFilepath + "");
		if (beforecount != aftercount) {
			// not equal line counts and backup
			glytoucanIDFilepath.delete();
			idlistBackupFile.renameTo(new File(glycanIDDirectory.getPath() + File.separator + "glytoucanIdList.csv"));
		} else {
			// equal line counts
			idlistBackupFile.delete();
		}

		for (int g = glycanArr.size() - 1; g > -1; g--) {
			if (glycanArr.get(g).length() < 10) {
				glycan += "Accession ID of \"" + hashKeyArr.get(g) + " \" is \"" + glycanArr.get(g) + "\"\n";
			} else {
				glycan += "Accession ID of \"" + hashKeyArr.get(g) + "\" is not avaliable now." + "\n";
			}
		}
		if (!glycan.equals("") && glycanArr.size() > 0) {
			JOptionPane.showMessageDialog(null, glycan, "Accession ID", JOptionPane.INFORMATION_MESSAGE, null);
			glycan = "";
			glycanArr.clear();
			model.setRowCount(0);
			model = readCSV();
			model.fireTableDataChanged();
			table.repaint();
			table.setModel(model);
			table.setVisible(true);
			table.updateUI();
			sorter = new TableRowSorter<TableModel>(model);
			table.setRowSorter(sorter);
			paginateFilterAndButton();
			table.getColumnModel().getColumn(0).setMaxWidth(35);
			table.getColumnModel().getColumn(1).setPreferredWidth(200);
			table.getColumnModel().getColumn(2).setPreferredWidth(250);
			table.getColumnModel().getColumn(3).setPreferredWidth(20);
			table.setRowHeight(25);
			
		}
	}

	/**
	 * Get Accession Id By HashKey
	 */
	private static boolean getAccessionIdByHashKey(String apiUrl, String hashKey, StringBuilder outputString) {
		try {
			String glytoucanId = hashKey;
			HttpResponse<JsonNode> jsonResponse = Unirest
					.get(apiUrl
							+ hashKey)
					.header("accept", "application/json").header("Content-Type", "application/json").asJson();
			JSONArray jsonarr = jsonResponse.getBody().getObject().getJSONArray("results");
			if (jsonarr.length() > 0) {
				for (Object o : jsonarr) {
					JSONObject jsonLineItem = (JSONObject) o;
					String accession = jsonLineItem.getString("accession");
					if (!accession.isEmpty() && accession.length() < 10) {
						glytoucanId = accession;
					}
				}
			}
			outputString.append(glytoucanId);
			return true;
		} catch (UnirestException e) {
			if (e.getMessage().contains("org.apache.http.conn.HttpHostConnectException")) {
				JOptionPane.showMessageDialog(null, "Server service is unavailable now",
						"Server Error!", JOptionPane.WARNING_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(null,
						"Please, check your internet connection again!", "Connection Error",
						JOptionPane.ERROR_MESSAGE);
			}
			return false;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"UnExecpted error occur.\nPlease Try again your process!", "UnExecpted Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
}
