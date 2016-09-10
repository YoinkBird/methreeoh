package concurrentTCP;
import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.*;

import tcp_udp.*;

/**
 * 
 * Example of a Concurrent Server using TCP. The server
 * accepts a client and serves the client in a separate 
 * thread, allowing it to continue to accept other clients
 * and serve them likewise. The service being provided to 
 * clients is a simple capitalization of string.
 * Run Server as:
 * java concurrentTCP.Server <server_port>
 * where server_port is the port at which the server is to be run 
 * 
 * @author rameshyerraballi
 * 
 */
class Server { 
  private String className;
  private Hashtable peerHash;
  private String ServerHostname;
  private InetAddress ServerIPAddress;
  private int ServerPort;
  private Hashtable<String, String> protocolStrings;
  private ServerSocket tcpSocket;
  private DatagramSocket udpSocket;
  private DataOutputStream outToClient;
  private Protocol protocol;

  public Server(String[] args){
    this.className = new Throwable().getStackTrace()[0].getClassName();
    // parse args
    if (args.length < 1) {
      System.out.println("[" + this.className + "][-E-]: Usage: java Server <port number>");

      System.exit(1);
    }

    //    only works in 'main'
//    this.ServerIPAddress = InetAddress.getByName(args[0]);
//    InetAddress ServerIPAddress = InetAddress.getByName(args[0]);
    this.ServerHostname = "fred";
    this.ServerPort = java.lang.Integer.parseInt(args[0]);

    // init
    this.peerHash = new Hashtable();
    this.protocol = new Protocol();
    this.protocolStrings = this.protocol.protocolStrings;
  }
  /**
   * @param args args[0] is the port number at which the server must be run
   */
  public static void main(String args[]) throws Exception 
  { 
    // <init_ritual>
    Server ServerInst = new Server(args);
    // </init_ritual>

    // set up sockets
    ServerInst.initSockets();

    while(true) { 
      // new client connection
      Socket connectionSocket = ServerInst.tcpSocket.accept(); 

      // validate client connection
      if(ServerInst.validateClient(connectionSocket)){
        // broadcast relevant information about client
        //if(ServerInst.broadcastClient(connectionSocket)){

          // accepted client connection
          Servant newServant = new Servant(connectionSocket);
          // try
          // BetterServant newServant = new BetterServant(connectionSocket);

      } 
    }
  }

