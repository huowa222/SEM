package plant.networking;

import java.io.*;
import plant.*;
import java.net.*;
import plant.physical.*;

/**
 * Class that maintains and handles a client connection
 * 
 * @author Koen van Urk and Anton Timmermans
 */
public class ClientHandler extends Thread {

	private Socket socket;
	private Server server;
	private BufferedReader in;
	private BufferedWriter out;

	private User user;
	private String username;
	
	private int productRunStep = 0;
	private int haltStep = 0;
	private int resumeStep = 0;
	private int addAssemblyLineStep = 0;
	private int addStorageBinStep = 0;
	private int robotStep = 0;
	
	private String productRunProduct = null;
	private int productRunAssemblyLineIdentifier = 0;
	
	private String storageBinResource = null;
	private int storageBinCapacity = 0;
	private int storageBinAssemblyLine = 0;
	
	private String robotResource = null;
	private int robotIdentifier = 0;
	
	
	/**
	 * Constructor for a client
	 * 
	 * @param server The server that this client belongs to
	 * @param socket The socket for this client
	 */
	public ClientHandler(Server server, Socket socket) {
		this.server = server;
		this.socket = socket;
		this.user = new User();
		try {
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			shutDown();
		}
		
		try {
			this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		} catch (IOException e) {
			shutDown();
		}
	}
	
	/**
	 * Method that handles receiving and processing input
	 */
	public void run() {
		
		if(socket != null) {
			deliver("Please enter your username:");
		}
		
		// As long as the socket still exists we keep listening for input
    	while(socket != null) {
	
   			String input = null;
   			try {
   				input = in.readLine();
   			} catch(IOException e) {
   				System.out.println("Connection closed.");
   				shutDown();
   			}
    		
   			// If the inputstream is valid this value will not be null, otherwise the input stream is invalid
   			// and the client has disconnected
        	if(input != null) {	
        		
        		if(!user.isLoggedIn()) {
        			if(this.username == null) {
        				this.username = input;
        				deliver("Please enter your password:");
        			}else{
        				if(!user.login(username, input)) {
        					deliver("Incorrect credentials. Please try again.");
        					deliver("Please enter your username:");
        					this.username = null;
        				}else{
        					displayMenu();
        				}
        			}
        		}else{        		
        			// M for Menu
        			if(productRunStep != 0) {
        				this.handleP(input);
        			}else if(haltStep != 0) {
        				this.handleH(input);
        			}else if(resumeStep != 0) {
        				this.handleR(input);
        			}else if(addAssemblyLineStep != 0) {
        				this.handleA(input);
        			}else if(addStorageBinStep != 0) {
        				this.handleB(input);
        			}else if(robotStep != 0) {
        				this.handleT(input);
        				
        				
        			}else if(input.equals("S")) {
        				this.handleS(input);
        			}else if(input.equals("P")) {
        				this.handleP(input);
        			}else if(input.equals("H")) {
        				this.handleH(input);
        			}else if(input.equals("R")) {
        				this.handleR(input);
        			}else if(input.equals("A")) {
        				this.handleA(input);
        			}else if(input.equals("B")) {
        				this.handleB(input);
        			}else if(input.equals("T")) {
        				this.handleT(input);
        			}else if(input.equals("E")) {
        				shutDown();
        			}else{
        				displayMenu();
        			}
        		}
        		
        	}else{
        		System.out.println("Client has ended the connection.");
        		shutDown();
        	}
    	}
    } 
	
	private void shutDown() {
		server.removeClient(this);
		
		try {
			in.close();
			out.close();
			socket.close();
			socket = null;
		} catch(IOException e) {
			System.out.println("Fout tijdens afsluiten van verbinding.");
		}
	}
	
	/**
	 * Method to send a message to the client through the socket
	 * @param message The message to send
	 */
	public void deliver(String message) {
		try {
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			shutDown();
		}
	}
	
	
// -------------------------------------------------------------------------------
// SERVER COMMAND HANDLING
// -------------------------------------------------------------------------------
	
	private void resetSteps() {
		productRunStep = 0;
		haltStep = 0;
		resumeStep = 0;
		addAssemblyLineStep = 0;
		addStorageBinStep = 0;
		robotStep = 0;
	}
	
	private void displayMenu() {
		this.resetSteps();
		
		String help = "=========================================\n";
		help = help + "========= MENU ==========================\n";
		help = help + "=========================================\n";
		help = help + "S			Shows factory status\n";
		help = help + "P			Schedule a product run\n";
		help = help + "R			Resume an assembly line\n";
		help = help + "H			Halt an assembly line\n";
		help = help + "A			Add an assembly line\n";
		help = help + "B			Add a storage bin\n";
		help = help + "T			Add a robot\n";
		help = help + "E			Exit\n";
		help = help + "\nPick a menu item";
		
		deliver(help);
	}
	
	private void handleS(String input) {
		deliver(server.getFactory().getStatus());
		this.displayMenu();
	}
	
