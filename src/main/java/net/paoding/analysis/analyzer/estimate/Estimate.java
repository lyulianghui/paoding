package net.paoding.analysis.analyzer.estimate;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;

import net.paoding.analysis.analyzer.PaodingTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
//import org.apache.lucene.analysis.tokenattributes.TermAttribute;

public class Estimate {
	private Analyzer analyzer;
	private String print;
	private PrintGate printGate;

	public Estimate() {
		this.setPrint("50");// Ĭ��ֻ��ӡǰ50�зִ�Ч��1?7
	}

	public Estimate(Analyzer analyzer) {
		setAnalyzer(analyzer);
		this.setPrint("50");// Ĭ��ֻ��ӡǰ50�зִ�Ч��1?7
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public void setPrint(String print) {
		if (print == null || print.length() == 0
				|| print.equalsIgnoreCase("null")
				|| print.equalsIgnoreCase("no")) {
			printGate = null;
			this.print = null;
		} else {
			printGate = new LinePrintGate();
			printGate.setPrint(print, 10);
			this.print = print;
		}
	}

	public String getPrint() {
		return print;
	}

	public void test(String input) {
		this.test(System.out, input);
	}

	public void test(PrintStream out, String input) {
		Reader reader = new StringReaderEx(input);
		this.test(out, reader);
	}

	public void test(PrintStream out, Reader reader) {
		try {
			long begin = System.currentTimeMillis();
			
			LinkedList<CToken> list = new LinkedList<CToken>();
			int wordsCount = 0;
			
			//collect token
			TokenStream ts = analyzer.tokenStream("", reader);
			ts.reset();
			CharTermAttribute termAtt = (CharTermAttribute) ts
					.addAttribute(CharTermAttribute.class);
			while (ts.incrementToken()) {
				if (printGate != null && printGate.filter(wordsCount)) {
					//??4.0
					//list.add(new CToken(termAtt.term(), wordsCount));
					list.add(new CToken(new String(termAtt.buffer()), wordsCount));
				}
				wordsCount++;
			}
			
			long end = System.currentTimeMillis();
			int c = 0;
			if (list.size() > 0) {
				for (CToken ctoken : list) {
					c = ctoken.i;
					if (c % 10 == 0) {
						if (c != 0) {
							out.println();
						}
						out.print((c / 10 + 1) + ":\t");
					}
					out.print(ctoken.t + "/");
				}
			}
			if (wordsCount == 0) {
				System.out.println("\tAll are noise characters or words");
			} else {
				if (c % 10 != 1) {
					System.out.println();
				}
				String inputLength = "<δ֪>";
				if (reader instanceof StringReaderEx) {
					inputLength = "" + ((StringReaderEx) reader).inputLength;
				} else if (ts instanceof PaodingTokenizer) {
					inputLength = "" + ((PaodingTokenizer) ts).getInputLength();
				}
				System.out.println();
				System.out.println("\tworking class:  " + analyzer.getClass().getName());
				System.out.println("\tsourceLength: " + inputLength );
                System.out.println("\twordsCount: " + wordsCount);
				System.out.println("\ttime: " + (end - begin) + "ms ");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}

	// -------------------------------------------

	static class CToken {
		String t;
		int i;

		CToken(String t, int i) {
			this.t = t;
			this.i = i;
		}
	}

	static interface PrintGate {
		public void setPrint(String print, int unitSize);

		boolean filter(int count);
	}

	static class PrintGateToken implements PrintGate {
		private int begin;
		private int end;

		public void setBegin(int begin) {
			this.begin = begin;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		public void setPrint(String print, int unitSize) {
			int i = print.indexOf('-');
			if (i > 0) {
				int bv = Integer.parseInt(print.substring(0, i));
				int ev = Integer.parseInt(print.substring(i + 1));
				setBegin(unitSize * (Math.abs(bv) - 1));// ��1?7?���Ǵ���1?7Ū1?7ʼ��
				setEnd(unitSize * Math.abs(ev));// ����10�У��ǽ�ֹ��100(����ñ߽�)
			} else {
				setBegin(0);
				int v = Integer.parseInt(print);
				setEnd(unitSize * (Math.abs(v)));
			}
		}

		public boolean filter(int count) {
			return count >= begin && count < end;
		}
	}

	static class LinePrintGate implements PrintGate {

		private PrintGate[] list;

		public void setPrint(String print, int unitSize) {
			String[] prints = print.split(",");
			list = new PrintGate[prints.length];
			for (int i = 0; i < prints.length; i++) {
				PrintGateToken pg = new PrintGateToken();
				pg.setPrint(prints[i], unitSize);
				list[i] = pg;
			}
		}

		public boolean filter(int count) {
			for (int i = 0; i < list.length; i++) {
				if (list[i].filter(count)) {
					return true;
				}
			}
			return false;
		}

	}

	static class StringReaderEx extends StringReader {
		private int inputLength;

		public StringReaderEx(String s) {
			super(s);
			inputLength = s.length();
		}
	}

}
