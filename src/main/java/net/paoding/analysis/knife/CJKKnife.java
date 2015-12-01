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
package net.paoding.analysis.knife;

import net.paoding.analysis.dictionary.Dictionary;
import net.paoding.analysis.dictionary.Hit;
import net.paoding.analysis.dictionary.Word;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 1.0
 * 
 */
public class CJKKnife implements Knife, DictionariesWare {

	// -------------------------------------------------

	private Dictionary vocabulary;
	private Dictionary noiseWords;
	private Dictionary noiseCharactors;
	private Dictionary units;

	// -------------------------------------------------

	public CJKKnife() {
	}

	public CJKKnife(Dictionaries dictionaries) {
		setDictionaries(dictionaries);
	}

	public void setDictionaries(Dictionaries dictionaries) {
		vocabulary = dictionaries.getVocabularyDictionary();
		noiseWords = dictionaries.getNoiseWordsDictionary();
		noiseCharactors = dictionaries.getNoiseCharactorsDictionary();
		units = dictionaries.getUnitsDictionary();
	}

	
	// -------------------------------------------------

	/**
	 * �ֽ���CJK�ַ�ʼ�ģ���ɴ��������֡�Ӣ����ĸ�����ߡ��»��ߵ��ַ���ɵ����
	 */
	public int assignable(Beef beef, int offset, int index) {
		char ch = beef.charAt(index);
		if (CharSet.isCjkUnifiedIdeographs(ch))
			return ASSIGNED;
		if (index > offset) {
			if (CharSet.isArabianNumber(ch) || CharSet.isLantingLetter(ch)
					|| ch == '-' || ch == '_') {
				return POINT;
			}
		}
		return LIMIT;
	}

