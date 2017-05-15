package demo;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

/**
 *
 * @author Alex
 */
public abstract class Mode {

    long start;
    
    static Random rand = new Random();

    public static class reactionTime {

        int ID;
        long instance;
        ArrayList<Long> collection;
        long average;
        long best;
        long worst;
        int wins;
        long runTime;
        boolean winner;

        //constructor
        public reactionTime(int iterations) {
            instance = 0;
            collection = new ArrayList<Long>(iterations);
            average = 0;
            wins = 0;
            runTime=0;

        }

        long computeAverage(long reactionDelay) {
            

            long temp = 0;

            for (int i = 0; i < collection.size(); i++) {
                if (collection.get(i)==0) {
                    collection.set(i,reactionDelay);
                }
                temp += collection.get(i);
            }
            average = temp / collection.size();
            return average;
        }

        long computeBest(long reactionDelay) {
            best = reactionDelay;

            for (int i = 0; i < collection.size(); i++) {
                
                if (collection.get(i)==0) {
                    collection.set(i,reactionDelay);
                }

                if (collection.get(i) < best) {
                    best = collection.get(i);
                }
            }
            return best;
        }

        long computeWorst(long reactionDelay) {
            worst = 0;
            for (int i = 0; i < collection.size(); i++) {
                if (collection.get(i)==0) {
                    collection.set(i,reactionDelay);
                }    
                
                if (collection.get(i) > worst) {
                    worst = collection.get(i);
                }
            }
            return worst;
        }
        
        long computeRuntime() {
            for (int i = 0; i < collection.size(); i++) {
                runTime+=collection.get(i);
            }
            return runTime;
        }

    }
    
    static class bestComparator implements Comparator<reactionTime> {

        @Override
        public int compare(reactionTime o1, reactionTime o2) {
            long best1 = o1.best;
            long best2 = o2.best;

            return Long.compare(best1, best2);
        }

    }

    static class averageComparator implements Comparator<reactionTime> {

        @Override
        public int compare(reactionTime o1, reactionTime o2) {
            long best1 = o1.average;
            long best2 = o2.average;

            return Long.compare(best1, best2);
        }

    }

    static class worstComparator implements Comparator<reactionTime> {

        @Override
        public int compare(reactionTime o1, reactionTime o2) {
            long best1 = o1.worst;
            long best2 = o2.worst;

            return Long.compare(best1, best2);
        }

    }
    
        static class winsComparator implements Comparator<reactionTime> {

        @Override
        public int compare(reactionTime o1, reactionTime o2) {
            long best1 = o1.wins;
            long best2 = o2.wins;

            return Long.compare(best1, best2);
        }

    }
                static class IDComparator implements Comparator<reactionTime> {

        @Override
        public int compare(reactionTime o1, reactionTime o2) {
            long best1 = o1.ID;
            long best2 = o2.ID;

            return Long.compare(best1, best2);
        }

    }
        

    public void allOn() {
        for (int i = 0; i < GameController.led.length; i++) {
            GameController.led[i].low();
        }
    }

    public void allOn(int leds) {
        if (leds > GameController.led.length || leds < 0) {
            leds = GameController.led.length;
        }

        for (int i = 0; i < leds; i++) {
            GameController.led[i].low();
        }
    }

    public void allOff() {
        for (int i = 0; i < GameController.led.length; i++) {
            GameController.led[i].high();
        }
    }

    public void allOff(int leds) {
        if (leds > GameController.led.length || leds < 0) {
            leds = GameController.led.length;
        }

        for (int i = 0; i < leds; i++) {
            GameController.led[i].high();
        }
    }

    public void blinkAll(int interval, int leds) {

        try {
            allOn(leds);
            Thread.sleep(interval);
            allOff(leds);

        } catch (InterruptedException ex) {
            GameClient.appendToLogFile("blinkAll failed: " + ex.toString());

        }
    }
    
    public static int randInt(int min, int max) {

    //inclusive of min and max
    int randomNum = rand.nextInt((max - min) + 1) + min;

    return randomNum;
}

    public void alternateLEDs(int interval, int leds) {

        try {
            for (int i = 0; i < leds; i++) {
                allOff(GameController.size);
                GameController.led[i].low();
                Thread.sleep(interval);
            }
        } catch (InterruptedException ex) {
            GameClient.appendToLogFile("There has been an error in alternateLEDs: " + ex.getMessage());
        }
    }

    public void startup(int leds) {

        //startup LED blinks, waits for button press to begin
        GameController.clearPressedFlags();
        try {

            while (!GameController.startPressed) {

                alternateLEDs(400/GameController.difficulty, leds);
            }

            //get rid of blinks, just wait
            allOff(leds);
            Thread.sleep(500);
//            blinkAll(100, leds);
//            Thread.sleep(200);
//            blinkAll(100, leds);
//            Thread.sleep(200);
//            blinkAll(100, leds);
//            Thread.sleep(200);
//            blinkAll(100, leds);
            Thread.sleep(1000);
            Thread.sleep(1000);

        } catch (InterruptedException ex) {
            GameClient.appendToLogFile("Startblinks failed: " + ex.toString());
        }

        GameController.clearPressedFlags();
    }

    abstract void start (long ReactionDelay, int Players) throws Exception;

    abstract void createData() throws Exception;

}
