package org.dhp.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * 支持中文的字典树
 * @author zhangcb
 * @date   2016年6月28日
 * @email  chzcb2008@gmail.com
 *
 */
public class BigTrie {
	
	protected Vertex root = new Vertex();
	
	protected static class Vertex {
		protected int words;
		protected int prefixes;
		protected HashMap<Character,Vertex> edges;
		
		public Vertex() {
			this.words = 0;
			this.prefixes = 0;
			this.edges = new HashMap<>();
		}
	}
	
	public void addWord(String word) {
		StringContainer string = new StringContainer(word);
		addWord(root, string);
	}
	
	/**
	 * 文字一个个加入进去
	 * @param vertex
	 * @param word
	 */
	protected void addWord(Vertex vertex, StringContainer word) {
		//如果是最后一个
		if(word.remain() == 0) {
			vertex.words++;
		}
		else
		{
			vertex.prefixes++;
			Character c = word.charAt(0);
			Vertex v;
			if(vertex.edges.containsKey(c)) {
				v = vertex.edges.get(c);
			}
			else
			{
				v = new Vertex();
				vertex.edges.put(c, v);
			}
			word.next();
			addWord(v, word);
		}
	}
	
	public int countWords(String words) {
		return countWords(root, new StringContainer(words));
	}
	
	public boolean hasWords(String words) {
		return countWords(words)>0;
	}
	
	protected int countWords(Vertex vertex, StringContainer word) {
		if(word.remain() == 0) {
			return vertex.words;
		}
		
		Character c = word.charAt(0);
		if(vertex.edges.containsKey(c)) {
			word.next();
			return countWords(vertex.edges.get(c), word);
		}
		else
			return 0;
	}
	
	
	public String matchWord(String word) {
		return matchWord(word, false,false);
	}
	
	protected int index;
	
	/**
	 * 获得getMaxMatchWord结果时候，搜索到字符串的下标
	 * @return
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * 设置位置
	 */
	public void setIndex(int value) {
		this.index = value;
	}
	
	/**
	 * 输入一个单词得出最大匹配的单词
	 * @param word
	 * @return
	 */
	public String matchWord(String word, boolean skipBlank, boolean ignoreUpper) {
		StringContainer w = new StringContainer(word);
		Vertex vertex = root;
		StringBuffer s = new StringBuffer();
		String temp = "";
		for (;index<w.remain();index++) {
			Character c = w.charAt(index);
			//忽略空格
			if(skipBlank && c.equals(' ')) {
				continue;
			}
			if(ignoreUpper) {
				c = Character.toLowerCase(c);
			}
			if(!vertex.edges.containsKey(c)) {
				if(vertex.words != 0)
					return s.toString();
				else
					continue;
			}
			else
			{
				if(vertex.words != 0) {
					temp = s.toString();
				}
				s.append(c);
				vertex = vertex.edges.get(c);
			}
		}
		if(vertex.words == 0)
			return temp;
		return s.toString();
	}
	
	public String matchFirstWord(String word, boolean skipBlank, boolean ignoreUpper) {
		StringContainer w = new StringContainer(word);
		Vertex vertex = root;
		StringBuffer s = new StringBuffer();
		String temp = "";
		this.index = 0;
		for (;index<w.remain();index++) {
			Character c = w.charAt(index);
			//忽略空格
			if(skipBlank && c.equals(' ')) {
				continue;
			}
			if(ignoreUpper) {
				c = Character.toLowerCase(c);
			}
			if(!vertex.edges.containsKey(c)) {
				if(vertex.words != 0)
					return s.toString();
				else//一开始就不匹配就return
					return null;
			}
			else
			{
				if(vertex.words != 0) {
					temp = s.toString();
				}
				s.append(c);
				vertex = vertex.edges.get(c);
			}
		}
		if(vertex.words == 0)
			return temp;
		return s.toString();
	}
	
	/**
	 * 替换
	 * @param source
	 * @param replaceSet
	 * @param ignoreUpper 忽略大小写
	 * @return
	 */
	public Set<String> replace(StringContainer w, HashMap<String, String> replaceSet, boolean skipBlank, boolean ignoreUpper) {
		Set<String> result = new HashSet<>();
		Vertex vertex = root;
		StringBuffer s = new StringBuffer();
		String temp = "";
		String key;
		int index = 0;
		StringBuffer newSb = new StringBuffer();
		for (;index<w.remain();index++) {
			Character c = w.charAt(index);
			if(ignoreUpper) {
				c = Character.toLowerCase(c);
			}
			//忽略空格
			if(skipBlank && StringUtils.isBlank(c+"")) {
				newSb.append(c);
				continue;
			}
			if(!vertex.edges.containsKey(c)) {
				if(vertex.words != 0){
					key = s.toString();
					if(replaceSet.containsKey(key)) {
						newSb.append(replaceSet.get(key));
						result.add(key);
						//s就要重新开始计算
						s.delete(0, s.length());
					}
				}
				if(s.length()>0) {
					newSb.append(s);
					s.delete(0, s.length());
				}
				newSb.append(c);
				//不论当前是否可以找到匹配，就要重头匹配
				vertex = root;
			}
			else
			{
				if(vertex.words != 0) {
					temp = s.toString();
				}
				s.append(c);
				vertex = vertex.edges.get(c);
			}
		}
		if(vertex.words == 0)
		{
			key = temp;
			if(replaceSet.containsKey(key)) {
				newSb.append(replaceSet.get(key));
				result.add(key);
			}
		}
		if(s.length()>0) {
			newSb.append(s);
			s.delete(0, s.length());
		}
		w.changeBuffer(newSb);
		return result;
	}
	
	public static void main(String[] args) {
		BigTrie bigTrie = new BigTrie();
		bigTrie.addWord("dd");
		bigTrie.addWord("ddd");
		bigTrie.addWord("dddd");
		bigTrie.addWord("滴答滴答");
		
		System.out.println(bigTrie.countWords("d d"));
		System.out.println(bigTrie.countWords("ddd"));
		System.out.println(bigTrie.countWords("dddd"));
		
		System.out.println(bigTrie.matchWord("a           d d dadd滴答滴答",true,true));
		
		HashMap<String, String> replaceSet = new HashMap<>();
		replaceSet.put("dd", "佳佳");
		replaceSet.put("ddd", "美美美");
		replaceSet.put("dddd", "对对对对");
		
		StringContainer sc = new StringContainer("a           d d dadd滴答滴答");
		Set<String> ret = bigTrie.replace(sc, replaceSet, true, true);
		System.out.println(sc);
		System.out.println(ret);
	}
}
