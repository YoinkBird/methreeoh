package tcp_udp;

import java.io.*;
import java.net.*;
import java.util.Hashtable;

/**
 *
 * Example of a TCP Client that sends a string to
 * a server, for the server to convert to upper-case. Reads
 * the string returned by the server and displays it on the
 * screen
 * Run Client as
 * java tcp.Client <server_host> <server_port>
 * where server_host is the host ip of the server
 * and server_port is the port at which the server is running
 * @author rameshyerraballi
 *
 */
public class Client {

  /**
   * @param args args[0] is the server's host ip and args[1] is its port number
   *
   */
  public static void main(String[] args) throws Exception{
    // class name for debug messages
    String className = new Throwable().getStackTrace()[0].getClassName();
    // TODO Auto-generated method stub
    String sentence;
    String modifiedSentence;

    // TCP
    Socket tcpSocket = null;
    // user input
    BufferedReader inFromUser =
      new BufferedReader(new InputStreamReader(System.in));

    // parse args
    if (args.length != 3) {
      System.out.println("[" + className + "][-E-]: Usage: java Client <host name> <port number> <screen_name>");

      System.exit(1);
    }

    // honestly not quite sure what the difference is
    String ServerHostname = args[0];
    InetAddress ServerIPAddress = InetAddress.getByName(args[0]);
    int ServerPort = java.lang.Integer.parseInt(args[1]);
    // screen_name
    String userName = args[2];
    
    // UDP - for datagram
    byte[] sendData = new byte[1024];
    byte[] receiveData = new byte[1024];

    System.out.println("[" + className + "][-I-]: will transmit to " + ServerHostname + ":" + ServerPort);
    try {
      tcpSocket = new Socket(ServerHostname, ServerPort);
    } catch ( Exception e) {

    } // end of try-catch
    int myPort = tcpSocket.getLocalPort();

    //	        Socket tcpSocket = new Socket("data.uta.edu", 6789);

    DataOutputStream outToServer =
      new DataOutputStream(tcpSocket.getOutputStream());

    BufferedReader inFromServer =
      new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

    Hashtable protocolStrings = new Hashtable();
    // HELO¤<screen_name>¤<IP>¤<Port>\n
    protocolStrings.put("HELO", "HELO " + userName + " " + ServerHostname + " " + myPort);
    // RJCT¤<screen_name>\n
    protocolStrings.put("RJCT", "RJCT " + userName);
    // parties: [Tx|tcp|client,server]
    // HELO¤<screen_name>¤<IP>¤<Port> \n
    sentence = protocolStrings.get("HELO") + "\n";
    System.out.println("[" + className + "][-I-]: [Tx(server)|" + ServerHostname + ":" + ServerPort + "|" + sentence + "]");

    outToServer.writeBytes(sentence + '\n');

    modifiedSentence = inFromServer.readLine();

    System.out.println("[" + className + "][-I-]: [Rx(server)|" + ServerHostname + ":" + ServerPort + "|" + modifiedSentence + "]");

    /*
    The server sends this message in response to the Greeting, to let the Chat Client know that the screen name is already in use.
    // parties: [Rx|tcp|server,client]
    RJCT¤<screen_name>\n
    */
    if ( modifiedSentence.equals(protocolStrings.get("RJCT")) ){
      System.out.println("bad username, exiting");
      System.exit(2);
    }


  }

}