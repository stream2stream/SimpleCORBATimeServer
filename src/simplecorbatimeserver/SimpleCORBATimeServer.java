/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simplecorbatimeserver;

import corbastuff.ORBHandler;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.CosNaming.*;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import timeServices.TimeServer;
import timeServices.TimeServerHelper;

/**
 *
 * @author Selvyn
 */
public class SimpleCORBATimeServer
{
    static private String  itsServerIdCommand = "-serverid:";
    static private String  itsServerId = "TimeServer";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        Object tobj = new Object();
        try
        {
            ORBHandler orb = ORBHandler.getInstance();
            orb.setArgs(args);
            orb.start();

            synchronized( ORBHandler.getInstance().itsMonitor )
            {
                ORBHandler.getInstance().itsMonitor.wait();
            }
            
            for( int argv = 0; argv < args.length; argv++ )
            {
                int idx = args[argv].indexOf(itsServerIdCommand);
                if( idx > -1 )
                {
                    String tempServerId = args[argv].substring(idx + itsServerIdCommand.length());
                    if( tempServerId.length() > 0 )
                        itsServerId = tempServerId;
                }
            }

            // add your creating of object implementation here
            TimeServerImpl myServant = new TimeServerImpl();
            ORBHandler.its_POA.activate_object_with_id("TimeServer".getBytes(), myServant);
            //myServant._this();

            // Going to try and use the TAO naming service...
            org.omg.CORBA.Object obj = ORBHandler.its_ORB.resolve_initial_references( "NameService" );
            NamingContextExt its_NamingServer = NamingContextExtHelper.narrow( obj );

            if( its_NamingServer == null )
                System.out.println(" Can't bind to the Naming Service");
            else
                System.out.println(" Found Naming Service");
            
            NameComponent nc1 = new NameComponent(itsServerId, "kernel object");
            NameComponent[] name1 = {nc1};
            its_NamingServer.rebind(name1, ORBHandler.its_rootPOA.servant_to_reference(myServant));

            // for testing purposues
            saveIORToFile(myServant);

            System.out.println("plans rebind sucessful!");

            System.out.println( "CORBA Java server running ..." );
        }
        catch( Exception e )
        {
            e.printStackTrace();
        };
    }
    
    static private void saveIORToFile( TimeServerImpl myServant ) throws ServantNotActive, WrongPolicy
    {
        org.omg.CORBA.Object poaObj = ORBHandler.its_rootPOA.servant_to_reference(myServant);
        TimeServer servant = TimeServerHelper.narrow(poaObj);
        String objIOR = ORBHandler.its_ORB.object_to_string(servant);
        
        writeToFile( objIOR );

        System.out.println( objIOR );
    }
    
    static  private void    writeToFile( String text )
    {
        try 
        {
            FileWriter writer = new FileWriter( "timeserver.ior.txt", false );
            PrintWriter pwriter = new PrintWriter( writer );
            
            pwriter.print(text);
            pwriter.close();
        } catch (IOException ex) 
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

}
