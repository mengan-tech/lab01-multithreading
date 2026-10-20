package edu.itc.se.lab01.task1;

public class Main {
    public static void main(String[] args) {
        boolean sequential = args.length > 0 && args[0].equals("--sequential");
        //1. countdown object is a subclass of Thread class
        Countdown appolo = new Countdown("apollo");


        //2. runnable passed to a Thread object
        Thread gemini = new Thread(new CountdownTask(), "gemini");
        

        //3. lambda expression passed to a Thread object
        Thread mercury = new Thread(() -> {
            String name = Thread.currentThread().getName();
            try{
                for (int i = 3; i > 0; i--) {
                    System.out.println(name + ": " + i);
                    Thread.sleep(200); 
                
                }
                System.out.println(name + " liftoff!");

            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }, "mercury");
        

        //4. Virtual Thread
        Thread artemis = Thread.ofVirtual().name("artemis").unstarted(new CountdownTask());

        // start all four depending on 'sequential'
        Thread[] threads = {appolo, gemini, mercury, artemis};
        System.out.println("Mission  control: all launched");
        for (Thread thread : threads){
            if (sequential) {
                thread.run(); // run sequentially in the main thread
            }
            else{
                thread.start();
            }
        
        }
        

        // join all four
        for (Thread thread : threads){
            try{
                thread.join();
                
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        System.out.println("Mission control: all landed");

        // print name, isVirtual(), getState() of each
        for (Thread thread : threads){
            System.out.println("Thread name: " + thread.getName() + ", isVirtual: " + thread.isVirtual() + ", state: " + thread.getState());
        }

    

        
    }
}