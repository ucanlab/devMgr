package au.csie.ucanlab.mobilecloud;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class devMgr {

	// Log Convention : System.out.println("[devMgr][E] Message");
	
	public static void main(String[] args) {
 
		String configFile;
		InputStream is;
		Properties prop = new Properties();
		
		if(args.length == 0) {
			configFile = "./aumc.config";			
		} else {
			configFile = args[0].toString();
		}
				
		String root_dir;
		String node_dir;
		final String dest_path;   // = prop.getProperty("node_dir", "C:\\DEMO\\");
		final String nodes_file = "nodes.xml";  //= "nodes.xml";  // = "nodes.xml";
		final int listen_port;    // = Integer.parseInt(prop.getProperty("devmgr_port", "12345"));
		
		try {			
			is = new FileInputStream(configFile);
			
			try {
				prop.load(is);
				root_dir = prop.getProperty("root_dir");
				if(root_dir == null){
					System.out.println("[devMgr][E] root_dir is NOT set in config file(" + configFile + ")");
					System.exit(0);
				}
				
				node_dir = prop.getProperty("node_dir");
				if(node_dir == null){
					System.out.println("[devMgr][E] node_dir is NOT set in config file(" + configFile + ")");
					System.exit(0);
				}				
				
				dest_path = root_dir + node_dir;
				//nodes_file = prop.getProperty("nodes_file", "nodes.xml");
				listen_port = Integer.parseInt(prop.getProperty("devmgr_port", "12345"));	
				
				is.close();
				
				new Thread(){
					public void run(){
						try{
							new GetDevReq(dest_path, nodes_file, listen_port);
						} catch(Exception e){
							e.printStackTrace();
						}
					}
				}.start();	
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("[devMgr][E] Can not open config file(" + configFile + ")");
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("[devMgr][E] Can not find config file(" + configFile +")");
			e.printStackTrace();
		}
	}
} 

class GetDevReq implements Runnable{ 
	
	String destPath;// = "C:\\DEMO\\";
	String nodeFile;// = "node.xml";
	String nodesFile;// = "nodes.xml";
	int listenPort;// = 12345;
	ServerSocket Client = null;
	//int numberOfDev = 0;
	
	public GetDevReq(String path, String nodes, int port){
		
		this.destPath = path;
		this.nodeFile = "node.xml";
		this.nodesFile = nodes;
		this.listenPort = port;
		
		try {
			Client = new ServerSocket(this.listenPort);
			Thread t = new Thread(this);
			t.start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void run(){
		try {
			Socket socket;
			System.out.println("[devMgr] AUMC Device Manager Running ...");
			
			while(true) {
				
				//STEP 1. Receive device request
				socket = null;
				synchronized(Client){
					socket = Client.accept();
				}
			
				//收到行動裝置的狀態，建立node.xml紀錄該node的狀態
				//System.out.println("[devMgr] Rcv dev req from (" + socket + ")...");
				BufferedInputStream rxStream = new BufferedInputStream(socket.getInputStream());
				File clear_file = new File(this.destPath + this.nodeFile);
				clear_file.delete();
				RandomAccessFile file = new RandomAccessFile(this.destPath + this.nodeFile, "rw");

				int readByte;
				while((readByte = rxStream.read())!=-1){
					file.writeByte(readByte);
				}

				file.close();
				rxStream.close();
				socket.close();

				//透過XML Reader讀取node.xml (包含此節點的IP, PORT<補說明>, CPU, MEM...)
				SAXBuilder builder = new SAXBuilder();
				File xmlNodeFile = new File(this.destPath + this.nodeFile);
				Document document = (Document) builder.build(xmlNodeFile);
				Element rootNode = document.getRootElement();
				List list = rootNode.getChildren("node");
				Element node = (Element) list.get(0);
				//Attribute action is used to determine the node joining [join] or leaving [leave]
				String action = node.getAttributeValue("action");
				String ip = node.getChildText("ip");
				String port = node.getChildText("port");
				String cpu = node.getChildText("cpu");
				String mem = node.getChildText("mem");
				String bat = node.getChildText("bat");
				
				// STEP 2. Manipulate nodes.xml
				// 透過XMLWriter產生所有active節點的狀態清單，即nodes.xml
				File xmlFile = new File(this.destPath + this.nodesFile);
				if(xmlFile.exists()) {
					int index = isNodeExist(this.destPath + this.nodesFile, ip+":"+port);
					if(index > -1) { 
						if(action.equalsIgnoreCase("join")) {
							// node exist, modify it
							builder = new SAXBuilder();
							document = (Document) builder.build(xmlFile);
							rootNode = document.getRootElement();
							list = rootNode.getChildren("node");
							node = (Element) list.get(index);
							node.getChild("cpu").setText(cpu);
							node.getChild("mem").setText(mem);
							node.getChild("bat").setText(bat);
							XMLOutputter xmlOutput = new XMLOutputter();
							xmlOutput.setFormat(Format.getPrettyFormat());
							xmlOutput.output(document, new FileWriter(this.destPath + this.nodesFile));
							System.out.println("[devMgr] node [" + ip + ":" + port + "] with C:"+cpu+"; M:" + mem + "; B:" + bat + " joins the cloudlet.");
						} else if(action.equalsIgnoreCase("leave")){
							builder = new SAXBuilder();
							document = (Document) builder.build(xmlFile);
							rootNode = document.getRootElement();
							list = rootNode.getChildren("node");
							node = (Element) list.get(index);
							node.getParent().removeContent(node);
							XMLOutputter xmlOutput = new XMLOutputter();
							xmlOutput.setFormat(Format.getPrettyFormat());
							xmlOutput.output(document, new FileWriter(this.destPath + this.nodesFile));
							System.out.println("[devMgr] node [" + ip + ":" + port + "] leaves the cloudlet.");
						}
					} else {
						if(action.equalsIgnoreCase("join")) {
							// new node, add it
							builder = new SAXBuilder();
							document = (Document) builder.build(xmlFile);
							rootNode = document.getRootElement();
							node = new Element("node");
							node.setAttribute("network", ip+":"+port);
							node.addContent(new Element("cpu").setText(cpu));
							node.addContent(new Element("mem").setText(mem));
							node.addContent(new Element("bat").setText(bat));
							rootNode.addContent(node);
							XMLOutputter xmlOutput = new XMLOutputter();
							xmlOutput.setFormat(Format.getPrettyFormat());
							xmlOutput.output(document, new FileWriter(this.destPath + this.nodesFile));
							System.out.println("[devMgr] node [" + ip + ":" + port + "] with C:"+cpu+"; M:" + mem + "; B:" + bat + " joins the cloudlet.");
						} else if(action.equalsIgnoreCase("leave")){
							System.out.println("[devMgr] node [" + ip + ":" + "port" + "] did not join the cloudlet.");
						}
							
					}
				} else {
					if(action.equalsIgnoreCase("join")) {
						builder = new SAXBuilder();
						Element nodelist = new Element("nodelist");
						Document doc = new Document(nodelist);
						node = new Element("node");
						node.setAttribute("network", ip+":"+port);
						node.addContent(new Element("cpu").setText(cpu));
						node.addContent(new Element("mem").setText(mem));
						node.addContent(new Element("bat").setText(bat));
						doc.getRootElement().addContent(node);		 
						XMLOutputter xmlOutput = new XMLOutputter();
						xmlOutput.setFormat(Format.getPrettyFormat());
						xmlOutput.output(doc, new FileWriter(this.destPath + this.nodesFile));
						System.out.println("[devMgr] node [" + ip + ":" + port + "] with C:"+cpu+"; M:" + mem + "; B:" + bat + " joins the cloudlet.");
					} else if(action.equalsIgnoreCase("leave")) {
						System.out.println("[devMgr] node [" + ip + ":" + "port" + "] want to leave but nodes.xml does not exist.");
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//判斷此行動裝置的狀態是否已經被記錄在nodes.xml中
	private int isNodeExist(String file, String ip_port) {
		try {
			SAXBuilder builder = new SAXBuilder();
			File xmlFile = new File(file);
			Document document = (Document) builder.build(xmlFile);
			Element rootNode = document.getRootElement();
			List list = rootNode.getChildren("node");
			//System.out.println("size = "+list.size());
			for (int i = 0; i < list.size(); i++) {
				Element node = (Element) list.get(i);
				//System.out.println(node.getAttributeValue("network"));
				if(node.getAttributeValue("network").toString().compareTo(ip_port) == 0){
					return i;
				} 
			}
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
}