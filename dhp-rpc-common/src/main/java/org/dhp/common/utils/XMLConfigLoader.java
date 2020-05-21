package org.dhp.common.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLConfigLoader {
	
	protected Map<String, Object> root = new HashMap<>();
	
	public XMLConfigLoader() {
		super();
	}
	
	public void load(String filename) {
		load(new InputSource(filename));
	}
	
	public void load(InputStream inputStream) {
		load(new InputSource(inputStream));
	}
	
	protected Document document;
	
	public void load(InputSource inputSource) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(inputSource);
			NodeList rootNodes = document.getChildNodes();
			Element docNode = document.getDocumentElement();
			int len = rootNodes.getLength();
			for(int i=0;i<len;i++) {
				addNodeList(root, root, rootNodes.item(i).getChildNodes());	
			}
			int attrLen = docNode.getAttributes().getLength();
			for(int j=0;j<attrLen;j++) {
				root.put(docNode.getAttributes().item(j).getNodeName(),docNode.getAttributes().item(j).getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void save(String fileName) {
		TransformerFactory transFactory= TransformerFactory.newInstance();
        try {
            Transformer transformer = transFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            DOMSource source=new DOMSource();
            source.setNode(document);
            StreamResult result=new StreamResult();
            result.setOutputStream(new FileOutputStream(fileName));
            
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }   
	}

	private void addNodeList(Map<String, Object> parent, Map<String, Object> currentNode, NodeList childNodes) {
		int len = childNodes.getLength();
		if(len==0) {
			return;
		}
		for(int i=0;i<len;i++) {
			Node node = childNodes.item(i);
			String nodeName = node.getNodeName();
			Map<String, Object> nodeMap = new HashMap<>();
			nodeMap.put("_node_", node);
			if(node.hasAttributes()) {
				int attrLen = node.getAttributes().getLength();
				for(int j=0;j<attrLen;j++) {
					nodeMap.put(node.getAttributes().item(j).getNodeName(),node.getAttributes().item(j).getNodeValue());
				}
			}
			if(node.hasChildNodes()) {
				addNodeList(currentNode,nodeMap, node.getChildNodes());
			}
			
			if(node.getNodeType() == Node.TEXT_NODE) {
				currentNode.put("value", node.getNodeValue().trim());
			}
			else
			{
				List<Map<String, Object>> childrens;
				if(!currentNode.containsKey(nodeName)) {
					childrens = new ArrayList<>();
					currentNode.put(nodeName, childrens);
				}
				else
				{
					childrens = (List<Map<String, Object>>) currentNode.get(nodeName);
				}
				childrens.add(nodeMap);	
			}
			
		}
	}
	
	public Map<String, Object> getRoot() {
		return root;
	}
	
	/**
	 * 逐个向下级属性访问
	 * @param properties
	 * @return
	 */
	public List<Map<String, Object>> getMapList(String... properties){
		List<Map<String, Object>> targets = new ArrayList<>();
		targets.add(root);
		for(String prop : properties) {
			Object value = null;
			List<Map<String, Object>> result = new ArrayList<>();
			for(Map<String, Object> target : targets) {
				value = target.get(prop);
				if(value == null){
					continue;
				}
				if(value instanceof String) {
					return null;
				}
				else if(value instanceof List) {
					result.addAll((List)value);
				}
				else
				{
					result.add((Map<String, Object>) value);
				}
			}
			if(value == null){
				return null;
			}
			targets = new ArrayList<>();
			targets.addAll(result);
		}
		return targets;
	}
	
	public Map<String, Object> getMap(String...properties) {
		List<Map<String, Object>> targets = getMapList(properties);
		if(targets!=null){
			return targets.get(0);	
		}
		return null;
	}
	
	public String getValue(String property) {
		return root.get(property).toString();
	}
	
}
