

import java.net.InetAddress;
import java.net.InetSocketAddress;

import rice.p2p.commonapi.Node;
import rice.environment.Environment;
import rice.pastry.NodeHandle;
import rice.p2p.commonapi.Id;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

//The NodeFactory class assigns ports in
//increasing order from the starting port, as required by this assignment.????

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

	NodeFactory(int port) {
		this(new Environment(), port);
	} 
	
	//port :   the local port to bind to 
	// bootPort = the IP:port of the node to boot from
	public NodeFactory(int port, InetSocketAddress bootPort) {
		this(port);
		bootHandle = factory.getNodeHandle(bootPort);
	}
	
	NodeFactory(Environment env, int port) {
		this.env = env;
		this.env.getParameters().setString("nat_search_policy", "never");
		this.env.getParameters().setInt("pastry_socket_writer_max_queue_length", 3000);
		this.port = port;
		nidFactory = new RandomNodeIdFactory(env);    
		try {
			factory = new SocketPastryNodeFactory(nidFactory, port, env);
		} catch (java.io.IOException ioe) {
			throw new RuntimeException(ioe.getMessage(), ioe);
		}

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

	public Id getIdFromBytes(byte[] material) {
		PastryIdFactory pIF = new PastryIdFactory(env);
		return pIF.buildId(material);
	}
}
