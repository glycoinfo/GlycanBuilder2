package org.eurocarbdb.application.glycanbuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.eurocarbdb.application.glycanbuilder.util.SelectAPIDialog;
import org.eurocarbdb.application.glycanbuilder.util.UserLoginDialog;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

/**
 * Glycan Structure Send to API and Change User Contributor Id and API Key
 * @author GIC 20211220
 */
public class GlycanStructureAndChangeUser extends JComponent {

	protected GlycanDocument theDoc;
	private JFrame theParent = null;

	private static String reg_url;
	private static String env;                     
	private boolean exists = false;
	private static final String accessionIdBetaApi = "https://sparqlist.glyconavi.org/api/Get_beta_accession_number_by_wurcs_text?wurcs=";
	private static final String accessionIdRealApi = "https://api.glycosmos.org/glycanformatconverter/2.7.0/wurcs2wurcs";
	private static final String registerBetaApi = "https://api.gtc.beta.glycosmos.org/glycan/register";
	private static final String registerRealApi = "https://api.glytoucan.org/glycan/register";

	/**
	 * Get Directory of files for application
	 */
	public static File getConfigurationDirectory() throws IOException {
		String userHomeDirectory = System.getProperty("user.home");
		String osName = System.getProperty("os.name");

		File configurationFile;
		if (osName.equals("Linux")) {
			configurationFile = new File(userHomeDirectory + File.separator + ".GlycoWorkbench");
		} else if (osName.startsWith("Windows")) {
			String applicationDataDirectory = System.getenv("APPDATA");

			if (applicationDataDirectory == null) {
				applicationDataDirectory = userHomeDirectory + File.separator + "Application Data";
			}

			configurationFile = new File(applicationDataDirectory + File.separator + "GlycoWorkBench");
		} else {
			configurationFile = new File(userHomeDirectory + File.separator + ".GlycoWorkbench");
		}

		if (!configurationFile.exists()) {
			if (!configurationFile.mkdir()) {
				throw new IOException("Could not create directory: " + configurationFile.toString());
			}
		}
		return configurationFile;
	}

	/**
	 * Get JsonFile for Contributor ID and API Key
	 */
	public static File getJsonFile() throws IOException {
		File configDirectory = getConfigurationDirectory();
		File creatingJsonFile = new File(configDirectory.getPath() + File.separator + "Apikey.json");
		if (!creatingJsonFile.exists()) {
			creatingJsonFile.createNewFile();
		}
		return creatingJsonFile;
	}
	
	/**
	 * Read GlyTouCanId of CSV File
	 */
	public static CSVReader readCSVFile() throws IOException {
		File configDirectory = getConfigurationDirectory();
		File glytoucanIDFilepath = new File(configDirectory.getPath() + File.separator + "GlytoucanIdList.csv");
		if (!glytoucanIDFilepath.exists()) {
			glytoucanIDFilepath.createNewFile();
		}		
		CSVReader csvreader = new CSVReader(new FileReader(glytoucanIDFilepath));
		return csvreader;
	}

	/**
	 * Selection API environments for Get Accession Id or HashKey    
	 */
	public void onSelectAPIDialog(JFrame frame, GlycanDocument doc) {
		theParent = frame;
		theDoc = doc;
			try {
				SelectAPIDialog dig = new SelectAPIDialog(this.theParent);
				dig.setVisible(true);
				if (!dig.isCanceled()) {
					boolean isBeta = dig.getFormat().contains("beta");
					reg_url = dig.getFormat();
					String keyPath = getJsonFile().toString();
					if (isBeta) {
						env = "beta";
					} else {
						env = "real";
					}
					getAccessionIdorHashKey(env, keyPath, reg_url);
				}
			} catch (Exception e) {
				e.getMessage();
			}
	}