	public int dissect(Collector collector, Beef beef, int offset) {
		// ��point == -1ʱ��ʾ���ηֽ�û������POINT���ʵ��ַ�
		// ���point != -1����ֵ��ʾPOINT�����ַ�Ŀ�ʼλ�ã�
		// ���λ�ý������أ���һ��Knife����pointλ�ÿ�ʼ�ִ�
		int point = -1;

		// ��¼ͬ���ַ�ִʽ�����λ��(������limitλ�õ��ַ�)-Ҳ����assignable��������LIMIT���ʵ��ַ��λ��
		// ���point==-1��limit�������أ���һ��Knife����limitλ�ÿ�ʼ���Էִ�
		int limit = offset + 1;

		// ����point��limit������ֵ:
		// ��ǰֱ������LIMIT�ַ�
		// �������������һ��POINT�ַ���Ὣ���¼Ϊpoint
		GO_UNTIL_LIMIT: while (true) {
			switch (assignable(beef, offset, limit)) {
			case LIMIT:
				break GO_UNTIL_LIMIT;
			case POINT:
				if (point == -1) {
					point = limit;
				}
			}
			limit++;
		}

		// ����offset��beef.length()���Ǳ���Knife�����Σ���Ӧ�������δ�����ַ���֧��һ���ʷ�������beef�еĴ���
		// ħ���߼���
		// Beef��ŵ:�������GO_UNTIL_LIMITѭ�����հ�limitֵ����Ϊbeef.length���ʾ��Ϊδ�����ַ�
		// ��Ϊbeefһ�������ı�ȫ����������һ��char='\0'��ֵ��Ϊ���һ��char��־����
		// �������ϵ�GO_UNTIL_LIMIT����limit=beef.length()֮ǰ���Ѿ�break����ʱlimit!=beef.length
		if (offset > 0 && limit == beef.length()) {
			return -offset;
		}

		// ��¼��ǰ���ڼ���(�Ƿ��Ǵʵ����)���ַ���beef�е�ʼֹλ��(��ʼλ�ã��������λ��)
		int curSearchOffset = offset, curSearchEnd;

		// ��¼��ǰ�����ӵ��ַ�ĳ��ȣ����ֵ�����(curSearchEnd - curSearchOffset)
		int curSearchLength;

		// ��ǰ���ӵ��ַ���жϽ��
		Hit curSearch = null;

		// ����Ҫ�жϵ��ַ�����ʼλ��
		// ������������ų�������ж�仯
		final int offsetLimit;
		if (point != -1)
			offsetLimit = point;
		else
			offsetLimit = limit;

		// ��¼����ǰΪֹ��ֳ��Ĵʵ�����������λ��
		int maxDicWordEnd = offset;

		// ��¼���Ĳ��ڴʵ��е��ַ�(��Ϊ�����ַ�)��beef��λ�ã�-1��ʾû�����λ��
		int isolatedOffset = -1;

		// ��¼����ǰΪֹ���ɴʵ����г��ʵ���󳤶ȡ�
		// ���ڸ����ж��Ƿ����shouldBeWord()�������԰�ǰ��������š������֮��ģ�����û�б��г����ַ���һ����
		// ����������maxDicWordLength��Ӧ���Լ�shouldBeWord()��ʵ��
		int maxDicWordLength = 0;

		// ��1��ѭ����λ�������ַ�Ŀ�ʼλ��
		// �����ӵ��ַ�ʼλ�õļ�����offsetLimit�����limit
		for (; curSearchOffset < offsetLimit; curSearchOffset++) {

			// �ڶ���ѭ����λ�������ַ�Ľ���λ��(�����λ�õ��ַ�)
			// �����ʼ״̬�ǣ������ӵ��ַ�һ����Ϊ1��������λ��Ϊ��ʼλ��+1
			curSearchEnd = curSearchOffset + 1;
			curSearchLength = 1;
			for (; curSearchEnd <= limit; curSearchEnd++, curSearchLength++) {

				/*
				 * Fix issue 50: �������ֽ������� 
				 */				
				//�������������������
				curSearch = searchNumber(beef, curSearchOffset, curSearchLength);
				if (curSearch.isHit()) {
					if (isolatedOffset >= 0) {
						dissectIsolated(collector, beef, isolatedOffset,
								curSearchOffset);
						isolatedOffset = -1;
					}
					
					// trick: ��index������������ʵ�ʽ���λ��
					int numberSearchEnd = curSearch.getIndex();
					int numberSearchLength = curSearch.getIndex() - curSearchOffset;

					// 1.2)
					// ����������λ��
					if (maxDicWordEnd < numberSearchEnd) {
						maxDicWordEnd = numberSearchEnd;
					}

					// 1.3)
					// ���´�����󳤶ȱ�����ֵ
					if (curSearchOffset == offset
							&& maxDicWordLength < numberSearchLength) {
						maxDicWordLength = numberSearchLength;
					}

					Word word = curSearch.getWord();
					if (!word.isNoise()) {
						dissectIsolated(collector, beef, curSearchOffset,
								curSearch.getIndex());
					}
					curSearchOffset = numberSearchEnd - 1;
					break;
				}
				if (curSearch.isUnclosed()) {
					continue;
				}

				// ͨ��ʻ���жϣ������жϽ��curSearch
				curSearch = vocabulary.search(beef, curSearchOffset,
						curSearchLength);

				// ---------------�������ص��жϽ��--------------------------

				// 1)
				// �Ӵʻ�����ҵ��˸ô���...
				if (curSearch.isHit()) {

					// 1.1)
					// ȷ�Ϲ����ַ�Ľ���λ��=curSearchOffset��
					// �������ӷ����ֽ�Ѵ�isolatedOffset��ʼ�ĵ�curSearchOffset֮��Ĺ����ַ�
					// �����ַ�ֽ���ϣ��������ַ�ʼλ��isolatedOffset���
					if (isolatedOffset >= 0) {
						dissectIsolated(collector, beef, isolatedOffset,
								curSearchOffset);
						isolatedOffset = -1;
					}

					// 1.2)
					// ����������λ��
					if (maxDicWordEnd < curSearchEnd) {
						maxDicWordEnd = curSearchEnd;
					}

					// 1.3)
					// ���´�����󳤶ȱ�����ֵ
					if (curSearchOffset == offset
							&& maxDicWordLength < curSearchLength) {
						maxDicWordLength = curSearchLength;
					}

					// 1.2)
					// ֪ͨcollector�����ҵ��Ĵ���
					Word word = curSearch.getWord();
					if (!word.isNoise()) {
						collector.collect(word.getText(), curSearchOffset,
								curSearchEnd);
					}
				}

				// ��isolatedFound==true����ʾ�ʵ�û�иô���
				boolean isolatedFound = curSearch.isUndefined();

				// ��isolatedFound==false����ͨ��Hit��next���Լ��Ӵʵ�û��beef�Ĵ�offset��curWordEnd
				// + 1λ�õĴ�
				// ����ж���ȫ��Ϊ�˼���һ�δʵ��������Ƶģ�
				// ���ȥ�����if�жϣ�����Ӱ��������ȷ��(���ǻ��һ�δʵ����)
				if (!isolatedFound && !curSearch.isHit()
						&& curSearch.getNext() != null) {
					isolatedFound = curSearchEnd >= limit
							|| beef.charAt(curSearchEnd) < curSearch.getNext()
									.charAt(curSearchLength);
				}
				// 2)
				// �ʻ����û�иô����û���Ըô��￪ͷ�Ĵʻ�...
				// -->�����¼Ϊ��������
				if (isolatedFound) {
					if (isolatedOffset < 0 && curSearchOffset >= maxDicWordEnd) {
						isolatedOffset = curSearchOffset;
					}
					break;
				}

				// ^^^^^^^^^^^^^^^^^^�������ص��жϽ��^^^^^^^^^^^^^^^^^^^^^^^^
			} // end of the second for loop
		} // end of the first for loop

		// ����ѭ���ִʽ���󣬿��ܴ������ļ���δ�ܴӴʵ�����ɴʵĹ����ַ�
		// ��ʱisolatedOffset��һ������һ����Чֵ(��Ϊ��Щ��������Ȼ���Ǵ�����Ǵʵ���ܴ�������Ϊ��ʼ�Ĵ��
		// ֻҪִ�е��˲���֪����Щ��Ȼ��ǰ׺���ַ��Ѿ�û�л���Ϊ������)
		// ���Բ���ͨ��isolatedOffset���ж��Ƿ��ʱ�����й����ʣ��ж�����ת��Ϊ��
		// ���һ���ʵ�ĴʵĽ���λ���Ƿ�С��offsetLimit(!!offsetLimit, not Limit!!)
		if (maxDicWordEnd < offsetLimit) {
			dissectIsolated(collector, beef, maxDicWordEnd, offsetLimit);
		}

		// ����������maxDicWordLength��ʱ����
		// ���θ���������ַ��ı�û����Ϊһ���ʱ��зֳ�(�����ʵ��дʺ͹������з�)��
		// �������shouldBeWord�����϶�ΪӦ����Ϊһ�����з֣������г���
		int len = limit - offset;
		if (len > 2 && len != maxDicWordLength
				&& shouldBeWord(beef, offset, limit)) {
			collector.collect(beef.subSequence(offset, limit).toString(),
					offset, limit);
		}

		// ����point��limit�����壬������һ��Knife��ʼ�дʵĿ�ʼλ��
		return point == -1 ? limit : point;
	}

