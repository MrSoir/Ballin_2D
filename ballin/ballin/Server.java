package ballin;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server{
	
	private ServerSocket serverSocket;
	private List<Socket> sockets = new ArrayList<>();
	private List<ServerSender> serverSndrs = new ArrayList<>();
	private List<ServerReceiver> serverRcvrs = new ArrayList<>();
	private ServerListener serverListnr;
	
	private int clientCounter = 1; // der server selbst ist spieler mit der id 0, clients beginnen daher ab id 1
		
	private AtomicBoolean serverShutDown = new AtomicBoolean(false);
	
	public static interface ServerListener{
		public void receiveClientObject(Object obj, int clientId);
		public void clientAdded(int clientId);
		public void clientRemoved(int clientId);
	}

//	public static class ServerObject implements Serializable{
//		private static final long serialVersionUID = -6335303253594184936L;
//		double value;
//		String name;
//	}
	
	public Server(int port, ServerListener serverListnr) throws IOException{
		serverSocket = new ServerSocket(port);
		this.serverListnr = serverListnr;
		
		Thread thrd = new Thread(new ForClientListener());
		thrd.setDaemon(true);
		thrd.start();
	}
	public void sendObjectsToClients(Object obj){
		for(ServerSender srvrSndr: serverSndrs){
			srvrSndr.send(obj);
		}
	}
	
	private class ForClientListener implements Runnable{
		@Override
		public void run() {
			while(!serverShutDown.get()){
				try {
					System.out.println("Server: waiting for new client...");
					Socket socket = serverSocket.accept();
					if(!serverShutDown.get()){
						System.out.println("Server: new client accepted");
						
						final int clientId = clientCounter++;
						
						sockets.add(socket);
	
						serverSndrs.add(new ServerSender(socket, clientId));
											
						ServerReceiver receiver = new ServerReceiver(socket, clientId);
						serverRcvrs.add(receiver);
						Thread thrd = new Thread(receiver);
						thrd.setDaemon(true);
						thrd.start();
						
						if(serverListnr != null){
							serverListnr.clientAdded(clientId);
						}
					}
				}
				catch (SocketException e){
					System.err.println("Server.ForClientListener.run: threw SocketException! - (possibly becaus ServerSocket was already closed)");
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static class ClientIdServerObj implements Serializable{
		private static final long serialVersionUID = 3191567467795951558L;
		private final int client_id;
		public ClientIdServerObj(final int client_id){
			this.client_id = client_id;
		}
		public int getClientId(){return client_id;}
	}
	private class ServerSender{
		private Socket socket;
		private ObjectOutputStream obj_out;
		private final int clientId;
		private boolean clientIsDead = false;
		private int exceptionCounter = 0;

		private ServerSender(Socket s, int clientId){
			this.socket = s;
			this.clientId = clientId;
			try {
				obj_out= new ObjectOutputStream(socket.getOutputStream());
				final int id = clientId;
				send(new ClientIdServerObj(id));// dem klienten gleich mal mitteilen, welche id ihm zugewiesen wird
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		private void send(Object obj){
			if(!clientIsDead && socket.isConnected()){
				try {
					obj_out.writeObject(obj);
				}catch(java.net.SocketException e){
					System.err.printf("ServerSender.send: java.net.SocketException%n");
					close();
				} catch (IOException e) {
					System.err.printf("ServerSender.send: IOException%n");
					if(exceptionCounter++ > 10){
						close();
					}
				}
			}
		}
		private void close(){
			try {
				obj_out.close();
			}catch(java.net.SocketException e){
				System.err.printf("ServerSender.close: java.net.SocketException%n");
			}catch(IOException e) {
				e.printStackTrace();
			}finally{
				clientIsDead = true;
				if(serverListnr != null){
					serverListnr.clientRemoved(clientId);
				}
			}
		}
	}
	private class ServerReceiver implements Runnable{
		private Socket socket;
		private ObjectInputStream obj_in;
		private final int clientId;
		ServerReceiver(Socket s, int clientId){
			this.socket = s;
			this.clientId = clientId;
			try {
				obj_in = new ObjectInputStream(s.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		int exceptionCounter = 0;
		@Override
		 public void run() {
			 try {
				Object obj;
				while(!serverShutDown.get() && socket.isConnected()
						&& serverListnr != null
						&& (obj = obj_in.readObject()) != null){
//					ServerObject servObj = (ServerObject)obj;
					serverListnr.receiveClientObject(obj, clientId);
				}
			 }catch(java.net.SocketException e){
				 System.err.printf("ServerReceiver: inReadObject threw a SocketException -> seems like the connection was closed -> closing receiver%n");
					close();
			 }catch(EOFException e){
				 System.err.printf("ServerReceiver: inReadObject threw an EOFException -> seems like the connection was closed -> closing receiver%n");
					close();
			 }catch(ClassNotFoundException e){
				 System.err.printf("ServerReceiver: inReadObject threw a ClassNotFoundException%n");
				 exceptionCounter++;
			 }catch(IOException e) {
				 System.err.printf("ServerReceiver: inReadObject threw an IOException%n");
				 exceptionCounter++;
	 		 }
			 finally{
				 if(exceptionCounter > 10){
						close();
				 }
			 }
		}
		private void close(){
			if(serverListnr != null)
				serverListnr.clientRemoved(clientId);
			try {
				obj_in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void close(){
		System.out.println("closing server!");
		serverShutDown.set(true);
		serverListnr = null;
		try{
			serverSocket.close();
		}catch(IOException e){
			System.err.println("Server.close: threw IOException on closing ServerSocket!");
		}
		for(ServerReceiver receiver: serverRcvrs){
			receiver.close();
		}
		for(ServerSender sender: serverSndrs){
			sender.close();
		}
		for(Socket socket: sockets){
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
