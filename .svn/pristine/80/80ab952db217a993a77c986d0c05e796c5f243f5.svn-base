package demo;

import java.util.ArrayList;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.BooleanPrimitive;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.StringPrimitive;

/**
 *
 * @author Alex
 */
public class HeadToHead extends Mode {

    final int iterations = GameController.VSrounds;
    int players;

    ArrayList<reactionTime> times = new ArrayList<reactionTime>(GameController.size);

    //at some point this probably should change to an ArrayList, makes everything easier
    @Override
    public void createData() {
        highScore(times);
        times.sort(new IDComparator());
        for (int i = 0; i < players; i++) {
            
            ValueCollection newRow = new ValueCollection();
            newRow.put("Wins", new IntegerPrimitive(times.get(i).wins));
            newRow.put("Average", new IntegerPrimitive(times.get(i).average));
            newRow.put("Best", new IntegerPrimitive(times.get(i).best));
            newRow.put("Worst", new IntegerPrimitive(times.get(i).worst));
            newRow.put("Mode", new StringPrimitive("VS"));
            newRow.put("ButtonID", new IntegerPrimitive(times.get(i).ID+1));
            newRow.put("Players", new IntegerPrimitive(players));
            newRow.put("Winner", new BooleanPrimitive(times.get(i).winner));
            GameController.vsData.addRow(newRow);

        }

    }

    public void initializeReactionTimes(int iterations) {

        for (int i = 0; i < GameController.size; i++) {
            times.add(i, new reactionTime(iterations));
            times.get(i).ID = i;
        }

    }

    public void clearInstanceTimes() {
        for (int i = 0; i < times.size(); i++) {
            times.get(i).instance = 0;
        }
    }

    public void highScore(ArrayList<reactionTime> timeArray) {

        timeArray.sort(new winsComparator());
        Collections.reverse(timeArray);
        int winner = timeArray.get(0).ID;
        ArrayList<reactionTime> Array1 = new ArrayList<reactionTime>(0);
        for (int i = 0; i < timeArray.size(); i++) {
            //System.out.println("Button"+timeArray.get(i).ID+" has "+timeArray.get(i).wins);
            if (timeArray.get(0).wins == timeArray.get(i).wins) {
                Array1.add(timeArray.get(i));
            }
        }
        if (Array1.size() > 1) {

            Array1.sort(new averageComparator());
            winner = Array1.get(0).ID;
            ArrayList<reactionTime> Array2 = new ArrayList<reactionTime>(0);
            for (int i = 0; i < Array1.size(); i++) {
                if (Array1.get(0).average == Array1.get(i).average) {
                    Array2.add(Array1.get(i));
                }
            }
            if (Array2.size() > 1) {
                Array2.sort(new bestComparator());
                winner = Array2.get(0).ID;
                ArrayList<reactionTime> Array3 = new ArrayList<reactionTime>(0);
                for (int j = 0; j < Array2.size(); j++) {
                    if (Array2.get(0).best == Array2.get(j).best) {
                        Array3.add(Array2.get(j));
                    }

                }
                if (Array3.size() > 1) {
                    Array3.sort(new worstComparator());
                    winner = Array3.get(0).ID;
                    for (int k = 1; k < Array3.size(); k++) {
                        if (Array3.get(0).worst == Array3.get(k).worst) {
                            //if we get this far and there is a tie.. everyone loses
                            winner = -1;
                        }
                    }
                }
            }
        }
        
        for (int i = 0; i < timeArray.size(); i++) {
            if (timeArray.get(i).ID == winner) {
                timeArray.get(i).winner = true;

            } else {
                timeArray.get(i).winner = false;
            }

        }

    }

    //this is the individual run time
    void run(long reactionDelay, int players) throws Exception {
        clearInstanceTimes();
        allOn(players);
        GameController.clearPressedFlags();
        start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + reactionDelay) {

            //check for buttonpresses
            for (int i = 0; i < players; i++) {
                if (GameController.buttonPressed[i]) {
                    //check to see if this button has logged a time yet
                    //if not, assign time
                    if (times.get(i).instance < 1) {
                        times.get(i).instance = System.currentTimeMillis() - start;
                        GameController.led[i].high();
                    }
                }
            }
        }
        allOff();
    }

    @Override
    //this loops "run" method however many times
    void start(long reactionDelay, int players) throws Exception {
        this.players = players;
        GameController.blocking = true;
        try {

            startup(players);
            //clear/intialize reaction times
            initializeReactionTimes(iterations);

            //run the game some number of times
            for (int i = 0; i < iterations; i++) {
                int winner = -1; //initialized to show no winner
                //initialize bestTime to longest available (worst) time
                long bestTime = reactionDelay;
                run(reactionDelay, players);

                //after each run, log the button times to average later
                for (int j = 0; j < times.size(); j++) {

                    //if time=0, they didnt clock in, so set to max
                    if (times.get(j).instance == 0) {
                        times.get(j).instance = reactionDelay;
                    }

                    //log the instance to the collection
                    times.get(j).collection.add(times.get(j).instance);

                    //now check to see who won
                    if (times.get(j).instance < bestTime) {
                        bestTime = times.get(j).instance;
                        winner = j;
                    }
                }
                if (winner >= 0) {
                    
                    times.get(winner).wins++;
                 }

                //wait a bit
                Thread.sleep(500);

                //blink three times at 100ms intervals to prep for next round
//                if (i<iterations-1) {
//                    for (int j=0; j<3; j++) {
//                        blinkAll(100,players);
//                        Thread.sleep(200);
//                    }
//                }
                //wait a bit again, but make this random
                int randomWait = randInt(0, 2000);

                Thread.sleep(randomWait);
            }

            //compute the average, best and worst, total runtime
            //this is some weird array loop notation, look into this more
            for (reactionTime time : times) {
                time.computeAverage(reactionDelay);
                time.computeBest(reactionDelay);
                time.computeWorst(reactionDelay);
                time.computeRuntime();
            }
            //compute the winner

            for (int i = 0; i < players; i++) {
                //assign runtime based on players, not times(which is always 3)
                GameController.ledRuntime[i] += (double) times.get(i).runTime / 1000;
            }
        } catch (InterruptedException ex) {
            throw new Exception("Error in VS.start(): " + ex.toString());
        } catch (Exception ex1) {
            throw new Exception("Error in VS.start(): " + ex1.toString());
        }

        GameController.blocking = false;
       // highScore(times);
        createData();

    }

}
