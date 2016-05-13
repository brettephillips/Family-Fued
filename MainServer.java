//imports the necesary classes for the GUI program
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class MainServer
{   
   private ArrayList<String> questionsList = new ArrayList<String>();
   private ArrayList<String> answersList = new ArrayList<String>();
   private Vector<PrintWriter> printers = new Vector<PrintWriter>();
   private int printerNumber = -1;
   private int questionCount = 1;
   private int[] playerScore = new  int[4];
   private String[] playerName = new String[4];
   private int playerCount = -1;
   private int index = 0;
   
   public MainServer(){
      ServerSocket ss;
      try
      {
         ss = new ServerSocket(16100);
         Socket s = null;
         int threadCount = 0;
         while(true)
         {
            s = ss.accept();
            Thread th = new MainServerInner(s, threadCount+"");
            th.start();
            threadCount++;
         }         
      }
      catch(IOException ioe){
      }                   
   }
   
   class MainServerInner extends Thread
   {
      Socket s;
      String outputMessage = "";     
      HashSet<Integer> numsCalled = new HashSet<Integer>();
      Random generate = new Random();
      String threadName = ""; 
      
      public MainServerInner(Socket _s, String _name){
         s = _s;
         threadName = _name;
      }
      
      public String getThreadName(){
         return threadName;
      }
      
      public String makeQuestion(){
         
         boolean check = false;
         index = generate.nextInt(25);
         String output = "";
        
         while(!check){
            if(!numsCalled.contains(index)){
               numsCalled.add(index);
               check = true;
               output = questionsList.get(index);
               return output;
            }
            else{
               index = generate.nextInt(25);             
            }
         }
         return "error";
      }
      
      public String makeAnswer(){
         return answersList.get(index);
      }
      
      public void broadcast(String msg){
         for(PrintWriter p: printers)
         {
            if(msg.contains("DISCONNECT: ")){
               System.out.println(msg.substring(12));
               playerCount--;
               break;
            }
            else{                
               p.println(msg);
               p.flush();
            }
         }
      }
      
      public void unlockNextClient(){
         if(printerNumber == -1){
            printerNumber++;
            printers.get(printerNumber).println("UNLOCK");
            printers.get(printerNumber).flush();
         }
         if(printerNumber>= 0 && printerNumber<3){
            
            printers.get(printerNumber).println("UNLOCK");
            printers.get(printerNumber).flush();
            printerNumber++;
         }        
         else{
            printerNumber = 0;
            printers.get(printerNumber).println("UNLOCK");
            printers.get(printerNumber).flush();
            //set the unlock to printer zero
         }
      
      }
      public void lockClients(){
         broadcast("LOCK");
      }
      
      public void unlockCurrentClient(){
         printers.get(printerNumber).println("UNLOCK");
         printers.get(printerNumber).flush();
      }
   
      public void run()
      {
         BufferedReader brRun = null;
         PrintWriter pwRun = null;          
         
         try{        
            brRun = new BufferedReader(new InputStreamReader(s.getInputStream()));
            pwRun = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
            printers.add(pwRun);
            addAnswers();
            addQuestions();
            
           
            while(true){
               outputMessage = brRun.readLine();
                                 
               if(outputMessage.contains("Player Name:") && !outputMessage.contains("Message:") && !outputMessage.contains("READY!!!")){
                  if(playerCount<3){
                     playerCount++;                  
                     System.out.println("Player Number: "+(playerCount+1));
                     playerName[playerCount] = outputMessage.substring(12);
                     System.out.println("Name entered: "+playerName[playerCount]);
                     System.out.println("Player: "+playerName[playerCount]+ " connected.\n\n");   
                     
                    }     
                   if(playerCount == 3 ){                                             
                        
                     String teamMemberNames = "NameHeader, ";
                                      
                     for(String list: playerName){
                        teamMemberNames +=(list+", ");
                     }
                           //this is for the list of players that are connected
                     broadcast("NameHeader: "+teamMemberNames);
                     System.out.println("NAMES HAVEN BEEN SENT TO THE CLIENTS!!!");    
                  }                        
                  else if(playerCount>=4){
                        //this should get a tag
                        broadcast("Message: A spectator has been added");
                        playerCount++;
                     }
                     
                                      
               }          
                              
               else if(outputMessage.contains("READY!!!")){
                  if(questionCount < 4){
                     questionCount++;
                     System.out.println("current question count: "+questionCount);
                  }
                  else if(questionCount == 4){
                     questionCount = 1;
                     outputMessage = "Question: "+ makeQuestion();
                     broadcast(outputMessage);
                     //unlockNextClient(); 
                  }  
                                
               }
                  
               else if(outputMessage.contains("Answer: ")){
                  printerNumber = Integer.parseInt(getThreadName());
                  System.out.println("THREAD SUBMISSION NUMBER: "+printerNumber);
                  lockClients();
                  String temp = answersList.get(index);
                  String[] x = temp.split(",");
                     
                  for(String list : x){
                     
                     System.out.println("Database Answers: "+list);
                     System.out.println(outputMessage);
                        
                     if(list.contains(outputMessage.substring(8))){
                        System.out.println("Correct: " + list+"\n");
                        broadcast("Correct: " + list);
                        unlockCurrentClient();
                           //use the current thread's name and unlock it again
                     }
                     else{
                        //broadcast("WRONG!!!");
                     }
                  }
               }              
                               
               broadcast(outputMessage);
            }
         }              
            
         catch(NullPointerException npe)
         {
            System.err.println("Client has disconnected");
         }        
         catch(IOException ioe){
         }
      
      }
     
      
   
      public void addAnswers(){
         FileReader frTwo = null;
         BufferedReader brTwo = null;      
         String line;      
         try{
            frTwo = new FileReader("FAMILY FEUD ANSWERS.csv");
            brTwo = new BufferedReader(frTwo);
         }
         catch(FileNotFoundException fnfe){
         }
      
         try{
            line = brTwo.readLine();         
         
            while(!line.equals(null)){
               answersList.add(line);        
               line = brTwo.readLine();
            }
         }
         catch(IOException ioe){
         } 
         catch(NullPointerException npe){
         }   
      }
   
      public void addQuestions()
      {
         FileReader fr = null;
         BufferedReader br = null;      
         String line;
      
         try
         {
            fr = new FileReader("FAMILY FEUD QUESTIONS.csv");
            br = new BufferedReader(fr);
         }
         catch(FileNotFoundException fnfe)
         {
         }
      
         try
         {
            line = br.readLine();
            while(!line.equals(null))
            {
               questionsList.add(line);
               line = br.readLine();
            } 
         }
         catch(IOException ioe)
         {
         }
         catch(NullPointerException npe)
         {
         }
      }
   
   }
   public static void main(String [] args)
   {
      MainServer mainServer = new MainServer();
   }

}

/*
 //this is making new question
                        outputMessage = "Question: "+ makeQuestion();
             
                        //broadcasts the question               
                        broadcast(outputMessage);
                        unlockNextClient();  need this later*/