	// -------------------------------------------------

	protected Hit searchNumber(CharSequence input, int offset, int count) {
		int endPos = -1;
		StringBuilder nums = new StringBuilder();
		for (int i = 0; i < count; i++) {
			char c = input.charAt(offset + i);
			if (CharSet.toNumber(c) < 0) {
				break;
			}
			nums.append(c);
			endPos = i;
		}
		//û������������
		if (endPos == -1) {
			return Hit.UNDEFINED;
		}
		//�������ֻ�û���������ܻ���
		if (endPos == count - 1) {
			return new Hit(Hit.UNCLOSED_INDEX, null, null);
		}
		//ֻ��һ���������֣���������ģ�������
		if (endPos == 0) {
			return Hit.UNDEFINED;
		}
		
		//���ֺ����������֣�ȡ��һ���ֳ���
		//trick: ����������index����ݸò������ĵĽ���λ��
		return new Hit(offset + endPos + 1, new Word(nums.toString()), null);
	}

	/**
	 * �Թ����ַ�ִ�
	 * 

	 */
	protected void dissectIsolated(Collector collector, Beef beef, int offset,
			int limit) {
		int curSearchOffset = offset;
		int binOffset = curSearchOffset; // ����һ���Ԫ�ִʵĿ�ʼλ��
		int tempEnd;

		while (curSearchOffset < limit) {
			// �����ַ�����Ǻ������֣�����"��ʮ����"��"ʮ����"��������
			tempEnd = collectNumber(collector, beef, curSearchOffset, limit,
					binOffset);
			if (tempEnd > curSearchOffset) {
				curSearchOffset = tempEnd;
				binOffset = tempEnd;
				continue;
			}

			// ħ���߼���
			// noiseWords�Ĵ�������ѧ����ȻҲ�Ǵʣ���CJKKnife�������ɴʻ���е���ʣ�
			// ��Щnoise�ʿ���û�г��ִʻ�?��ͻᱻ��Ϊ�����ַ��ڴ˴���(������Ϊ�ʻ㡢�����ж�Ԫ�ִ�)
			tempEnd = skipNoiseWords(collector, beef, curSearchOffset, limit,
					binOffset);
			if (tempEnd > curSearchOffset) {
				curSearchOffset = tempEnd;
				binOffset = tempEnd;
				continue;
			}

			// ���ǰ�ַ���noise���֣��䲻�μӶ�Ԫ�ִ�
			Hit curSearch = noiseCharactors.search(beef, curSearchOffset, 1);
			if (curSearch.isHit()) {
				binDissect(collector, beef, binOffset, curSearchOffset);
				binOffset = ++curSearchOffset;
				continue;
			}
			curSearchOffset++;
		}

		// 
		if (limit > binOffset) {
			binDissect(collector, beef, binOffset, limit);
		}
	}

