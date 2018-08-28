package hepsimproio.browser;

import java.io.*;



public class Main
 {

 /**
 *  Open ProIO file. 
 @author Chekanov
 **/

 public static void main(String args[]) {

  boolean goodVM=true;


  if(args.length > 0) {
            System.out.println("Open file="+args[0]);
            new MainGui(args[0]);
    } else {
            new MainGui(null); 
         }


}


}

