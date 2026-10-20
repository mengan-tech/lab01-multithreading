package edu.itc.se.lab01.task1;

public class CountdownTask implements Runnable{
     @Override
    public void run() {
        String name = Thread.currentThread().getName();
        // Implementation for the countdown task
        try{
            for (int i = 3; i >0; i--) {
                System.out.println(name + ": " + i);
                Thread.sleep(200); 
            
            }
            System.out.println(name + " liftoff!");

        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    
}
