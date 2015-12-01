package net.paoding.analysis.analyzer.estimate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import net.paoding.analysis.analyzer.PaodingAnalyzer;
import net.paoding.analysis.knife.PaodingMaker;

import org.apache.lucene.analysis.Analyzer;

public class TryPaodingAnalyzer {
	private static final String ARGS_TIP = ":";
	static String input = null;
	static String file = null;
	static Reader reader = null;
	static String charset = null;
	static String mode = null;
	static String analyzerName = null;
	static String print = null;
	static String properties = PaodingMaker.DEFAULT_PROPERTIES_PATH;
	
	public static void main(String[] args) {
		try {
			resetArgs();
			
			int inInput = 0;
			for (int i = 0; i < args.length; i++) {
				if (args[i] == null || (args[i] = args[i].trim()).length() == 0) {
					continue;
				}
				if (args[i].equals("--file") || args[i].equals("-f")) {
					file = args[++i];
				} else if (args[i].equals("--charset") || args[i].equals("-c")) {
					charset = args[++i];
				} else if (args[i].equals("--mode") || args[i].equals("-m")) {
					mode = args[++i];
				} else if (args[i].equals("--properties") || args[i].equals("-p")) {
					properties = args[++i];
				} else if (args[i].equals("--analyzer") || args[i].equals("-a")) {
					analyzerName = args[++i];
				} else if (args[i].equals("--print") || args[i].equals("-P")) {
					print = args[++i];
				} else if (args[i].equals("--input") || args[i].equals("-i")) {
					inInput++;
				} else if (args[i].equals("--help") || args[i].equals("-h")
						|| args[i].equals("?")) {
					printHelp();
					return;
				} else {
					if (!args[i].startsWith("-")
							&& (i == 0 || args[i - 1].equals("-i") || args[i - 1].equals("--input") || !args[i - 1].startsWith("-"))) {
						if (input == null) {
							input = args[i];
						} else {
							input = input + ' ' + args[i];
						}
						inInput++;
					}
				}
			}
			if (file != null) {
				input = null;
				reader = getReader(file, charset);
			}
			//
			analysing();
		} catch (Exception e1) {
			resetArgs();
			e1.printStackTrace();
		}
	}



	private static void resetArgs() {
		input = null;
		file = null;
		reader = null;
		charset = null;
		mode = null;
		print = null;
		analyzerName = null;
		properties = PaodingMaker.DEFAULT_PROPERTIES_PATH;
	}
	

	
	private static void analysing() throws Exception {
		Analyzer analyzer;
		if (analyzerName == null || analyzerName.length() == 0 || analyzerName.equalsIgnoreCase("paoding")) {
			analyzer = new PaodingAnalyzer(properties);
			if (mode != null) {
				((PaodingAnalyzer) analyzer).setMode(mode);
			}
		}
		else {
			Class clz;
			if (analyzerName.equalsIgnoreCase("standard")) {
				analyzerName = "org.apache.lucene.analysis.standard.StandardAnalyzer";
			}
			else if (analyzerName.equalsIgnoreCase("cjk")) {
				analyzerName = "org.apache.lucene.analysis.cjk.CJKAnalyzer";
			}
			else if (analyzerName.equalsIgnoreCase("cn") || analyzerName.equalsIgnoreCase("chinese")) {
				analyzerName = "org.apache.lucene.analysis.cn.ChineseAnalyzer";
			}
			else if (analyzerName.equalsIgnoreCase("st") || analyzerName.equalsIgnoreCase("standard")) {
				analyzerName = "org.apache.lucene.analysis.standard.StandardAnalyzer";
			}
			clz = Class.forName(analyzerName);
			analyzer = (Analyzer) clz.newInstance();
		}
		boolean readInputFromConsle = false;
		Estimate estimate = new Estimate(analyzer);
		if (print != null) {
			estimate.setPrint(print);
		}
		while (true) {
			if (reader == null) {
				if (input == null || input.length() == 0 || readInputFromConsle) {
					input = getInputFromConsole();
					readInputFromConsle = true;
				}
				if (input == null || input.length() == 0) {
					System.out.println("Warn: none charactors you input!!");
					continue;
				}
				else if (input.startsWith(ARGS_TIP)) {
					String argsStr = input.substring(ARGS_TIP.length());
					main(argsStr.split(" "));
					continue;
				}
			}
			if (reader != null) {
				estimate.test(System.out, reader);
				reader = null;
			}
			else {
				estimate.test(System.out, input);
				input = null;
			}
			System.out.println("--------------------------------------------------");
			if (false == readInputFromConsle) {
				return;
			}
		}
	}

