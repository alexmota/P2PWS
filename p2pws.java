/*
 * @arthur Alexio Mota
 * @arthur Hua Yang
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Object;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class p2pws implements Runnable{

    // global hash table to store the file
	static Hashtable<String, String> files;
	Socket conn;
    int current_port;

    // constructor
	p2pws(Socket sock, int socket){
		this.conn = sock;
		//this.files = new Hashtable<String, String>();
	    this.current_port = socket;
    }

	public static void main(String[] args){

		int port;
        files = new Hashtable<String, String>();
        if(args.length != 1){
			System.out.println("Invalid number of arguments");
			System.out.println("Format:");
            System.out.println("java p2pws [port number]");
            return;
		}

		try{
			port = Integer.parseInt(args[0]);
			System.out.println("Port: " + port);
		}
		catch(NumberFormatException nfe){
			System.out.println(nfe);
			return;
		}

		try{
			//Create server socket on provided port
			ServerSocket server_sock = new ServerSocket(port);
			for(;;){
				Socket conn = server_sock.accept();
				System.out.println("Connection Established");

				//create thread for each new connection
				new Thread(new p2pws(conn, port)).start();
			}
		}
		catch(IOException e){
			System.out.println(e);
			return;
		}

	}

	public void run(){
		try {
			// input and output to client
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			DataOutputStream toClient = new DataOutputStream(conn.getOutputStream());
			// stack to store the client inputs
			String line;
			// boolean for closing the connectiong
			boolean quit = false;
			// continues to read client inputs till 'end' to end the connection
			while ((line = fromClient.readLine()) != null) {
				//line = line.replace("\n", ""); //get rid of newline chars
				String tokens[] = line.split(" ");
				//Check to see if the line contains a valid request
				if((tokens[0].equals("PUT") || 
					tokens[0].equals("DELETE") ||
					tokens[0].equals("GET")) && tokens.length == 3){
					
                    HTTP_Request(tokens, fromClient, toClient);
                    
                    if (tokens[0].equals("GET")) {
                        break;
                    }
                }
			}
			System.out.println("Closing the connection.");
            fromClient.close();
            toClient.close();
            conn.close();
		} catch (IOException e) {
			System.out.println(e);
		}

	}

	//Evaluates received request from client/peer
	public void HTTP_Request(String[] input, BufferedReader fromClient, DataOutputStream toClient) {

		switch(input[0]){
			case "PUT":

				try{
					wsPUT(input, fromClient, toClient);
				}
				catch(IOException e){
					System.out.println(e);
					return;
				}
                break;
			case "GET":
                try {
                    functions.HTTP_Get(input[1], toClient, current_port, files);
                } catch (Exception e) {
                    System.out.println(e);
                    return;
			    }
                break;
            case "DELETE":
				wsDELETE(input, fromClient, toClient);
				break;
			default:
				System.out.println("Incorrect Input");
				break;
		}
	}

	public String response(int cmd) {

		switch(cmd){
			case 1: //if Content not found
				return "HTTP/1.1 404 Not Found\nContent-Length: 0";
			default: //request was a success
				return "HTTP/1.1 200 OK \nContent-Length: 0";
		}
	}

	public void wsPUT(String[] input, BufferedReader fromClient, DataOutputStream toClient) throws IOException {
		//removes content-length line
		System.out.println("In put!");
		String line = fromClient.readLine();
		String size[] = line.split(" ");
		int limit = Integer.parseInt(size[1]); //Get the Content-Length: ?

		String content = "";
		while (limit > 0) { //Loop will run until the Content-length is 0 or less

			line = fromClient.readLine();
			limit-=line.length();
			limit--;
			content+=line;
		}

		String key = md5Hash(input[1]);
		if(!files.contains(key)){
			files.put(key, content);
		}
        toClient.writeBytes(response(0));
    }	

	public void wsDELETE(String[] input, BufferedReader fromClient, DataOutputStream toClient) {
		//Remove file content from hash map
		try{
			System.out.println("HERE: "+ input[1] + " " + files.get(md5Hash(input[1])));
			if(files.remove(md5Hash(input[1])) == null){
				toClient.writeBytes(response(1));
			}
			toClient.writeBytes(response(0));
			System.out.println("Check: "+ input[1] + " " + files.get(md5Hash(input[1])));
		}
		catch(IOException e){
			System.out.println(e);
		}
	}

	public static String md5Hash(String input) {
         
        String md5 = null;
        if(null == input) return null;
         
        try {
	        //Create MessageDigest object for MD5
	        MessageDigest hash = MessageDigest.getInstance("MD5");
	         
	        //Update input string in message digest
	        hash.update(input.getBytes(), 0, input.length());
	
	        //Converts message digest value in base 16 (hex)
	        md5 = new BigInteger(1, hash.digest()).toString(16);
        } 
        catch (NoSuchAlgorithmException e) {

            e.printStackTrace();
        }
        return md5;
    }
}