	protected int collectNumber(Collector collector, Beef beef, int offset,
			int limit, int binOffset) {

		/*
		 * Fix : "�ٶ�ʮ��" => 1020
		 */
		
		// ��ǰ�����жϵ��ַ��λ��
		int curTail = offset;
		int number1 = -1;
		int number2 = -1;
		int bitValue = 0;
		int minUnit = 0;
		int number2Start = curTail;
		boolean hasDigit = false;// ���ã�ȥ��û������ֻ�е�λ�ĺ��֣��硰�򡱣���ǧ��
		for (; curTail < limit
				&& (bitValue = CharSet.toNumber(beef.charAt(curTail))) >= 0; curTail++) {
			// 
			if (bitValue == 2
					&& (beef.charAt(curTail) == '两'
							|| beef.charAt(curTail) == '倆' || beef
							.charAt(curTail) == '倆')) {
				if (curTail != offset) {
					break;
				}
			}
			// ���������ָ�λֵ�����֣�"��������" ->"3456"
			if (bitValue >= 0 && bitValue < 10) {
				hasDigit = true;
				if (number2 < 0){
					number2 = bitValue;
					number2Start = curTail;
				}
				else {
					number2 *= 10;
					number2 += bitValue;
				}
			} else {
				if (number2 < 0) {
					if (number1 < 0) {
						number1 = 1;
					} else {
						//"һ��ʮ" => "һ��" "ʮ"
						break;
					}
					if (bitValue >= minUnit) {
						if (minUnit == 0){
							number1 *= bitValue;
							minUnit = bitValue;
						} else {
							break;
                       }
					} else {
						minUnit = bitValue;
					}
				} else {
					if (number1 < 0) {
						number1 = 0;
					}
					if (bitValue >= minUnit) {
						if (minUnit == 0){
							number1 += number2;
							number1 *= bitValue;
							minUnit = bitValue;
						} else {
							//"һ�ٶ�ǧ" => "һ��" "��ǧ"
							curTail = number2Start;
							number2 = -1;
							break;
						}
					} else {
						minUnit = bitValue;
						number1 += number2 * bitValue;
					}
				}
				number2 = -1;
				number2Start = -1;
			}
		}
		if (!hasDigit) {
			return offset;
		}
		if (number2 > 0) {
			if (number1 < 0) {
				number1 = number2;
			} else {
				number1 += number2;
			}
		}
		if (number1 >= 0) {
			// ��Ԫ�ִ���
			if (offset > binOffset) {
				binDissect(collector, beef, binOffset, offset);
			}
			collector.collect(String.valueOf(number1), offset, curTail);
			
			if (units != null) {
				// ������ܸ��˼�����λ
				Hit wd = null;
				Hit wd2 = null;
				int i = curTail + 1;
				
				/*
				 * Fix issue 48: ���Ҽ�����λ����ĸ���Խ�����
				 */
				while (i <= limit && (wd = units.search(beef, curTail, i - curTail)).isHit()) {
					wd2 = wd;
					i ++;
					if (!wd.isUnclosed()) {
						break;
					}
				}
				i --;
				if (wd2 != null) {
					collector.collect(wd2.getWord().getText(), curTail, i);
					return i;
				}
			}
		}

		// �������һ���ж�ʧ���ַ�Ľ���λ�ã�
		// ��λ��Ҫô��offset��Ҫô��ʾcurTail֮ǰ���ַ�(������curTail�ַ�)�Ѿ�����Ϊ�Ǻ�������
		return curTail;
	}