	private static String getInputFromConsole() throws IOException {
		printTitleIfNotPrinted("");
		String input = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		String line;
		do {
			System.out.print("paoding> ");
			line = reader.readLine();
			if (line == null || line.length() == 0) {
				continue;
			}
			if (line.equals(ARGS_TIP + "clear") || line.equals(ARGS_TIP + "c")) {
				input = null;
				System.out.println("paoding> Cleared");
				return getInputFromConsole();
			}
			else if (line.equals(ARGS_TIP + "exit") || line.equals(ARGS_TIP + "quit") || line.equals(ARGS_TIP + "e") || line.equals(ARGS_TIP + "q") ) {
				System.out.println("Bye!");
				System.exit(0);
			}
			else if (input == null && line.startsWith(ARGS_TIP)) {
				input = line;
				break;
			}
			else {
				if (line.endsWith(";")) {
					if (line.length() > ";".length()) {
						input = line.substring(0, line.length() - ";".length());
					}
					break;
				}
				else {
					if (input == null) {
						input = line;
					} else {
						input = input + "\n" + line;
					}
				}
			}
		} while (true);
		return input == null ? null : input.trim();
	}

	private static void printHelp() {
		String app = System.getProperty("paoding.try.app",
				"TryPaodingAnalyzer");
		String cmd = System.getProperty("paoding.try.cmd", "java "
				+ TryPaodingAnalyzer.class.getName());

		titlePrinted = false;
		printTitleIfNotPrinted("\t");
	}
	
	
	private static boolean titlePrinted = false;
	private static boolean welcomePrinted = false;
	private static void printTitleIfNotPrinted(String prefix) {
		if (!titlePrinted) {
			System.out.println();
			if (!welcomePrinted) {
				System.out.println("Welcome to Paoding Analyser(2.0.4-alpha2)");
				System.out.println();
				welcomePrinted = true;
			}

			titlePrinted = true;
			
		}
	}
	
	
		
	static String getContent(String path, String encoding) throws IOException {
		return (String) read(path, encoding, true);
	}
	
	static Reader getReader(String path, String encoding) throws IOException {
		return (Reader) read(path, encoding, false);
	}
	
	static Object read(String path, String encoding, boolean return_string) throws IOException {
		InputStream in;
		if (path.startsWith("classpath:")) {
			path = path.substring("classpath:".length());
			URL url = Estimate.class.getClassLoader().getResource(path);
			if (url == null) {
				throw new IllegalArgumentException("Not found " + path
						+ " in classpath.");
			}
			System.out.println("read content from:" + url.getFile());
			in = url.openStream();
		} else {
			File f = new File(path);
			if (!f.exists()) {
				throw new IllegalArgumentException("Not found " + path
						+ " in system.");
			}
			System.out.println("read content from:" + f.getAbsolutePath());
			in = new FileInputStream(f);
		}
		Reader re;
		if (encoding != null) {
			re = new InputStreamReader(in, encoding);
		} else {
			re = new InputStreamReader(in);
		}
		if (!return_string) {
			return re;
		}
		char[] chs = new char[1024];
		int count;
		StringBuffer content = new StringBuffer();
		while ((count = re.read(chs)) != -1) {
			content.append(chs, 0, count);
		}
		re.close();
		return content.toString();
		}
}
