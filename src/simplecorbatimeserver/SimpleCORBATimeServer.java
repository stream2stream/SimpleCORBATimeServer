/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simplecorbatimeserver;

import corbastuff.KeyboardHandler;
import corbastuff.ORBHandler;
import corbastuff.codeWalker;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
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
    static  private final    String  itsServerIdCommand = "-serverid:";
    static  private final    String  itsInstancesCommand = "-instances:";
    static  private final    String  itsDebugCommand = "-debug:";
    static  private String  itsServerId = "TimeServer";
    static  private boolean isDebugOn = false;
    static  private TimeServerImpl[]  itsServants;
    static  private TimeServer[]      itsActivatedServants;
    static  private int     itsNoOfInstances = 1;
    private NamingContextExt itsNamingServer;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        SimpleCORBATimeServer self = new SimpleCORBATimeServer();
        
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
            
            self.parseParams(args);
            
            // New code
            TimeServerImpl[] myServants = self.activateTheServants( itsNoOfInstances );
            
            self.registerWithNamingService( itsActivatedServants );

            if( isDebugOn )
            {
                for( int idx = 0; idx < myServants.length; idx++ )
                {
                    // for testing purposues
                    self.saveIORToFile(itsServerId + idx + ".ior.txt", myServants[idx]);
                }
            }
            
            System.out.println( "CORBA Java server running ..." );
            self.waitForShutdownCommand();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        };
    }
    
    static private void saveIORToFile( String fname, TimeServerImpl myServant ) throws ServantNotActive, WrongPolicy
    {
        org.omg.CORBA.Object poaObj = ORBHandler.its_rootPOA.servant_to_reference(myServant);
        TimeServer servant = TimeServerHelper.narrow(poaObj);
        String objIOR = ORBHandler.its_ORB.object_to_string(servant);
        
        writeToFile( fname, objIOR );

        System.out.println( objIOR );
    }
    
    static  private void    writeToFile( String fname, String text )
    {
        try 
        {
            FileWriter writer = new FileWriter( fname, false );
            PrintWriter pwriter = new PrintWriter( writer );
            
            pwriter.print(text);
            pwriter.close();
        } catch (IOException ex) 
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void    parseParams( String args[] )
    {
        for (String arg : args)
        {
            int idx = arg.indexOf(itsServerIdCommand);
            if( idx > -1 )
            {
                String tempServerId = arg.substring(idx + itsServerIdCommand.length());
                if( tempServerId.length() > 0 )
                    itsServerId = tempServerId;
            }
            idx = arg.indexOf(itsInstancesCommand);
            if(idx > -1)
            {
                String tempInstanceValue = arg.substring(idx + itsInstancesCommand.length());
                if( tempInstanceValue.length() > 0 )
                {
                    itsNoOfInstances = Integer.parseInt(tempInstanceValue);
                }
            }
            idx = arg.indexOf(itsDebugCommand);
            if(idx > -1)
            {
                String tempDebugValue = arg.substring(idx + itsDebugCommand.length());
                if( tempDebugValue.equalsIgnoreCase("on") )
                {
                    isDebugOn = true;
                }
            }
        }
    }

        public  void waitForShutdownCommand()
    {
        Thread tt = new Thread( KeyboardHandler.getInstance() );
        
        tt.start();
        
        System.out.println("Waiting for a quit command");
        synchronized( KeyboardHandler.getInstance().itsMonitor )
        {
            try 
            {
                KeyboardHandler.getInstance().itsMonitor.wait();
            } 
            catch (InterruptedException ex) 
            {
                Logger.getLogger(codeWalker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Application is shutting down");
        unregisterFromNamingServer();
        ORBHandler.getInstance().shutdownOrderly();
        System.out.println("Application has quit...");
    }

    private TimeServerImpl[] activateTheServants( int noOfInstances )
    {
        itsServants = new TimeServerImpl[noOfInstances];
        itsActivatedServants = new TimeServer[noOfInstances];
        
        try
        {
            for( int idx = 0; idx < noOfInstances; idx++ )
            {
                itsServants[idx] = new TimeServerImpl();
                String oid = "[" + itsServerId + "-"+idx + "]";
                ORBHandler.its_POA.activate_object_with_id(oid.getBytes(), itsServants[idx]);
                org.omg.CORBA.Object theActivatedServant = ORBHandler.its_rootPOA.servant_to_reference(itsServants[idx]);
                itsActivatedServants[idx] = TimeServerHelper.narrow(theActivatedServant);            
            }
        } 
        catch (ServantAlreadyActive ex)
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (ObjectAlreadyActive ex)
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (WrongPolicy ex)
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (ServantNotActive ex) 
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return  itsServants;
    }

    public  void    registerWithNamingService( TimeServer[] activatedServants )
    {
        try
        {
            // Going to try and use the TAO naming service...
            org.omg.CORBA.Object obj = ORBHandler.its_ORB.resolve_initial_references( "NameService" );
            itsNamingServer = NamingContextExtHelper.narrow( obj );
            
            if( itsNamingServer == null )
                System.out.println(" Can't bind to the Naming Service");
            else
            {
                System.out.println(" Found Naming Service");

                for( int idx = 0; idx < itsNoOfInstances; idx++ )
                {
                    NameComponent nc1 = new NameComponent(itsServerId+"-"+idx, "clock object");
                    NameComponent[] name1 = {nc1};
                    itsNamingServer.rebind(name1, activatedServants[idx]);
                }
            }
        } 
        catch (InvalidName ex)
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotFound ex)
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CannotProceed ex)
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (org.omg.CosNaming.NamingContextPackage.InvalidName ex)
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
        private void unregisterFromNamingServer()
    {
        try 
        {
            if( itsNamingServer == null )
                System.out.println(" Naming Service not available");
            else
            {
            for( int idx = 0; idx < itsNoOfInstances; idx++ )
                {
                    NameComponent nc1 = new NameComponent(itsServerId + "-" + idx, "clock object");
                    NameComponent[]name1 = {nc1};
                    itsNamingServer.unbind(name1);
                }
            }
        } 
        catch (NotFound ex) 
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (CannotProceed ex) 
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (org.omg.CosNaming.NamingContextPackage.InvalidName ex) 
        {
            Logger.getLogger(SimpleCORBATimeServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


}
