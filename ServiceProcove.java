import java.io.*;
import java.net.*;

// Classe qui s'occupe des échanges avec un client
class ServiceProcove implements Runnable
{
	public final int DELAI = 30000;
	public final int PORT_PROCOVE = 51000;
	// Check control du robot
	public static ServiceProcove control = null;
	private Socket aSocket;
	private BufferedReader reader;
    private PrintWriter writer;
	Simbot mRobot;
	String [] ControlCommand = { "MOT","ROT"};
	String [] NoControlCommand = { "REL","FIN","SYN","DEL","VER","LST","SPC","BAS","BAT","CAM","COL","DIR","LAS","LUM","NAO","POS","SON","VIT" };
	
	IntHolder nbr;
	// Le constructeur reçoit en paramètre un socket déjà connecté au client et un simbot
	public ServiceProcove(Socket s, Simbot robot,IntHolder n)
	{
		aSocket = s;
		mRobot = robot;
		nbr = n;
		try
		{
			// Obtention des flux d'entrée-sortie
            reader = new BufferedReader(new InputStreamReader(aSocket.getInputStream()));
			// Le writer est "autoflush"
            writer = new PrintWriter(new OutputStreamWriter(aSocket.getOutputStream()),true);
		}
		catch (IOException ioee)
		{
			System.err.println("Echec a la creation du service procove");
			System.exit(1);
		}
	}
	// Fonction qui regarde si la commande est dans un array et return true
	private static boolean check(String[] arr, String toCheckCommand) 
    {
        boolean test = false; 
        for (String element : arr)
		{ 
            if (element.toUpperCase().equals(toCheckCommand.toUpperCase()))
			{ 
                test = true; 
                break; 
            } 
        } 
		return test;
    }
	// Fonction qui return le nombre de parametre que la commande passer en parametre peut avoir
	public int NombreDeParametre(String n)
	{
		if (n.toUpperCase().equals("MOT") || n.toUpperCase().equals("ROT"))
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}

	// Fonction qui execute les commandes
	public boolean executeCommand(String[] cmd, boolean thisClientHasControl, boolean someClientHasControl, boolean[] hasHasAcquiredCTR)
	{
		boolean isFIN = false;
		
		if (cmd[0].toUpperCase().equals("FIN"))
		{
			isFIN = true;
			writer.println("ACK");
			if (thisClientHasControl)
			{
				control = null;
			}
		}
		else if (cmd[0].toUpperCase().equals("BAS") || 
				 cmd[0].toUpperCase().equals("BAT") ||
				 cmd[0].toUpperCase().equals("COL") ||
				 cmd[0].toUpperCase().equals("DIR") ||
				 cmd[0].toUpperCase().equals("LAS") ||
				 cmd[0].toUpperCase().equals("LUM") ||
				 cmd[0].toUpperCase().equals("NAO") ||
				 cmd[0].toUpperCase().equals("POS") ||
				 cmd[0].toUpperCase().equals("SON") ||
				 cmd[0].toUpperCase().equals("VIT"))
		{
			writer.println("ERR 5");
			writer.flush();
			System.out.println("Les commandes ne sont pas implementé");
		}
		else if (cmd[0].toUpperCase().equals("LST"))
		{
			writer.println("LST CTR DEL FIN LST MOT REL ROT SPC SYN VER");
			writer.flush();
		}
		else if (cmd[0].toUpperCase().equals("VER"))
		{
			writer.println("VER 1.8");
			writer.flush();
		}
		else if (cmd[0].toUpperCase().equals("REL"))
		{
			if (thisClientHasControl || !thisClientHasControl)
			{
				writer.println("ACK");
				control = null;
			}
			writer.flush();
		}
		else if (cmd[0].toUpperCase().equals("SYN"))
		{
			writer.println("ACK");
			writer.flush();
		}
		else if (cmd[0].toUpperCase().equals("DEL"))
		{
			int d = DELAI/1000;
			writer.println("DEL " + d);
		}
		else if (cmd[0].toUpperCase().equals("CTR"))
		{
			
			if (someClientHasControl == false)
			{
				writer.println("ACK");
				if (control == null)
				{
					control = this;
					hasHasAcquiredCTR[0]=true;
				}
			}
			else if (thisClientHasControl)
			{
				writer.println("ACK");
			}
			else
			{
				// not called
			}
		}
		else if (cmd[0].toUpperCase().equals("CAM"))
		{
			
		}
		else if (cmd[0].toUpperCase().equals("MOT"))
		{
			try
			{
				int v = Integer.parseInt(cmd[1]);
				if (v > 100 || v < -100)
				{
					writer.println("ERR 4");
				}
				else
				{
					mRobot.setPuissance(v);
					writer.println("ACK");
				}
			} 
			catch(NumberFormatException e)
			{
				writer.println("ERR 3");
			}
		}
		
		else if (cmd[0].toUpperCase().equals("SPC"))
		{
			String SPC = mRobot.getSpecs();
			writer.println("SPC " + SPC);
		}
		
		else if (cmd[0].toUpperCase().equals("ROT"))
		{
			try
			{
				int v = Integer.parseInt(cmd[1].toUpperCase());
				if (v > 100 || v < -100)
				{
					writer.println("ERR 4");
				}
				else
				{
					mRobot.setRotation(v);
					writer.println("ACK");
				}
			}
					
			catch(NumberFormatException e)
			{
				writer.println("ERR 3");
			}
		}
		
		writer.flush();
		return isFIN;
	}
	
