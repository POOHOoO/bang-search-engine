package edu.upenn.cis.bang.indexer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.MessageDigest;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Node;
import rice.environment.Environment;
import rice.pastry.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

/**
 * A simple class for creating multiple Pastry nodes in the same
 * ring
 * 
 * 
 * @author Nick Taylor
 *
 */
public class NodeFactory {
	Environment env;
	NodeIdFactory nidFactory;
	SocketPastryNodeFactory factory;
	NodeHandle bootHandle;
	int createdCount = 0;
	int port;
	PastryIdFactory idfactory;
	
	NodeFactory(int port) {
		this(new Environment(), port);
	}	
	
	NodeFactory(int port, InetSocketAddress bootPort) {
		this(port);
		bootHandle = factory.getNodeHandle(bootPort);
	}
	
	NodeFactory(Environment env, int port) {
		this.env = env;
		this.port = port;
		nidFactory = new RandomNodeIdFactory(env);		
		try {
			factory = new SocketPastryNodeFactory(nidFactory, port, env);
		} catch (java.io.IOException ioe) {
			throw new RuntimeException(ioe.getMessage(), ioe);
		}
		
		idfactory = new PastryIdFactory(env);
	}
	
	public Node getNode() {
		try {
			synchronized (this) {
				if (bootHandle == null && createdCount > 0) {
					InetAddress localhost = InetAddress.getLocalHost();
					InetSocketAddress bootaddress = new InetSocketAddress(localhost, port);
					bootHandle = factory.getNodeHandle(bootaddress);
				}
			}
			
			PastryNode node =  factory.newNode(bootHandle);
			while (! node.isReady()) {
				Thread.sleep(100);
			}
			synchronized (this) {
				++createdCount;
			}
			return node;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public void shutdownNode(Node n) {
		((PastryNode) n).destroy();
		
	}
	
	public Id getIdFromString(String str) {
		return idfactory.buildIdFromToString(str);
	}
	
}
