package tcp_udp;

import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.*;

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
  private String className;
  private String userName;
  private Hashtable peerHash;
  private String ServerHostname;
  private InetAddress ServerIPAddress;
  private int ServerPort;
  private Hashtable<String, String> protocolStrings;
  private Socket tcpSocket;
  private DatagramSocket udpSocket;
  private DataOutputStream outToServer;

  public Client(String[] args){
    this.className = new Throwable().getStackTrace()[0].getClassName();
    // parse args
    if (args.length < 3) {
      System.out.println("[" + this.className + "][-E-]: Usage: java Client <host name> <port number> <screen_name>");

      System.exit(1);
    }

    // honestly not quite sure what the difference is
    this.ServerHostname = args[1];
    //    only works in 'main'
//    this.ServerIPAddress = InetAddress.getByName(args[0]);
//    InetAddress ServerIPAddress = InetAddress.getByName(args[0]);
    this.ServerPort = java.lang.Integer.parseInt(args[2]);
    // screen_name
    this.userName = args[0];

    // init
    this.peerHash = new Hashtable();
  }

  /**
   * @param args args[0] is the server's host ip and args[1] is its port number
   *
   */
  public static void main(String[] args) throws Exception{
    // <init ritual>
    Client thisClient = new Client(args);
    // cannot do this in the constructor for some reason
    thisClient.ServerIPAddress = InetAddress.getByName(args[0]);
    // </init ritual>
    // TODO Auto-generated method stub
    String message = "where is sauce";

    thisClient.connectToServer();
    thisClient.sendToPeer(message);
    thisClient.receiveFromPeer();
    thisClient.disconnectFromServer();
  }

  public void connectToServer() throws Exception{
    String className = this.className;
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    String sentence;
    String modifiedSentence;
//    // TCP
//    Socket tcpSocket = null;
    // user input
    BufferedReader inFromUser =
      new BufferedReader(new InputStreamReader(System.in));

    
    System.out.println("[" + className + "][-I-]: will transmit to " + this.ServerHostname + ":" + this.ServerPort);
    // TCP
    try {
      this.tcpSocket = new Socket(this.ServerHostname, this.ServerPort);
    } catch ( Exception e) {

    } // end of try-catch
    int myPort = this.tcpSocket.getLocalPort();
    // UDP
    this.udpSocket = new DatagramSocket();
    int udpPort = this.udpSocket.getLocalPort();

    //	        Socket tcpSocket = new Socket("data.uta.edu", 6789);
    this.outToServer =
      new DataOutputStream(this.tcpSocket.getOutputStream());

    BufferedReader inFromServer =
      new BufferedReader(new InputStreamReader(this.tcpSocket.getInputStream()));

    // src: http://stackoverflow.com/a/31550047
    // store the deterministic components of the protocol strings, e.g. username
    // when calling, add the dynamic components
    Hashtable<String, String> protocolStrings = new Hashtable<>();
    // HELO¤<screen_name>¤<IP>¤<Port>\n
    protocolStrings.put("HELO", "HELO " + this.userName + " " + tcpSocket.getLocalAddress().toString().replace("/","") + " " + udpPort);
    // RJCT¤<screen_name>\n
    protocolStrings.put("RJCT", "RJCT " + this.userName);
    // MESG¤<screen_name>:¤<message>\n
    protocolStrings.put("MESG", "MESG " + this.userName);
    // ACPT¤<SNn>¤<IPn>¤<PORTn>
    protocolStrings.put("ACPT", "ACPT");
    // EXIT\n
    protocolStrings.put("EXIT", "EXIT");
    // TODO: set up a method
    this.protocolStrings = protocolStrings;

    // parties: [Tx|tcp|client,server]
    // HELO¤<screen_name>¤<IP>¤<Port> \n
    sentence = this.protocolStrings.get("HELO");
    System.out.println(logPreAmble + "[-I-]: [Tx(server)|tcp|" + this.ServerHostname + ":" + this.ServerPort + "|" + sentence + "]");

    this.outToServer.writeBytes(sentence + '\n');

    modifiedSentence = inFromServer.readLine();

    System.out.println(logPreAmble + "[-I-]: [Rx(server)|tcp|" + this.ServerHostname + ":" + this.ServerPort + "|" + modifiedSentence + "]");

    /*
    The server sends this message in response to the Greeting, to let the Chat Client know that the screen name is already in use.
    // parties: [Rx|tcp|server,client]
    RJCT¤<screen_name>\n
    */
    if ( modifiedSentence.equals(this.protocolStrings.get("RJCT")) ){
      System.out.println("bad username, exiting");
      System.exit(2);
    }

    /*
    The server sends this message in response to the Greeting, to acknowledge the validity of the screen name
     and to inform the Chatter Client of the Identities of the ALL Chatters (including yourself).
    Each identity is separated by a “:”.
    // parties: [Rx|tcp|server,client]
    ACPT¤<SNn>¤<IPn>¤<PORTn>\n
    */
    if (! modifiedSentence.startsWith(this.protocolStrings.get("ACPT")) ){
      System.out.println("bad ACPT response, exiting");
      System.exit(2);
    }
    // parse reply
    this.parseAccept(modifiedSentence);
    System.out.println("-D-: printing out user array");
    this.printPeerList();
  }
  public void sendToPeer(String sentence) throws Exception{
    String className = this.className;
    String modifiedSentence;
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    // UDP section
    {
      // process response
      // TODO: remove hard-code

      /*
      This message is sent to the UDP ports of all members in the membership list, when a chat user types in a message (in the JTextField).
      */
      // parties: [Tx|udp|client,clients]
      // MESG¤<screen_name>:¤<message>\n
      // TODO: remove hard-code
      //sentence = inFromUser.readLine();
      // add timestamp for easierdebug
      // http://stackoverflow.com/a/6953926
      //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
      //sentence = "[" + timeStamp + "]";
      sentence = this.protocolStrings.get("MESG") + " " + sentence;
      sentence += "\n";
      byte[] sendData = new byte[1024];
      sendData = sentence.getBytes();

      // print out table/retrieve element/whatever
      {
        for (String[] peerDataArr : this.getPeerList()){
          String todoSN = peerDataArr[0];
          InetAddress todoIP = InetAddress.getByName(peerDataArr[1]);
          int todoPort = java.lang.Integer.parseInt(peerDataArr[2]);

          System.out.println( logPreAmble + "[-I-]: [Tx(peer)|udp|" + todoSN + "|" + todoIP + ":" + todoPort + "|" + sentence + "]");

          DatagramPacket sendPacket =
            new DatagramPacket(sendData, sendData.length, todoIP, todoPort);
          //         new DatagramPacket(sendData, sendData.length, todoIP, 9876);
          System.out.println( logPreAmble + "[-I-]: UDP packet created");
          this.udpSocket.send(sendPacket);

          System.out.println( logPreAmble + "[-I-]: UDP packet sent: "
              + this.udpSocket.getLocalPort() + "->" + todoPort);
        }
      }
    }
  }
  public String getUdp() throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    String sentence;
    byte[] receiveData = new byte[1024];

    DatagramPacket receivePacket =
      new DatagramPacket(receiveData, receiveData.length);

    System.out.println( logPreAmble + "[-I-]: waiting for reply on " + this.udpSocket.getLocalPort());
    try{
      this.udpSocket.receive(receivePacket);
    }
    catch (IOException localIOException) {}
    System.out.println( logPreAmble + "[-I-]: received reply " + this.udpSocket.getLocalPort());

    String response =
      new String(receivePacket.getData());

    //System.out.println( logPreAmble + "[-I-]: [Rx(peer)|udp|" + todoIP + ":" + todoPort + "|" + response + "]");
    System.out.println( logPreAmble + "[-I-]: [Rx(peer)|udp|" + this.ServerHostname + ":" + this.ServerPort + "|" + response + "]");
    return response;
  }
  public String receiveFromPeer() throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    String sentence;

    // TODO: for now, loop until receive new message
    boolean udpReceived = false;
    /*
       This is a message received on the UDP Socket.
       It is another Chatter’s chat message.
       Parse it and display the message in the JtextArea as shown in the GUI
    // parties: [Rx|udp|client,clients]
    MESG¤<screen_name>:¤<message>\n
    */
    String response = this.getUdp();
    System.out.println( logPreAmble + "[-I-]: [Rx(peer)|udp|" + this.ServerHostname + ":" + this.ServerPort + "|" + response + "]");
    String[] respArr = this.parseIncoming(response);
    String message = new String();
    if(respArr[0].equals("MESG")){
      udpReceived = true;
      // TODO: don't assume defined
      message = respArr[1];
    }
    return message;
  }
  public void disconnectFromServer() throws Exception{
    disconnectFromServerInit();
    // TODO: for now, loop until receive new message
    boolean udpReceived = false;
    while(! udpReceived){
      udpReceived = disconnectFromServerVerify();
    }
  }
  // initiate exit handshake
  public void disconnectFromServerInit() throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    String sentence;
    /*
    When the Chat Client wants to terminate (or exit) the chat it sends this to the Membership Server over TCP.
    The exit should take effect ONLY when the client clicks on the EXIT button provided in the GUI.
    The Client must read a response (see below) back from the server over the UDP Socket and then terminate.
    // parties: [Tx|tcp|client,server]
    EXIT\n
    */
    sentence = "EXIT\n";
    System.out.println( logPreAmble + "[-I-]: [Tx(server)|" + this.ServerHostname + ":" + this.ServerPort + "|" + sentence + "]");

    this.outToServer.writeBytes(sentence + '\n');
  }
  // if exit acknowledged, close ports and return true
  // otherwise return false
  public boolean disconnectFromServerVerify() throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";
    String sentence;

    /*
       This is a message received on the UDP Socket.
       It is from the Membership Server notifying the exit of a member from the chatroom.
       Parse it and display an appropriate message in the JtextArea (Elvis has left the Building); Remove from local list.
    // parties: [Rx|udp|server,client]
    EXIT¤<screen_name>\n
    */
    boolean udpReceived = false;
    String response = this.getUdp();
    System.out.println( logPreAmble + "[-I-]: [Rx(server)|udp|" + this.ServerHostname + ":" + this.ServerPort + "|" + response + "]");
    udpReceived = this.disconnectFromServerFinalise(response);

    return udpReceived;
  }
  // if exit acknowledged, close ports and return true
  // otherwise return false
  public boolean disconnectFromServerFinalise(String response) throws Exception{
    String methodName = new Throwable().getStackTrace()[0].getMethodName();
    String logPreAmble = "[" + className + "][" + methodName + "]";

    boolean udpReceived = false;
    String[] respArr = this.parseIncoming(response);
    if(respArr[0].equals("EXIT")){
      if(respArr[1].startsWith(this.userName)){
        udpReceived = true;
        // done
        this.tcpSocket.close();
        this.udpSocket.close();
      }
    }
    return udpReceived;
  }

  // parse incoming messages
  public String[] parseIncoming(String response) {
    String[] replyArr = new String[2];
    // remove newlines
    // response = response.replace("\n", "").replace("\r", "");
    // reply format: <keyword>¤<content>
    // extract,remove keyword
    String type = response.substring(0,4);
    replyArr[0] = type;
    String content = response.substring(5,response.length());
    if(content != null){
      replyArr[1] = content;
    }
    return replyArr;
  }
  // parse ACPT reply
  public ArrayList<String[]> parseAccept(String response) {
    ArrayList<String[]> peerArr = new ArrayList<String[]>();
    // reference
    Hashtable peerHashTmp = new Hashtable();
    //System.out.println("-D-: parsing ACPT reply");
    // reply format: ACPT¤<SNn>¤<IPn>¤<PORTn>:<SNn+1>¤<IPn+1>¤<PORTn+1>:
    // extract,remove keyword
    // then split on ':'
    // then split on ' '
    String type = response.substring(0,4);
    String sequence = response.substring(5,response.length());
    String[] replyArr1 = this.parseIncoming(response);
    type = replyArr1[0];
    sequence = replyArr1[1];
    //System.out.println("-D-:" + type + "|" + sequence);
    //System.out.println("-D-: ACPT List");
    String[] replyArr = sequence.split("[:]");
    for (String iter: replyArr) {
      //System.out.println(iter);
      String[] peerInfo = iter.split("[\\s]");
      // skip self
      if(peerInfo[0].equals(this.userName)){
        continue;
      }
      peerArr.add(peerInfo);
      peerHashTmp.put(peerInfo[0],peerInfo);
      System.out.print("[");
      for ( String val : peerInfo ){
        System.out.print(val);
        System.out.print("|");
      }
      System.out.print("]");
      System.out.println();
    }
    this.peerHash.putAll(peerHashTmp);
    return peerArr;
  }

  public String getUserName(){
    return this.userName;
  }
  public ArrayList<String[]> getPeerList(){
    // TODO: remove self
    ArrayList<String[]> values = new ArrayList<String[]>(this.peerHash.values());
    return values;
  }
  public void printPeerList(){
    // print out table/retrieve element/whatever
    {
      for (String[] peerDataArr : this.getPeerList()){
        for (String data : peerDataArr){
          System.out.print(data + "|");
        }
        System.out.println();
      }
    }
  }
  public void removePeer(String removeUserName){
    this.peerHash.remove(removeUserName);
  }

}
