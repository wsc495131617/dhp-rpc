package org.dhp.common.utils;

public class StringContainer {
	protected StringBuffer buffer;
	protected int position;
	protected int totalLen;
	public StringContainer(String words) {
		this.buffer = new StringBuffer(words);
		this.totalLen = words.length();
	}
	public int remain() {
		return this.totalLen-position;
	}
	public int next() {
		return ++this.position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public int getPosition() {
		return this.position;
	}
	public int getTotalLength() {
		return this.totalLen;
	}
	public int indexOf(String str) {
		return this.buffer.indexOf(str,position);
	}
	public String subString(int endIndex) {
		if(endIndex == -1) {
			return this.buffer.substring(position);
		}
		return this.buffer.substring(position,endIndex);
	}
	public Character charAt(int index) {
		return this.buffer.charAt(position+index);
	}
	public void changeBuffer(StringBuffer buffer) {
		this.buffer = buffer;
	}
	public String toString() {
		return this.buffer.toString();
	}
}