	/**
	 * Get Accession Id or HashKey from API
	 */
	public void getAccessionIdorHashKey(String env, String keyPath, String reg_url)
			throws UnirestException {
		
		String strIdUrl;
		String strRegUrl;
		if (env == "beta"){
			strIdUrl = accessionIdBetaApi;
			strRegUrl = registerBetaApi;
		}
		else {
			strIdUrl = accessionIdRealApi;
			strRegUrl = registerRealApi;
		}
		
		try {
			ArrayList<String> sequences = getSequences();
			if (sequences.size() == 0){
				JOptionPane.showMessageDialog(null, "You haven't draw the Glycan structure in the area", "GLYCAN STRUCTURE",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			String glycan_id = "";
			int index = 0;
			CSVReader csvreader = readCSVFile();
			List<String[]> glycan_sequences = csvreader.readAll();
			csvreader.close();
			for (String sequence : sequences) {
				exists = false;
				String res = "";
				// get accession id
				StringBuilder strAccId = new StringBuilder();
				if (!getAccessionId(strIdUrl, sequence, strAccId)) {
					return;
				}
				String accession = strAccId.toString();

				// checking accession id HAVE OR NOT
				if (accession.isEmpty() || accession == null) {

					// read the json file whether the contri_id and api key exit or not
					JSONParser jsonParser = new JSONParser();
					StringBuilder strConId = new StringBuilder();
					StringBuilder strApiKey = new StringBuilder();
					boolean saveConIdandApiKey = false;
					// get JSON file size
					long fsize = new File(keyPath).length();
					if (fsize < 1) {
						saveConIdandApiKey = true;
						if (!inputConIdandApiKey(strConId, strApiKey)) {
							return;
						}
					}
					else {
						Reader reader = new FileReader(keyPath);
						Object obj = jsonParser.parse(reader);
						JSONObject jsonObject = (JSONObject) obj;
						// if no id and key, show input dialog
						if (!(boolean) jsonObject.keySet().contains(env)) {
							
							saveConIdandApiKey = true;
							if (!inputConIdandApiKey(strConId, strApiKey)) {
								return;
							}
						}
						else {
							// contributor id and api key from JSON file
							JSONObject obj1 = (JSONObject) jsonObject.get(env);
							strConId.append((String) obj1.get("contributor_id"));
							strApiKey.append((String) obj1.get("api_key"));
						}
					}

					// get hash key
					StringBuilder strHashKey = new StringBuilder();
					int ret = getHashKey(strRegUrl, sequence, strConId.toString(), strApiKey.toString(), strHashKey);
					// Stop process error occur
					if (ret == 2) {
						return;
					} // invalid contri_id and api key
					else if (ret == 1) {
						saveConIdandApiKey = true;
						boolean nextAgain = true;
						while (nextAgain) {
							strConId = new StringBuilder();
							strApiKey = new StringBuilder();
							if (!inputConIdandApiKey(strConId, strApiKey)) {
								return;
							}
							
							int ret1 = getHashKey(strRegUrl, sequence, strConId.toString(), strApiKey.toString(), strHashKey);
							// Stop process error occur
							if (ret1 == 2) {
								return;
							} // success
							else if (ret1 == 0) {
								nextAgain = false;
							}
						}
					}
					res = strHashKey.toString();
					if (!res.isEmpty()) {
						glycan_id += " Hash Key of Glycan Structure" + (++index) + " is \"" + res + "\"\n";
					}
					
					// if new key , save it in JSON file
					if (saveConIdandApiKey) {
						writeConIDandApiKey(strRegUrl, strConId.toString(), strApiKey.toString());
					}
				} else {
					res = accession;
					if (accession != null) {
						glycan_id += " Accession ID of GlycanStructure" + (++index) + " is \"" + accession + "\"\n";
					}
				}
				// read csv file and add new sequence
				glycan_sequences = getCSVData(res, sequence, env, glycan_sequences);
			}
			JOptionPane.showMessageDialog(null, glycan_id, "Accession ID", JOptionPane.INFORMATION_MESSAGE);
			writeCSVData(glycan_sequences);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"UnExecpted error occur.\nPlease Try again your process!", "UnExecpted Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Get Accession Id By Sequence
	 */
	private boolean getAccessionId(String apiUrl, String sequence, StringBuilder outputString) {
		try {
			HttpResponse<String> response = null;
			Unirest.setTimeouts(0, 0);
			if (env == "beta"){
				String encodedSeq =  URLEncoder.encode(sequence, StandardCharsets.UTF_8.displayName());
				response = Unirest.post(apiUrl + encodedSeq).asString();
				String responseBody = response.getBody();
				if (responseBody.length() < 10) {
					outputString.append(responseBody);
				} 
			}
			else {
				response = Unirest.post(apiUrl)
						.header("Content-Type", "application/x-www-form-urlencoded").field("str", sequence)
						.asString();
				String responseBody = response.getBody();
				int responseStatus = response.getStatus();
				char status = Integer.toString(responseStatus).charAt(0);
				responseStatus = Character.getNumericValue(status);
				if (responseStatus == 5) {
					JOptionPane.showMessageDialog(null, "Server service is unavailable now",
							"Server Error!", JOptionPane.WARNING_MESSAGE);
					return false;
				}
				
				JSONParser parser2 = new JSONParser();
				JSONObject json2 = (JSONObject) parser2.parse(responseBody);
				outputString.append((String) json2.get("id"));
			}
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
	
	/**
	 * Get HashKey
	 */
	private int getHashKey(String apiUrl, String sequence, String conId, String apiKey, StringBuilder outputString) {
		String ConvertString = conId + ":" + apiKey;
		byte[] encodedBytes = Base64.getEncoder().encode(ConvertString.getBytes());
		String authorization = new String(encodedBytes);
		HttpResponse<String> response = null;
		try {
			Unirest.setTimeouts(0, 0);
			response = Unirest.post(apiUrl)
					.header("Content-Type", "application/json").header("Accept", "application/json")
					.header("Authorization", "Basic " + authorization)
					.body("{ \"sequence\": \"" + sequence + "\" }").asString();
			String responseBody = response.getBody();
			int responseStatus = response.getStatus();
			char s = Integer.toString(responseStatus).charAt(0);
			responseStatus = Character.getNumericValue(s);
			if (responseStatus == 4) {
				JOptionPane.showMessageDialog(null, "Your Contributor Id or API Key was wrong",
						"Key error!", JOptionPane.ERROR_MESSAGE);
				return 1;
			} else if (responseStatus == 5) {
				JOptionPane.showMessageDialog(null, "Server service is unavailable now",
						"Service Unavailable Error!", JOptionPane.WARNING_MESSAGE);
				return 2;
			}
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(responseBody);
			outputString.append((String) json.get("message"));
			return 0;
		} catch (UnirestException e) {
			if (e.getMessage().contains("org.apache.http.conn.HttpHostConnectException")) {
				JOptionPane.showMessageDialog(null, "Server service is unavailable now",
						"Server Error!", JOptionPane.WARNING_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(null,
						"Please, check your internet connection again!", "Connection Error",
						JOptionPane.ERROR_MESSAGE);
			}
			return 2;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"UnExecpted error occur.\nPlease Try again your process!", "UnExecpted Error",
					JOptionPane.ERROR_MESSAGE);
			return 2;
		}
	}
	
	/**
	 * User Input ConId and ApiKey
	 */
	private boolean inputConIdandApiKey(StringBuilder strConId, StringBuilder strApiKey)
	{
		UserLoginDialog dlg = new UserLoginDialog(theParent, null, null);
		boolean showAgain = true;
		while (showAgain) {
			dlg.setVisible(true);
			if (!dlg.isCanceled()) {
				// check if contributorId and api key is entered
				if (dlg.getContributorId().isEmpty() && dlg.getApikey().isEmpty()) {
					JOptionPane.showMessageDialog(null,
					"Need to insert both Contributor ID and API Key", "Mandatory Error!",
					JOptionPane.WARNING_MESSAGE);
				}else if(dlg.getContributorId().isEmpty() && !dlg.getApikey().isEmpty()) {
					JOptionPane.showMessageDialog(null,
					"Please enter Contributor ID", "Mandatory Error!",
					JOptionPane.WARNING_MESSAGE);
				}else if(!dlg.getContributorId().isEmpty() && dlg.getApikey().isEmpty()) {
					JOptionPane.showMessageDialog(null,
					"Please enter API Key", "Mandatory Error!",
					JOptionPane.WARNING_MESSAGE);
				}else {
					strConId.append(dlg.getContributorId());
					strApiKey.append(dlg.getApikey());
					showAgain = false;
				}
			} else {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Write ConId and ApiKey to Json file
	 */
	private boolean writeConIDandApiKey(String apiUrl, String conId, String apiKey)
	{
		try {
		String keyPath = getJsonFile().toString();
			try {
				JSONParser jsonParser = new JSONParser();
				FileReader reader = new FileReader(getJsonFile().toString());
				JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
				JSONObject envNewObj = new JSONObject();
				envNewObj.remove("contributor_id");
				envNewObj.put("contributor_id", conId);
				envNewObj.remove("api_key");
				envNewObj.put("api_key", apiKey);
				envNewObj.remove("reg_url");
				envNewObj.put("reg_url", apiUrl);
				jsonObject.put(env, envNewObj);
				FileWriter file = new FileWriter(keyPath);
				file.write(jsonObject.toJSONString());
				file.close();
				return true;
	
			} catch (ParseException e) {
				JSONObject envNewObj = new JSONObject();
				JSONObject envNewObj1 = new JSONObject();
				envNewObj.put("contributor_id", conId);
				envNewObj.put("api_key", apiKey);
				envNewObj.put("reg_url", apiUrl);
				envNewObj1.put(env, envNewObj);
				FileWriter file = new FileWriter(keyPath);
				file.write(envNewObj1.toJSONString());
				file.close();
				return true;
			} 
		}catch (Exception ex) {
			JOptionPane.showMessageDialog(null,
					"UnExecpted error occur.\nPlease Try again your process!", "UnExecpted Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
	
	/**
	 * Read Data From CSV
	 */
	private List<String[]> getCSVData(String id, String seq, String env, List<String[]> g_seq) {
		String[] glycan_data = new String[3];
		List<String[]> glycan_sequences = g_seq;
		glycan_data[0] = id;
		glycan_data[1] = seq;
		glycan_data[2] = env;
		if (glycan_sequences != null) {
			// loop glycan data in csv
			for (int k = 0; k < glycan_sequences.size(); k++) {
				String[] strArray = glycan_sequences.get(k);
				for (int j = 1; j < strArray.length - 1; j++) {
					// check sequence already exists
					if (strArray[j].equalsIgnoreCase(glycan_data[1]) && strArray[j + 1].equals(glycan_data[2])) {
						// overwrite latest data
						glycan_sequences.set(k, glycan_data);
						exists = true;
					}
				}
			}
		}

		// not exists -> insert new row
		if (!exists) {
			glycan_sequences.add(glycan_data);
		}
		return glycan_sequences;
	}

	/**
	 * Get sequence to get Accession Id or HashKey
	 */
	private ArrayList<String> getSequences() throws Exception {
		ArrayList<String> seq_strArr = new ArrayList<>();;
		if (this.theDoc.getStructures().size() > 0) {
			seq_strArr = this.theDoc.exportFromStructures(this.theDoc.getStructures(), "wurcs2");
		}

		return seq_strArr;
	}

	/**
	 * Write Accession Id or HashKey to CSV
	 */
	private void writeCSVData(List<String[]> glycan_sequences) {
		try {
			File configDirectory = getConfigurationDirectory();
			File glytoucanIDFilepath = new File(configDirectory.getPath() + File.separator + "glytoucanIdList.csv");
			try (CSVWriter writer = new CSVWriter(new FileWriter(glytoucanIDFilepath))) {
				writer.writeAll(glycan_sequences);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Change User
	 */
	public void changeUser() throws ParseException {
		try {
			SelectAPIDialog dig = new SelectAPIDialog(theParent);
			dig.setVisible(true);
			if (!dig.isCanceled()) {
				boolean isBeta = dig.getFormat().contains("beta");
				if (isBeta) {
					env = "beta";
				} else {
					env = "real";
				}
				reg_url = dig.getFormat();
				showUserDialogBox();
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"UnExecpted error occur.\nPlease Try again your process!", "UnExecpted Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	/**
	 * Show Input Dialog for Change User
	 */
	private void showUserDialogBox() {
		String strRegUrl;
		String sequence = "";
		StringBuilder strConId = new StringBuilder();
		StringBuilder strApiKey = new StringBuilder();
		if (env == "beta"){
			strRegUrl = registerBetaApi;
		}
		else {
			strRegUrl = registerRealApi;
		}
		
		if (!inputConIdandApiKey(strConId, strApiKey)) {
			return;
		}
		// get hash key
		StringBuilder strHashKey = new StringBuilder();
		int ret = getHashKey(strRegUrl, sequence, strConId.toString(), strApiKey.toString(), strHashKey);
		// Stop process error occur
		if (ret == 2) {
			return;
		} // invalid contri_id and api key
		else if (ret == 1) {
			boolean nextAgain = true;
			while (nextAgain) {
				strConId = new StringBuilder();
				strApiKey = new StringBuilder();
				if (!inputConIdandApiKey(strConId, strApiKey)) {
					return;
				}
				
				int ret1 = getHashKey(strRegUrl, sequence, strConId.toString(), strApiKey.toString(), strHashKey);
				// Stop process error occur
				if (ret1 == 2) {
					return;
				} // success
				else if (ret1 == 0) {
					nextAgain = false;
				}
			}
		}

		// Save it in jason file   
		if (writeConIDandApiKey(strRegUrl, strConId.toString(), strApiKey.toString())) {
			JOptionPane.showMessageDialog(null,
					"Change Successfully", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
