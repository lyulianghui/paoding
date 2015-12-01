/**
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.paoding.analysis.analyzer;

import java.util.Properties;

import net.paoding.analysis.Constants;
import net.paoding.analysis.analyzer.estimate.TryPaodingAnalyzer;
import net.paoding.analysis.knife.Knife;
import net.paoding.analysis.knife.Paoding;
import net.paoding.analysis.knife.PaodingMaker;

/**
 * PaodingAnalyzer�ǻ��ڡ��Ҷ���ţ����ܵ�Lucene������������ǡ��Ҷ���ţ����ܶ�Lucene����������
 * <p>
 * 
 * PaodingAnalyzer���̰߳�ȫ�ģ����������ʹ��ͬһ��PaodingAnalyzerʵ���ǿ��еġ�<br>
 * PaodingAnalyzer�ǿɸ��õģ��Ƽ����ͬһ��PaodingAnalyzerʵ��
 * <p>
 * 
 * PaodingAnalyzer�Զ���ȡ��·���µ�paoding-analysis.properties�����ļ���װ��PaodingAnalyzer
 * <p>
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @see PaodingAnalyzerBean
 * 
 * @since 1.0
 * 
 */
public class PaodingAnalyzer extends PaodingAnalyzerBean {

	/**
	 * �����·���µ�paoding-analysis.properties����һ��PaodingAnalyzer����
	 * <p>
	 * ��һ��JVM�У��ɶ�δ����������ζ�ȡ�����ļ��������ظ���ȡ�ֵ䡣
	 */
	public PaodingAnalyzer() {
		this(PaodingMaker.DEFAULT_PROPERTIES_PATH);
	}

	/**
	 * @param propertiesPath null��ʾʹ����·���µ�paoding-analysis.properties
	 */
	public PaodingAnalyzer(String propertiesPath) {
		init(propertiesPath);
	}

	protected void init(String propertiesPath) {
		// ���PaodingMaker˵����
		// 1����ε���getProperties()�����صĶ���ͬһ��propertiesʵ��(ֻҪ�����ļ�û������޸�)
		// 2����ͬ��propertiesʵ��PaodingMakerҲ������ͬһ��Paodingʵ��
		// �������1��2��˵�����ڴ��ܹ���֤��δ���PaodingAnalyzer��������װ�������ļ��ʹʵ�
		if (propertiesPath == null) {
			propertiesPath = PaodingMaker.DEFAULT_PROPERTIES_PATH;
		}
		Properties properties = PaodingMaker.getProperties(propertiesPath);
		String mode = Constants
				.getProperty(properties, Constants.ANALYZER_MODE);
		Paoding paoding = PaodingMaker.make(properties);
		setKnife(paoding);
		setMode(mode);
	}

	/**
	 * ������ΪPaodingAnalyzer����Ĳ������������� <br>
	 * ִ��֮���Բ鿴�ִ�Ч��������ѡһ�ַ�ʽ����:
	 * <p>
	 * 
	 * java net...PaodingAnalyzer<br>
	 * java net...PaodingAnalyzer --help<br>
	 * java net...PaodingAnalyzer �л����񹲺͹�<br>
	 * java net...PaodingAnalyzer -m max �л����񹲺͹�<br>
	 * java net...PaodingAnalyzer -f c:/text.txt<br>
	 * java net...PaodingAnalyzer -f c:/text.txt -c utf-8<br>
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (System.getProperty("paoding.try.app") == null) {
			System.setProperty("paoding.try.app", "PaodingAnalyzer");
			System.setProperty("paoding.try.cmd", "java PaodingAnalyzer");
		}
		TryPaodingAnalyzer.main(args);
	}

	// --------------------------------------------------

	/**

	 */
	public PaodingAnalyzer(Knife knife, int mode) {
		super(knife, mode);
	}

	/**
	 * �ȼ���maxMode()
	 * 
	 * @param knife
	 * @return
	 * @deprecated
	 */
	public static PaodingAnalyzer queryMode(Knife knife) {
		return maxMode(knife);
	}

	/**
	 * 
	 * @param knife
	 * @return
	 * @deprecated
	 */
	public static PaodingAnalyzer defaultMode(Knife knife) {
		return new PaodingAnalyzer(knife, MOST_WORDS_MODE);
	}

	/**
	 * 
	 * @param knife
	 * @return
	 * @deprecated
	 */
	public static PaodingAnalyzer maxMode(Knife knife) {
		return new PaodingAnalyzer(knife, MAX_WORD_LENGTH_MODE);
	}

	/**
	 * �ȼ���defaultMode()
	 * 
	 * @param knife
	 * @return
	 * @deprecated
	 * 
	 */
	public static PaodingAnalyzer writerMode(Knife knife) {
		return defaultMode(knife);
	}
}
