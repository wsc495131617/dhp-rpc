package org.dhp.common.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * 通过二进制的每位0和1来表示是否拥有某一个值
 * @author chzcb
 *
 */
public class BinarySet {
	protected byte[] data;
	
	public void set(int key,boolean value){
		int len = (key>>3);
		if(data == null || (data.length<=len && value)){//扩展data或者初始化data
			byte[] newdata = new byte[len+1];
			if(data != null)
				System.arraycopy(data, 0, newdata, 0, data.length);
			this.data = newdata;
		}
		int index = (key&7);
		byte v;
		if(value){
			v = (byte) (1<<index);
			data[len] = (byte) (data[len]^v);
		}
		else if(data.length>len)
		{
			v = (byte) ((1<<index)^-1);
			data[len] = (byte) (data[len]&v);
			int newlen = data.length;
			while(data[newlen-1] == 0){
				newlen--;
			}
			if(data.length!=newlen){
				byte[] newdata = new byte[newlen];
				System.arraycopy(data, 0, newdata, 0, newlen);
				this.data = newdata;
			}
		}
	}
	
	public boolean get(int key){
		int len = (key>>3);
		int index = (key&7);
		if(this.data.length<=len){
			return false;
		}
		return (data[len]&(1<<index))>0;
	}
	
	@Override
	public String toString() {
		String ret = "";
		for(byte b : data) {
			ret += StringUtils.leftPad(Integer.toBinaryString(b), 8, "0");
		}
		return ret;
	}
	
	public static void main(String[] args) {
		BinarySet e = new BinarySet();
		e.set(1, true);
		e.set(2, true);
		e.set(3, true);
		e.set(9, true);
		System.out.println(e);
		
		System.out.println(e.get(122));
		e.set(122, false);
		System.out.println(e.get(1));
		
		System.out.println(e.get(21));
		System.out.println(e.data.length);
	}

	public byte[] array() {
		return data;
	}
	
	public byte[] reverseArray() {
		int len = data.length;
		byte[] arr = new byte[len];
		for(int i=0;i<len;i++) {
			arr[i] = data[len-1-i];
		}
		return arr;
	}
}

