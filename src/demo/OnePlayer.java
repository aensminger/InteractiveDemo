package demo;



import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.StringPrimitive;

/*  Note: I'm playing around with Code Conventions here. Maybe set some sort
 *   of Java class header convention moving forward
 *
 *   One_Player
 *   Version 0.0.2
 *
 *   5/18/2015
 *   Copyright Devicify
 */
/**
 *
 * @author Alex Ensminger
 */
public class OnePlayer extends Mode {

    //think about not having public variables, use getters instead
    
    int presses;
    int lastFailed;
    long reactionTime;
    reactionTime times=new reactionTime(1);
    int[] pressCounter=new int[GameController.size];
    
    public void initialize() {
        for (int i=0; i<pressCounter.length; i++) {
                pressCounter[i]=0;
        }
    }
    
        @Override
    void createData() {
        presses = 0;
        
        //this loop adds up total presses, but we could certainly leave it split
        //into individual button presses and return that, or both
        for (int i = 0; i < pressCounter.length; i++) {
            presses += pressCounter[i];
        }

            ValueCollection newRow = new ValueCollection();
            newRow.put("Wins",new IntegerPrimitive(times.wins));
            newRow.put("Average",new IntegerPrimitive(times.average));
            newRow.put("Best",new IntegerPrimitive(times.best));
            newRow.put("Worst",new IntegerPrimitive(times.worst));
            newRow.put("Mode", new StringPrimitive("1Player"));
            newRow.put("ButtonID", new IntegerPrimitive(lastFailed));
            newRow.put("Players",new IntegerPrimitive(1));
        GameController.vsData.addRow(newRow);
 

  
    }

    

    @Override
    void start(long reactionDelay, int players) throws Exception {
        
        GameController.blocking=true;
        startup(GameController.size);
        
        initialize();
        
        int waitTime=1000;
        long delta = 25; //speed change variable
        boolean fail = false;

        while (!fail) {
            try {
                
                fail = true;
                boolean success = false;
                int randomLED = (int) (Math.random() * GameController.size + 1);
   
                int index = randomLED - 1; //think about shifting led/button clusters up an index to avoid
                lastFailed = randomLED;
                GameController.clearPressedFlags();
                start = System.currentTimeMillis();

                GameController.led[index].low(); //turn on LED to indicate button to push

                while (System.currentTimeMillis() < start + waitTime) {
                    
                    //for loop that just skips the "index" checking for incorrect button press
                    for (int i=0; i<GameController.buttonPressed.length; i++) {
                        if (i!=index) {
                            if (GameController.buttonPressed[i]==true) {
                                GameController.led[index].high();
                                GameController.ledRuntime[index]+=(double)(System.currentTimeMillis() - start)/1000;
                                times.instance=waitTime;
                                success=true;
                                break;
                            }
                        }
                    }
                    
                    //if correct button pressed first
                    if (GameController.buttonPressed[index] && !success) {
                        GameController.led[index].high();
                        times.wins++;
                        GameController.buttonPressed[index] = false;
                        times.instance= System.currentTimeMillis() - start;
                        GameController.ledRuntime[index]+=(double)times.instance/1000;
                        fail = false;
                        success = true;
                    }
                }
                //log the instance time
                times.collection.add(times.instance);

                //turn off led 
                GameController.led[index].high();
                Thread.sleep(waitTime / 2);
                waitTime -= delta*GameController.difficulty; //update waitTime so it speeds up

            } catch (InterruptedException ex) {
                throw new Exception("There was an error in OnePlayer.start(): " + ex.toString());
            }
        }

        times.computeAverage(reactionDelay);
        times.computeBest(reactionDelay);
        times.computeWorst(reactionDelay);
        GameController.blocking=false;
        createData();
    }



}