	public void run()
    {		
		try
		{
            // traitement (lecture-écriture selon un protocole déterminé)
			boolean fini = false;
            String ligne = null;
			
            while (!fini)
			{
				boolean thisClientHasControl = false;
				boolean someClientHasControl = false;
				
				try
				{
					boolean ok = true;
					if ((control != null) && (control.aSocket == this.aSocket))
					{
						thisClientHasControl = true;
					}			
					if (control != null)
					{
						someClientHasControl = true;
					}
					
					// Lit une ligne en provenance dun client
					ligne = reader.readLine();
					
					// une ligne nulle signifie que le flux est fermé (probablement
					// que l'utilisateur a fermé la fenêtre de PuTTY) et une ligne
					// vide que l'utilisateur veut se déconnecter
					if (ligne == null || ligne.length() == 0)
					{
						//fini = true;
						writer.println("ERR 1");
						continue;
					}
					
					ligne = ligne.replaceAll("\\s+", " ");
					String[] splitArray = ligne.split(" ");

					// Valid command
					if (splitArray.length == 0)
					{
						writer.println("ERR No command");
						writer.flush();
						ok = false;
					}
					else
					{
						if (splitArray[0].toUpperCase().equals("CTR"))
						{
							// ok processed elsewhere
						}
						else
						{
							if ( (!check(ControlCommand,splitArray[0].toUpperCase())) && (!check(NoControlCommand,splitArray[0].toUpperCase())) )
							{
								writer.println("ERR 1");
								System.out.println("La commande est invalide");
								writer.flush();
								ok = false;
							}
							
							if (ok)
							{
								if (NombreDeParametre(splitArray[0].toUpperCase()) != splitArray.length - 1)
								{
									writer.println("ERR 2");
									System.out.println("Les parametres sont invalide");
									writer.flush();
									ok = false;
								}
							}
						}
					}
								
					// Check Is a Control command with robot control
					if (ok)
					{
						if ((check(ControlCommand,splitArray[0].toUpperCase())) && (thisClientHasControl == false))
						{
							writer.println("ERR 7");
							System.out.println("Vous avez besoins du controle pour cette commande");
							writer.flush();
							ok = false;
						}
					}

					// Check CTR commande avec personne qui ne controle le robot
					if (ok)
					{
						if ((splitArray[0].toUpperCase().equals("CTR")) && (someClientHasControl == true) && (thisClientHasControl == false))
						{
							writer.println("ERR 7");
							System.out.println("Quelqun a deja le controle");
							writer.flush();
							ok = false;
						}
					}
							
					if (ok)
					{
						boolean[] hasHasAcquiredCTR = new boolean[1];
						hasHasAcquiredCTR[0]=false;
						fini = executeCommand(splitArray, thisClientHasControl, someClientHasControl, hasHasAcquiredCTR);
						if (fini == false)
						{
							if (hasHasAcquiredCTR[0] == true)
							{
								aSocket.setSoTimeout(DELAI);
							}
						}
					}
				}
				catch(SocketTimeoutException ste)
				{
					
					if ((control != null) && (control.aSocket == this.aSocket))
					{
					}
					if (thisClientHasControl)
					{
						System.err.println("Vous avez perdu le controle");
						control = null;
						aSocket.setSoTimeout(0);
					}
				}
			}

            // fermeture des flux
            reader.close();
            writer.close();

			if ((control != null) && (control.aSocket == this.aSocket))
			{
				control = null;
			}
			
			// fermeture des sockets
			aSocket.close();
			//nbr.value--;
			
		}		
		catch (IOException ioe)
		{
			System.err.println("Deconexion anormale dun client");
			nbr.value--;
			if ((control != null) && (control.aSocket == this.aSocket))
			{
				control = null;
			}
            System.exit(1);
        }
		finally
		{
			System.out.println("Deconexion du client");
			nbr.value--;
		}
    }
}