/* ---------------------------------------------------------------------------
 ** This software is in the public domain, furnished "as is", without technical
 ** support, and with no warranty, express or implied, as to its usefulness for
 ** any purpose.
 **
 **  A library for ProIO HEP events storage and processing based on Google's ProtocolBuffers 
 **
 ** Author: S.Chekanov (ANL). chekanov@anl.gov
 ** Copyright  2018
 ** -------------------------------------------------------------------------*/


package hepsimproio;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.text.*;
import proio.Reader;
import proio.Event;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;


/**

 Extract the log file from ProIO file.

@author S.Chekanov (ANL)

**/


public class Log {
    public static void main(String[] args) {
     
         
        try {
          event_seeker(args[0]);
        } catch (IOException e) {
          e.printStackTrace();
        }
  
        
    }
  
    
  
    private static String event_seeker(String args) throws IOException {

      System.out.println("file          = "+args);

      int nn=0;

       try {

          FileMC fm= new FileMC(args);
          Reader reader = new Reader(args);
          Event storeevent = null;
          Event tmpEvent;
          while (true) {
            tmpEvent = null;
            try {
              tmpEvent = reader.next(true);
            } catch (Throwable e) {
              ;
            }
            if (tmpEvent == null) {
              break;
            }
            storeevent = tmpEvent;
            nn++;
          }

         String requested="0";
         String logfile="None";
         String lumi="0";
         String events="0";
         String description="none";
         String time="none";
         String cross="0";
         String error="0";
         Map<String, ByteString> metadata = storeevent.getMetadata();
          for (Map.Entry<String, ByteString> entry : metadata.entrySet()) {

           String skey=entry.getKey(); 
           String name=entry.getValue().toStringUtf8();
           if (!name.equals("logfile")) {

              //System.out.println(skey);
              if (skey.equals("meta:cross_section_pb")) cross=name; 
              else if (skey.equals("meta:cross_section_pb_err")) error=name;  
              else if (skey.equals("meta:creation_time")) time=name.replace("\n","");
              else if (skey.equals("meta:description")) description=name.replace("\n","");
              else if (skey.equals("meta:events")) events=name;
              else if (skey.equals("meta:luminosity_inv_pb")) lumi=name;
              else if (skey.equals("meta:logfile")) logfile=name;
              else if (skey.equals("meta:events_requested")) requested=name;

             //System.out.println("Metadata: " + entry.getKey() + ": " + entry.getValue().toStringUtf8());
            }
          }

/*
      System.out.println("last_modified = "+time);
      System.out.println("description   = "+description);
      System.out.println("events        = "+events);
      System.out.println("requested     = "+requested);
      System.out.println("sigma_pb = "+cross);
      System.out.println("sigma_err_pb = "+error);
      System.out.println("lumi_pb_inv = "+lumi);
*/
      System.out.println(logfile);
      if (nn-1 != Integer.valueOf(events)) 
      System.out.println(HepSim.ANSI_RED+"####  Error: File is corrupted. Inconsistent number of events!  ####"+HepSim.ANSI_RESET );


      } catch (Throwable e) {
        System.out.println(e);
      }
      return "";
    }
  }
  
