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

import java.math.BigInteger;

import net.paoding.analysis.dictionary.Dictionary;
import net.paoding.analysis.dictionary.Hit;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 */
public class NumberKnife extends CombinatoricsKnife implements DictionariesWare {

	private Dictionary units;
	
	public NumberKnife() {
	}

	public NumberKnife(Dictionaries dictionaries) {
		setDictionaries(dictionaries);
	}

	public void setDictionaries(Dictionaries dictionaries) {
		super.setDictionaries(dictionaries);
		units = dictionaries.getUnitsDictionary();
	}
	

	public int assignable(Beef beef, int offset, int index) {
		char ch = beef.charAt(index);
		if (CharSet.isArabianNumber(ch))
			return ASSIGNED;
		if (index > offset) {
			if (CharSet.isLantingLetter(ch) || ch == '.' || ch == '-' || ch == '_') {
				if (ch == '-' || ch == '_' || CharSet.isLantingLetter(ch)
						|| !CharSet.isArabianNumber(beef.charAt(index + 1))) {
					//�ִ�Ч��
					//123.456		->123.456/
					//123.abc.34	->123/123.abc.34/abc/34/	["abc"��"abc/34"ϵ��LetterKnife�ֳ�����NumberKnife]
					//û�л��ж�!CharSet.isArabianNumber(beef.charAt(index + 1))����ֳ�"123."�����"123"
					//123.abc.34	->123./123.abc.34/abc/34/
					return POINT;
				}
				return ASSIGNED;
			}
		}
		return LIMIT;
	}
	
	protected int collectLimit(Collector collector, Beef beef,
			int offset, int point, int limit, int dicWordVote) {
		// "123abc"��ֱ�ӵ���super��
		if (point != -1) {
			return super.collectLimit(collector, beef, offset, point, limit, dicWordVote);
		}
		// 
		// 2.2��
		//    ^=_point
		//     
		final int _point = limit;
		// ��ǰ�����жϵ��ַ��λ��
		int curTail = offset;
		/*
		 * Fix issue 56: �������ֽ����������
		 */				
		BigInteger number1 = BigInteger.valueOf(-1);
		BigInteger number2 = BigInteger.valueOf(-1);
		int bitValue = 0;
		int maxUnit = 0;
		//TODO:�������ظ���curTail(��ֵΪoffset)�жϣ����±����ж��Ƿ�Ϊ���֣�����һ���ظ�����
		//�����������������ķִ�����Ӱ��΢����΢��ʱ�Ȳ��Ż�
		for (; (bitValue = CharSet.toNumber(beef.charAt(curTail))) >= 0; curTail++) {
			// 
			if (bitValue == 2
					&& (beef.charAt(curTail) == '两' || beef.charAt(curTail) == '倆' || beef
							.charAt(curTail) == '俩')) {
				if (curTail != offset) {
					break;
				}
			}
			// ���������ָ�λֵ�����֣�"��������"	->"3456"
			if (bitValue >= 0 && bitValue < 10) {
				if (number2.compareTo(BigInteger.ZERO) < 0)
					number2 = BigInteger.valueOf(bitValue);
				else {
					number2 = number2.multiply(BigInteger.valueOf(10));
					number2 = number2.add(BigInteger.valueOf(bitValue));
				}
			} else {
				if (number2.compareTo(BigInteger.ZERO) < 0) {
					if (number1.compareTo(BigInteger.ZERO) < 0) {
						number1 = BigInteger.ONE;
					}
					number1 = number1.multiply(BigInteger.valueOf(bitValue));
				} else {
					if (number1.compareTo(BigInteger.ZERO) < 0) {
						number1 = BigInteger.ZERO;
					}
					if (bitValue >= maxUnit) {
						number1 = number1.add(number2);
						number1 = number1.multiply(BigInteger.valueOf(bitValue));
						maxUnit = bitValue;
					} else {
						number1 = number1.add(number2.multiply(BigInteger.valueOf(bitValue)));
					}
				}
				number2 = BigInteger.valueOf(-1);
			}
		}
		if (number2.compareTo(BigInteger.ZERO) > 0) {
			if (number1.compareTo(BigInteger.ZERO) < 0) {
				number1 = number2;
			} else {
				number1 = number1.add(number2);
			}
		}
		if (number1.compareTo(BigInteger.ZERO) >= 0 && curTail > _point) {
			doCollect(collector, String.valueOf(number1), beef, offset, curTail);
		}
		else {
			super.collectLimit(collector, beef, offset, point, limit, dicWordVote);
		}
		
		curTail = curTail > limit ? curTail : limit;
		
		//
		// ������ܸ��˼�����λ
		if (units != null && CharSet.isCjkUnifiedIdeographs(beef.charAt(curTail))) {
			Hit wd = null;
			Hit wd2 = null;
			int i = curTail + 1;
			
			/*
			 * Fix issue 48: ���Ҽ�����λ����ĸ���Խ�����
			 */
			while (i <= limit && (wd = units.search(beef, curTail, i - curTail)).isHit()) {
				wd2 = wd;
				i++;
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
		//
		
		return curTail > limit ? curTail : -1;
	}


}
