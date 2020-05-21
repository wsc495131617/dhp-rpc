package org.dhp.common.utils;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public class XMLUtils {
	
	public static Document load(String filename) throws IOException, SAXException, ParserConfigurationException {
		return load(new InputSource(filename));
	}
	
	public static Document load(InputStream inputStream) throws IOException, SAXException, ParserConfigurationException {
		return load(new InputSource(inputStream));
	}
	
	
	public static Document load(InputSource inputSource) throws IOException, SAXException, ParserConfigurationException {
		Document document;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.parse(inputSource);
		NodeList rootNodes = document.getChildNodes();
		return document;
	}
}
