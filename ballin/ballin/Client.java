package ballin;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import ballin.Ballin.ServerToClientObj;

public class Client{
	private Socket client;
	private ClientSender sender = null;
	private ClientReceiver receiver = null;
	
	private ClientListener clientListnr;
		
	public static interface ClientListener{
		public void receiveServerObject(Object obj);
		public void setClientId(int client_id);
	}
		
	private AtomicBoolean connectionCapped = new AtomicBoolean(false);
	
	public Client(String hostName, int port, ClientListener clientListnr) throws UnknownHostException, IOException {
		String serverName = hostName;
		this.clientListnr = clientListnr;
		
		client = new Socket(serverName, port);
		if(client.isConnected()){
    		sender = new ClientSender(new ObjectOutputStream(client.getOutputStream()), client);
    		receiver = new ClientReceiver(new ObjectInputStream(client.getInputStream()), client);
    		Thread thrd = new Thread(receiver);
    		thrd.setDaemon(true);
    		thrd.start();
		}else{
    		System.err.printf("Client.UnknownHostException: failed to connect to '%s' on port '%s'%n", 
    				hostName, port);
		}
 	}
	
	public void sendObjectToServer(Object obj){
		sender.send(obj);
	}
	
	private class ClientSender{
		private ObjectOutputStream obj_out;
		private Socket socket;
		private ClientSender(ObjectOutputStream obj_out, Socket socket){
			this.obj_out = obj_out;
			this.socket = socket;
		}
		private int exceptionCounter = 0;
		private void send(Object obj){
			if(!connectionCapped.get() && socket.isConnected()){
				try {
					obj_out.writeObject(obj);
				}catch(java.net.SocketException e){
					System.err.printf("ClientSender: java.net.SocketException%n");
					Client.this.close();
				} catch (IOException e) {
					System.err.printf("ClientSender: IOException%n");
					if(exceptionCounter++ > 10){
						Client.this.close();
					}
				}
			}
		}
		private void close(){
			try {
				if(obj_out != null){
					obj_out.close();
				}
			} catch (IOException e) {
				System.err.printf("ClientSender.close: IOException%n");
//				e.printStackTrace();
			} catch (NullPointerException e){
				System.err.printf("ClientSender.close: NullPointerException%n");
			}
		}
	}
	
	private class ClientReceiver implements Runnable{
		ObjectInputStream obj_in;
		Socket socket;
		private ClientReceiver(ObjectInputStream obj_in, Socket socket){
			this.obj_in = obj_in;
			this.socket = socket;
		}
		private int exceptionCounter = 10;
		@Override
		public void run(){
			try {
				while(!connectionCapped.get() && socket.isConnected()){
					Object obj = obj_in.readObject();
					if(clientListnr != null)
						clientListnr.receiveServerObject(obj);
				}
			}catch(java.net.SocketException e){
				 System.err.printf("ClientReceiver: inReadObject threw an SocketException -> seems like the connection was closed -> closing receiver%n");
				Client.this.close();
			}catch(EOFException e){
				 System.err.printf("ClientReceiver: inReadObject threw an EOFException -> seems like the connection was closed -> closing receiver%n");
				Client.this.close();
			}catch (ClassNotFoundException e) {
				 exceptionCounter++;
				 System.err.printf("ClientReceiver: inReadObject threw an ClassNotFoundException%n");
			}catch (IOException e){
				 exceptionCounter++;
				 System.err.printf("ClientReceiver: inReadObject threw an IOException%n");
			}finally{
				if(exceptionCounter > 10){
					Client.this.close();
				}
			}
		}
		private void close(){
			try {
				if(obj_in != null){
					obj_in.close();
				}
			} catch (IOException e) {
				 System.err.printf("ClientReceiver.close: inReadObject threw an IOException%n");
//				e.printStackTrace();
			}
		}
	}
	
	public void close(){
		System.out.println("closing client!");
		connectionCapped.set(true);
		clientListnr = null;
		if(receiver != null){
			receiver.close();
		}
		if(sender != null){
			sender.close();
		}
		try {
			if(client != null){
				client.close();
			}
		} catch (IOException e) {
			 System.err.printf("Client.close: inReadObject threw an IOException%n");
//			e.printStackTrace();
		}
	}
	
	
}