	private void handleP(String input) {
		if(productRunStep == 0) {
			deliver("Please enter the name of the product:");
			productRunStep = 1;
		}else if(productRunStep == 1) {
			if(input.equals("Bear") || input.equals("Bunny") || input.equals("Squirrel")) {
				productRunProduct = "plant.physical.product." + input;
				productRunStep = 2;
				deliver("Please enter the identifier of the assembly line to run this product:");
			}else{
				deliver("Invalid product, please try again.");
			}
		}else if(productRunStep == 2) {
			try {
				productRunAssemblyLineIdentifier = Integer.parseInt(input);
				productRunStep = 3;
				deliver("Please enter a production goal:");
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
		}else{
			try {
				int productionGoal = Integer.parseInt(input);
				
				try {
					
					// This has already been checked when comparing the input to available Products
					@SuppressWarnings("unchecked")
					Class<? extends Product> theClass = (Class<? extends Product>) Class.forName(productRunProduct);
					
					server.getFactory().scheduleProductRunForAssemblyLine(productRunAssemblyLineIdentifier, theClass, productionGoal);
					deliver("Product run scheduled.");
					this.displayMenu();
				} catch (ClassNotFoundException e) {
					deliver("Dammit. We screwed up!");
				}
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
		}
	}
	
	private void handleH(String input) {
		if(haltStep == 0) {
			deliver("Please enter a numeric identifier for the assembly line:");
			haltStep = 1;
		}else{
			
			try {
				int identifier = Integer.parseInt(input);
				server.getFactory().haltAssemblyLine(identifier);
				this.displayMenu();
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
			
		}
	}
	
	private void handleR(String input) {
		if(resumeStep == 0) {
			deliver("Please enter a numeric identifier for the assembly line:");
			resumeStep = 1;
		}else{
			
			try {
				int identifier = Integer.parseInt(input);
				server.getFactory().resumeAssemblyLine(identifier);
				this.displayMenu();
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
			
		}
	}
	
	private void handleA(String input) {
		if(addAssemblyLineStep == 0) {
			deliver("Please enter a numeric identifier for the assembly line:");
			addAssemblyLineStep = 1;
		}else{
			
			try {
				int identifier = Integer.parseInt(input);
				server.getFactory().addAssemblyLine(new AssemblyLine(identifier));
				this.displayMenu();
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
			
		}
	}
	
	private void handleB(String input) {
		if(addStorageBinStep == 0) {
			deliver("Please enter a storage capacity:");
			addStorageBinStep = 1;
		}else if(addStorageBinStep == 1) {
			try {
				storageBinCapacity = Integer.parseInt(input);
				addStorageBinStep = 2;
				deliver("Please enter the name of the resource:");
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
		}else if(addStorageBinStep == 2) {
			if(input.equals("Button") || input.equals("Eye") || input.equals("Filling") || input.equals("Skin")) {
				storageBinResource = "plant.physical.resource." + input;
				addStorageBinStep = 3;
				deliver("Please enter the identifier of the assembly line to assign this bin:");
			}else{
				deliver("Invalid resource, please try again.");
			}
		}else if(addStorageBinStep == 3) {
			try {
				storageBinAssemblyLine = Integer.parseInt(input);
				addStorageBinStep = 4;
				deliver("Please enter the amount of resources to add to this bin:");
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
		}else{
			try {
				int productionGoal = Integer.parseInt(input);
				
				try {
					
					// This has already been checked when comparing the input to available Products
					@SuppressWarnings("unchecked")
					Class<? extends Resource> theClass = (Class<? extends Resource>) Class.forName(storageBinResource);
					
					StorageBin bin = new StorageBin(theClass, storageBinCapacity);
					for(int i = 0; i < productionGoal; i++) {
						try {
							bin.addResource(theClass.newInstance());
						} catch (Exception e) {
							System.out.println("Could not create resource");
						}
					}
					
					server.getFactory().addStorageBinToAssemblyLine(bin, storageBinAssemblyLine);
					deliver("Product run scheduled.");
					this.displayMenu();
				} catch (ClassNotFoundException e) {
					deliver("Dammit. We screwed up!");
				}
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void handleT(String input) {
		if(robotStep == 0) {
			deliver("Please enter a numerical identifier for this robot:");
			robotStep = 1;
		}else if(robotStep == 1) {
			try {
				robotIdentifier = Integer.parseInt(input);
				robotStep = 2;
				deliver("Please enter the name of the resource this robot handles:");
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
		}else if(robotStep == 2) {
			if(input.equals("Button") || input.equals("Eye") || input.equals("Filling") || input.equals("Skin")) {
				robotResource = "plant.physical.resource." + input;
				robotStep = 3;
				deliver("Please enter the identifier of the assembly line to assign this robot:");
			}else{
				deliver("Invalid resource, please try again.");
			}
		}else {
			try {
				int assemblyLine = Integer.parseInt(input);

				Robot robot = null;
				try {
					robot = new Robot((Class<? extends Resource>) Class.forName(robotResource), robotIdentifier);
					server.getFactory().addRobotToAssemblyLine(robot, assemblyLine);
				} catch (ClassNotFoundException e) {
					System.out.println("Oops, this should have already been checked!? Invalid resource class!");
				}
				
				
				displayMenu();
			} catch(NumberFormatException e) {
				deliver("Input not a number. Please try again.");
			}
		}
	}
	
}