	protected int skipNoiseWords(Collector collector, Beef beef, int offset,
			int end, int binOffset) {
		Hit word;
		for (int k = offset + 2; k <= end; k++) {
			word = noiseWords.search(beef, offset, k - offset);
			if (word.isHit()) {
				// ��Ԫ�ִ�
				if (binOffset > 0 && offset > binOffset) {
					binDissect(collector, beef, binOffset, offset);
					binOffset = -1;
				}
				offset = k;
			}
			if (word.isUndefined() || !word.isUnclosed()) {
				break;
			}
		}
		return offset;
	}

	protected void binDissect(Collector collector, Beef beef, int offset,
			int limit) {
		// ��Ԫ�ִ�֮���ԣ���W��X��Y��Z��ʾ�����ַ��е�4������
		// X ->X �����ֵĹ����ַ���Ϊһ����
		// XY ->XY ֻ�������ֵĹ����ַ���Ϊһ����
		// XYZ ->XY/YZ �����(>=3)�Ĺ����ַ�"�������"��Ϊһ����
		// WXYZ ->WX/XY/YZ ͬ��

		if (limit - offset == 1) {
			collector.collect(beef.subSequence(offset, limit).toString(),
					offset, limit);
		} else {
			// ���Ԫ�ִ�
			for (int curOffset = offset; curOffset < limit - 1; curOffset++) {
				collector.collect(beef.subSequence(curOffset, curOffset + 2)
						.toString(), curOffset, curOffset + 2);
			}
		}
	}

	protected boolean shouldBeWord(Beef beef, int offset, int end) {
		char prevChar = beef.charAt(offset - 1);
		char endChar = beef.charAt(end);
		// ���ĵ�˫���
		if (prevChar == '“' && endChar == '”') {
			return true;
		} else if (prevChar == '‘' && endChar == '’') {
			return true;
		}
		// Ӣ�ĵ�˫���
		else if (prevChar == '\'' && endChar == '\'') {
			return true;
		} else if (prevChar == '\"' && endChar == '\"') {
			return true;
		}
		// ���������
		else if (prevChar == '《' && endChar == '》') {
			return true;
		} else if (prevChar == '〈' && endChar == '〉') {
			return true;
		}
		// Ӣ�ļ�����
		else if (prevChar == '<' && endChar == '>') {
			return true;
		}
		return false;
	}

}
