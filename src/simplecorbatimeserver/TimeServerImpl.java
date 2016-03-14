/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simplecorbatimeserver;

import  timeServices.*;
import  getutcdateandtime.*;
/**
 *
 * @author Selvyn
 */
public class TimeServerImpl extends TimeServerPOA
{
    private	int count = 0;
    
    public TimeServerImpl()
    {
        this._initialize_inheritance_tree();
    }
    
    @Override
    public java.lang.String getJavaDateAndTime()
    {
        GetUTCDateAndTime service = new GetUTCDateAndTime();
        String result = "[" + service.getDateAndTime() + "]";
        
        System.out.println(result);
        
        return result;
    }
    
    public void _initialize_inheritance_tree()//GEN-BEGIN:M_void__initialize_inheritance_tree_
    {
        // Do not edit! This is a method which is necessary for using delegation.
    }//GEN-END:M_void__initialize_inheritance_tree_
    
    
}