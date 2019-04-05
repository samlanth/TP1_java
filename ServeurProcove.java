import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
	

class IntHolder
{
    public Integer value;

    IntHolder(Integer value)
	{
        this.value = value;
    }

    @Override
    public String toString()
	{
        return String.valueOf(value);
    }
}

public class ServeurProcove
 {
	// Variables membres
    Simbot mRobot;
	
	// Constructeur
    public ServeurProcove(Simbot robot)
	{
        mRobot = robot;
    }

    private ServerSocket socServeur;
	private Socket client;

	// Fonctionne un peu comme un serveur Echo : pour chaque client connecté
    // on crée une instance de "ServiceProcove" qu'on lance dans un thread
    public void servir(int port)
	{
        try
		{
			boolean enService = true;
			
            // Étape 1 : création du socket socServeur
            socServeur = new ServerSocket(port);
            System.out.println("Serveur procove en attente d'une connexion");
			IntHolder nbr = new IntHolder(0);
			while (enService)
			{
				client = socServeur.accept();
				
				if (nbr.value<3)
				{
					System.out.println("Client connecte");
					nbr.value++;
					
					ServiceProcove connexion = new ServiceProcove(client,mRobot,nbr);
					Thread t = new Thread(connexion);
					t.start();
					System.out.println("NBR= " + (nbr.value));
				}
				else
				{
					System.out.println("Trop de usager = " + (nbr.value));
				}
			}
			client.close();
            socServeur.close();
        } 
		catch (IOException ioe)
		{
            ioe.printStackTrace();
            System.exit(1);
        }
    }
	
    public static void main(String args[])
	{
		int PORT_PROCOVE = 51000; // Default
        // Création d'un simulateur de robot avec rafraîchissement aux 5 secondes
        Simbot robot = new Simbot(2000);
		if (args.length == 1)
		{
			try
			{
				int noPort = Integer.parseInt(args[0]);
				if (noPort == 0)
				{
					System.err.println("Utilise port defaut");
					noPort = PORT_PROCOVE;
				}
				
				// Création et lancement du serveur
				ServeurProcove serveur = new ServeurProcove(robot);
				serveur.servir(noPort);

			}
			catch (NumberFormatException nfe)
			{
				System.err.println("Erreur de format port");
			}
		}
		else
		{	
			// Création et lancement du serveur
			ServeurProcove serveur = new ServeurProcove(robot);
			serveur.servir(PORT_PROCOVE);
		}
		
    }
}