  public void initSockets() throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    System.out.println( logPreAmble + "[-I-]: [INIT(server)|" 
        + "]");
    // TCP
    try {
      this.tcpSocket = new ServerSocket(this.ServerPort);
    } catch ( Exception e) {
    }
    System.out.println( logPreAmble + "[-I-]: [INIT(server)|tcp|" + /*this.ServerHostname +*/ ":" + this.ServerPort + "]");
    // UDP
    this.udpSocket = new DatagramSocket();
    int udpPort = this.udpSocket.getLocalPort();
    System.out.println( logPreAmble + "[-I-]: [INIT(server)|udp|"
        + udpPort
        + "]");
  }

  public boolean validateClient(Socket ClientTcpSocket) throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    String sentence;
    String clientInput;
    String clientResponse;

    boolean userIsValid = false;

    
    // send to client
    DataOutputStream  outToClient = 
      new DataOutputStream(ClientTcpSocket.getOutputStream());
    // read client input
    BufferedReader inFromClient =
      new BufferedReader(new InputStreamReader(ClientTcpSocket.getInputStream()));

    clientInput = inFromClient.readLine();
    System.out.println(logPreAmble + 
        "[-I-]: [Rx(server)|" + this.ServerHostname + ":" + this.ServerPort + "|" + clientInput + "]");

    // parse client input
    String[] inputArr = this.protocol.parseIncoming(clientInput);

    // parties: [Tx|tcp|client,server]
    // HELO¤<screen_name>¤<IP>¤<Port> \n
    if(inputArr[0].equals(this.protocolStrings.get("HELO"))){
      System.out.println(logPreAmble +
          "[-I-]: [Rx(server)|" + this.ServerHostname + ":" + this.ServerPort + "|"
          + "processing HELO"
          + "]");
      Hashtable userParam = this.protocol.parseHelo(inputArr[1]);
      // RJCT if user already present
      if( this.protocol.userHash.containsKey( userParam.get("user") )){
        clientResponse = "RJCT " + userParam.get("user") + "\n";
        System.out.println(logPreAmble +
            "[-I-]: [Tx(server)|" + this.ServerHostname + ":" + this.ServerPort + "|"
            + clientResponse
            + "]");
        outToClient.writeBytes(clientResponse); 
      }
      // JOIN,ACPT if user not present
      else{
        // broadcast
        String newUserName = (String) userParam.get("user");
        // send join string before adding user
        String joinMsg = this.protocol.createJoinMsg(userParam);
        this.sendUDPtoAllClients(ClientTcpSocket, joinMsg);
        // add user
        this.protocol.userHash.put(userParam.get("user"),userParam);
        // create acpt string
        String acptMsg = this.protocol.createAcptMsg();
        // send acpt string
        clientResponse = acptMsg + "\n";
        outToClient.writeBytes(clientResponse); 
        System.out.println(logPreAmble +
            "[-I-]: [Rx(server)|" + this.ServerHostname + ":" + this.ServerPort + "|"
            + joinMsg
            + "]");
        userIsValid = true;
      }
    }

    // print users
    //this.protocol.parseAccept(clientInput);
    System.out.println("-D-: list of current users:");
    this.protocol.printUserList();

    return userIsValid;
  }

  public boolean sendUDPtoAllClients(Socket ClientTcpSocket, String message) throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    String sentence;
    String clientInput;
    String clientResponse;

    boolean userIsValid = false;

    
    // send to client
    DataOutputStream  outToClient = 
      new DataOutputStream(ClientTcpSocket.getOutputStream());
    // read client input
    BufferedReader inFromClient =
      new BufferedReader(new InputStreamReader(ClientTcpSocket.getInputStream()));

    // send string
    byte[] sendData = new byte[1024];
    sendData = message.getBytes();
    // send
    {
      Enumeration userEnum = this.protocol.userHash.keys();
      while ( userEnum.hasMoreElements() ){
        String user = (String) userEnum.nextElement();
        Hashtable userInfo = (Hashtable) this.protocol.userHash.get(user);

        String todoSN = (String) userInfo.get("user");
        InetAddress todoIP = InetAddress.getByName((String) userInfo.get("host"));
        int todoPort = java.lang.Integer.parseInt((String) userInfo.get("port"));

        System.out.println( logPreAmble + "[-I-]: [Tx(peer)|udp|"
            + todoSN + "|" + todoIP + ":" + todoPort + "|"
            + message 
            + "]");

        DatagramPacket sendPacket =
          new DatagramPacket(sendData, sendData.length, todoIP, todoPort);
        //         new DatagramPacket(sendData, sendData.length, todoIP, 9876);
        System.out.println( logPreAmble + "[-I-]: UDP packet created");
        try{
          this.udpSocket.send(sendPacket);
        }catch (Exception e1){
          continue;
        }

        System.out.println( logPreAmble + "[-I-]: UDP packet sent");
      }
    }

    return userIsValid;
  }
  public boolean sendUDPtoClient(Socket ClientTcpSocket, String user, String message) throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    String sentence;
    String clientInput;
    String clientResponse;

    boolean userIsValid = false;

    
    // send to client
    DataOutputStream  outToClient = 
      new DataOutputStream(ClientTcpSocket.getOutputStream());
    // read client input
    BufferedReader inFromClient =
      new BufferedReader(new InputStreamReader(ClientTcpSocket.getInputStream()));

    // send string
    byte[] sendData = new byte[1024];
    sendData = message.getBytes();
    // send
    {
      //Enumeration userEnum = this.protocol.userHash.keys();
      //while ( userEnum.hasMoreElements() ){
        //String user = (String) userEnum.nextElement();
        Hashtable userInfo = (Hashtable) this.protocol.userHash.get(user);

        String todoSN = (String) userInfo.get("user");
        InetAddress todoIP = InetAddress.getByName((String) userInfo.get("host"));
        int todoPort = java.lang.Integer.parseInt((String) userInfo.get("port"));

        System.out.println( logPreAmble + "[-I-]: [Tx(peer)|udp|"
            + todoSN + "|" + todoIP + ":" + todoPort + "|"
            + message 
            + "]");

        DatagramPacket sendPacket =
          new DatagramPacket(sendData, sendData.length, todoIP, todoPort);
        //         new DatagramPacket(sendData, sendData.length, todoIP, 9876);
        System.out.println( logPreAmble + "[-I-]: UDP packet created");
        try{
          this.udpSocket.send(sendPacket);
        }catch (Exception e1){
          System.out.println(e1.toString());
          //continue;
        }

        System.out.println( logPreAmble + "[-I-]: UDP packet sent");
      //}
    }

    return userIsValid;
  }
} 

class Servant extends Thread
{
  private String clientSentence; 
  private String capitalizedSentence; 
  private Socket SocketToClient;

  public Servant (Socket sock)
  {
    SocketToClient = sock;
    start();
  }

  public void run()
  {
    try {
      BufferedReader inFromClient = 
        new BufferedReader(new  
            InputStreamReader(SocketToClient.getInputStream())); 

      DataOutputStream  outToClient = 
        new DataOutputStream(SocketToClient.getOutputStream());

      while ((clientSentence = inFromClient.readLine()) != null) {
        System.out.println("From Client on IP: " + SocketToClient.getInetAddress() 
            + " @port: " + SocketToClient.getPort() + " :\n\t" + clientSentence);
        capitalizedSentence = clientSentence.toUpperCase() + '\n'; 

        outToClient.writeBytes(capitalizedSentence); 
      }
    }
    catch (IOException e) {
      System.out.println("Socket problems");
    }
  }
}

class BetterServant implements Runnable 
{
  private String clientSentence; 
  private String capitalizedSentence; 
  private Socket SocketToClient;
  Thread myThread;

  public BetterServant (Socket sock)
  {
    SocketToClient = sock;
    myThread = new Thread(this);
    myThread.start();
  }
  public void run()
  {
    try {
      BufferedReader inFromClient = 
        new BufferedReader(new InputStreamReader(SocketToClient.getInputStream())); 

      DataOutputStream  outToClient = 
        new DataOutputStream(SocketToClient.getOutputStream());
      while ((clientSentence = inFromClient.readLine()) != null) {
        capitalizedSentence = clientSentence.toUpperCase() + '\n'; 	    
        outToClient.writeBytes(capitalizedSentence); 
      }
    }
    catch (IOException e) {
      System.out.println("Socket problems");
    }
  }
}

