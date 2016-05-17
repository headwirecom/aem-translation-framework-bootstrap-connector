package com.headwire.xliff.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SearchAndReplaceInputStream extends BufferedInputStream {

	private InputStream is;
	private char[] search;
	private char[] replace;

	private int len, pos, idx;
	private char ch, buf[];

	public SearchAndReplaceInputStream(BufferedInputStream is, String search, String replace) {
		super(is);
		
		this.is = is;
		this.search = search.toCharArray();
		this.replace = replace.toCharArray();

		len = this.search.length;
		pos = 0;
		idx = -1;

		ch = this.search[0];
		buf = new char[Math.max(this.search.length, this.replace.length)];
	}
	
		
	@Override
	public int read() throws IOException {
		if ( idx == -1 ) {
			idx = 0;

			int i = -1;
			while ( ( i = is.read() ) != -1 && ( buf[pos] = (char)i ) == ch ) {
				if ( ++pos == len ) {
					break;
				}

				ch = search[pos];
			}

			if ( pos == len ) {
				buf = new char[Math.max(this.search.length, this.replace.length)];
				System.arraycopy(replace, 0, buf, 0, replace.length);
			}

			pos = 0;
			ch = search[pos];
		}

		int toReturn = -1;
		if ( idx > -1 && buf[idx] != 0 ) {
			toReturn = buf[idx];
			buf[idx] = 0;
			if ( idx < buf.length - 1 && buf[idx + 1] != 0 ) {
				idx++;
			} else {
				idx = -1;
				buf = new char[Math.max(this.search.length, this.replace.length)];
			}
		}

		return toReturn;
	}
	
	public int available() {try {
		return is.available();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace(); return 0;
	}}
	public void close() {try {
		is.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}}
	public void mark(int readlimit) {is.mark(readlimit);}
	public boolean markSupported() {return is.markSupported();}
	public void reset() {try {
		is.reset();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}}
	public long skip(long n) {try {
		return is.skip(n);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace(); return 0;
	}}
} 
