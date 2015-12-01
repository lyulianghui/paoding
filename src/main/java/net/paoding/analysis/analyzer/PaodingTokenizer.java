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

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import net.paoding.analysis.analyzer.impl.MostWordsTokenCollector;
import net.paoding.analysis.knife.Beef;
import net.paoding.analysis.knife.Collector;
import net.paoding.analysis.knife.Knife;
import net.paoding.analysis.knife.Paoding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
//import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * <p>
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @see Beef
 * @see Knife
 * @see Paoding
 * @see Tokenizer
 * @see PaodingAnalyzer
 * 
 * @see Collector
 * @see TokenCollector
 * @see MostWordsTokenCollector
 * 
 * @since 1.0
 */
public final class PaodingTokenizer extends Tokenizer implements Collector {

	// -------------------------------------------------
	protected Log log = LogFactory.getLog(PaodingTokenizer.class);
	/**
	 */
	private int inputLength;

	/**
	 * 
	 */
	private static final int bufferLength = 128;

	/**
	 * 
	 *
	 */
	private final char[] buffer = new char[bufferLength];

	/**
	 * 
	 * @see #collect(String, int, int)
	 * @see #
	 */
	private int offset;

	/**
	 * 
	 */
	private final Beef beef = new Beef(buffer, 0, 0);

	/**
	 * 
	 */
	private int dissected;

	/**
	 * 
	 * @see #
	 */
	private Knife knife;

	/**
	 * 
	 */
	private TokenCollector tokenCollector;

	/**
	 * 
	 *
	 */
	private Iterator<Token> tokenIteractor;

	private CharTermAttribute termAtt;
	private OffsetAttribute offsetAtt;
	private TypeAttribute typeAtt;

	// -------------------------------------------------

	/**
	 * 
	 * @param input
	 * @param knife
	 * @param tokenCollector
	 */
	public PaodingTokenizer(Reader input, Knife knife,
			TokenCollector tokenCollector) {
		//super(input);
		this.input = input;
		this.knife = knife;
		this.tokenCollector = tokenCollector;	
		init();
	}

    /**
     * add by puye,to suit lucene 5.1
     * @param knife
     * @param tokenCollector
     */
    public PaodingTokenizer(Knife knife,TokenCollector tokenCollector){
        this.knife = knife;
        this.tokenCollector = tokenCollector;
        init();
    }

	private void init() {
		termAtt = addAttribute(CharTermAttribute.class);
		offsetAtt = addAttribute(OffsetAttribute.class);
		typeAtt   = addAttribute(TypeAttribute.class);
		offsetAtt.setOffset(0,0);
	}

	// -------------------------------------------------

	public TokenCollector getTokenCollector() {
		return tokenCollector;
	}

	public void setTokenCollector(TokenCollector tokenCollector) {
		this.tokenCollector = tokenCollector;
	}

	// -------------------------------------------------

	public void collect(String word, int offsetWord, int endWord) {
//		log.warn("collect.word.offsetbefore="+offset+",now="+(offset + offsetWord)
//				+",endoffsetbefore="+offset+",now="+(offset + endWord));
		tokenCollector.collect(word, offset + offsetWord, offset + endWord);
	}

	// -------------------------------------------------
	public int getInputLength() {
		return inputLength;
		
	}

	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		while (tokenIteractor == null || !tokenIteractor.hasNext()) {
			// System.out.println(dissected);
			int read = 0;
			int remainning = -1;
			if (dissected >= beef.length()) {
				remainning = 0;
			} else if (dissected < 0) {
				remainning = bufferLength + dissected;
			}
			if (remainning >= 0) {
				if (remainning > 0) {
					System.arraycopy(buffer, -dissected, buffer, 0, remainning);
				}
				read = input
						.read(buffer, remainning, bufferLength - remainning);
				inputLength += read;
				int charCount = remainning + read;
				if (charCount < 0) {
					return false;
				}
				if (charCount < bufferLength) {
					buffer[charCount++] = 0;
				}
				beef.set(0, charCount);
				offset += Math.abs(dissected);
				// offset -= remainning;
				dissected = 0;
			}
			dissected = knife.dissect((Collector) this, beef, dissected);
			// offset += read;// !!!
			tokenIteractor = tokenCollector.iterator();
		}
		Token token = tokenIteractor.next();
		//log.warn("token.startOffset="+token.startOffset() +",token.endoffset="+token.endOffset() );
		//termAtt.setTermBuffer(token.term());
		//termAtt.setEmpty();
		//termAtt.append(token);//v3.*
		//termAtt.copyBuffer(token.buffer(), 0, token.length()); // wrong for paoding V4.*
		termAtt.copyBuffer(token.buffer(), 0, token.buffer().length); //V4.*
		offsetAtt.setOffset(correctOffset(token.startOffset()),
				correctOffset(token.endOffset()));
		//log.warn("token="+token+",startOffset="+offsetAtt.startOffset() +",endOffset="+offsetAtt.endOffset() );
		typeAtt.setType("paoding");
		return true;
	}

	@Override
	public void reset() throws IOException {
		super.reset();		
		offset = 0;
		inputLength = 0;
		offsetAtt.setOffset(0, 0);	
		termAtt.setEmpty();
	}
	
	 @Override
	 public final void end() throws IOException {
	      // set final offset
		  super.end();
		  dissected = 0;
		  beef.set(0, 0); 
	      final int finalOffset = correctOffset(offset);
	      this.offsetAtt.setOffset(finalOffset, finalOffset);
	    }
}
