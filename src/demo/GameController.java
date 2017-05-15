package demo;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.filetransfer.FileTransferVirtualThing;
import com.thingworx.metadata.FieldDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.metadata.collections.FieldDefinitionCollection;
import com.thingworx.types.BaseTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.constants.CommonPropertyNames;
import com.thingworx.types.primitives.LocationPrimitive;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alex
 */
@SuppressWarnings("serial")
//these properties are just for testing right now, they are not needed
@ThingworxPropertyDefinitions(properties = {
    @ThingworxPropertyDefinition(name = "Last_Location", description = "", baseType = "LOCATION", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"}),
    @ThingworxPropertyDefinition(name = "LED1_Runtime", description = "", baseType = "NUMBER", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"}),
    @ThingworxPropertyDefinition(name = "LED2_Runtime", description = "", baseType = "NUMBER", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"}),
    @ThingworxPropertyDefinition(name = "LED3_Runtime", description = "", baseType = "NUMBER", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"}),
    @ThingworxPropertyDefinition(name = "Software_Version", description = "", baseType = "STRING", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"}),
    @ThingworxPropertyDefinition(name = "External_IP", description = "", baseType = "STRING", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"}),
    @ThingworxPropertyDefinition(name = "Internal_IP", description = "", baseType = "STRING", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"}),
    @ThingworxPropertyDefinition(name = "Current_Mode", description = "", baseType = "STRING", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"}),

    @ThingworxPropertyDefinition(name = "Connected_Runtime", description = "", baseType = "NUMBER", category = "Status", aspects = {"isReadOnly:false", "isPersistant:true", "dataChangeType:ALWAYS", "pushType:ALWAYS"})
})

public final class GameController extends FileTransferVirtualThing {

    private static final String softwareVersion = "1.0.3.5";
    public static String externalIP;
    public static String internalIP;
    public static String currentMode = "Startup";

    public static InfoTable vsData;
    public static InfoTable onePlayerData;
    public static boolean stopALL = false;

    final static int size = 3;
    public static int VSrounds;
    public static boolean blocking = false;
    public static double Connected_Runtime;

    public static GpioController gpio;
    public static GpioPinDigitalOutput[] led = new GpioPinDigitalOutput[size];
    public static GpioPinDigitalInput[] button = new GpioPinDigitalInput[size];
    public static boolean[] buttonPressed = new boolean[size];
    public static double[] ledRuntime = new double[size];
    public static Integer difficulty;
    public static boolean startPressed;
    public static GpioPinDigitalOutput connected; //connection indicator LED
    public static GpioPinDigitalOutput statusCom; //status indicator, blinking etc
    public static GpioPinDigitalInput startButton;
    public static LocationPrimitive location;

    //ThWx Constructor GC
    public GameController(String name, String description, ConnectedThingClient client, HashMap<String, String> virtualDirectories) {
        super(name, description, client, virtualDirectories);
        try {
            createDataShapes();
            initializeGPIO();
            addListeners();
            super.initializeFromAnnotations();
            getRuntimes();
        } catch (Exception ex) {
            Logger.getLogger(GameController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String readProperty(String property_filename) throws Exception {

        String value = "0";
        try {
            File fin = new File("/opt/InteractiveDemo/" + property_filename + ".txt");
            FileInputStream fis = new FileInputStream(fin);
            //Construct BufferedReader from InputStreamReader
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            value = br.readLine();
            if (value == null) {
                value = "0";
            }
            fis.close();
            br.close();
        } catch (Exception e) {
            GameClient.appendToLogFile("There was an error in readProperty(" + property_filename + "): " + e.toString());
        }
        return value;

    }

    public void saveProperty(String property_filename, String Value) throws Exception {
        try {
            File fin = new File("/opt/InteractiveDemo/" + property_filename + ".txt");
            FileWriter fos = new FileWriter(fin, false);
            //Construct BufferedReader from InputStreamReader
            BufferedWriter bw = new BufferedWriter(fos);
            bw.write(Value);
            bw.close();
            fos.close();

        } catch (Exception e) {
            GameClient.appendToLogFile("There was an error in saveProperty(" + property_filename + ", " + Value + "): " + e.toString());
        }

    }

    public void createDataShapes() {

        FieldDefinitionCollection vsFields = new FieldDefinitionCollection();
        vsFields.addFieldDefinition(new FieldDefinition("Mode", BaseTypes.STRING));
        vsFields.addFieldDefinition(new FieldDefinition("Email", BaseTypes.STRING));
        vsFields.addFieldDefinition(new FieldDefinition("Initials", BaseTypes.STRING));
        vsFields.addFieldDefinition(new FieldDefinition("Players", BaseTypes.INTEGER));
        vsFields.addFieldDefinition(new FieldDefinition("Best", BaseTypes.INTEGER));
        vsFields.addFieldDefinition(new FieldDefinition("Worst", BaseTypes.INTEGER));
        vsFields.addFieldDefinition(new FieldDefinition("Average", BaseTypes.INTEGER));
        vsFields.addFieldDefinition(new FieldDefinition("Wins", BaseTypes.INTEGER));
        vsFields.addFieldDefinition(new FieldDefinition("GUID", BaseTypes.GUID));
        //this is "LastFailed" or "Player Number", depending on mode
        vsFields.addFieldDefinition(new FieldDefinition("ButtonID", BaseTypes.INTEGER));
        //maybe we want to record what hand the user is using
        vsFields.addFieldDefinition(new FieldDefinition("Handedness", BaseTypes.STRING));
        vsFields.addFieldDefinition(new FieldDefinition("Winner", BaseTypes.BOOLEAN));
        defineDataShapeDefinition("DVDemo.GameData", vsFields);

    }

    @Override // ThWx GC
    public void synchronizeState() {
        // Be sure to call the base class
        super.synchronizeState();
        // Send the property values to Thingworx when a synchronization is required
        super.syncProperties();
    }

    //@Override //ThWx GC
    @Override
    public void processScanRequest() throws Exception {
        // Be sure to call the base classes scan request
        super.processScanRequest();
        // Execute the code for this simulation every scan
        this.scanDevice();

    }

    //ThWx GC
    public void scanDevice() throws Exception {
        //save to file first, then transmit

        saveRuntimes();

        super.setProperty("LED1_Runtime", ledRuntime[0]);
        super.setProperty("Software_Version", softwareVersion);
        super.setProperty("External_IP", externalIP);
        super.setProperty("Internal_IP", internalIP);
        super.setProperty("Current_Mode", currentMode);
        super.setProperty("LED2_Runtime", ledRuntime[1]);
        super.setProperty("LED3_Runtime", ledRuntime[2]);
        super.setProperty("Connected_Runtime", Connected_Runtime);
        super.setProperty("Last_Location", location);
        super.updateSubscribedProperties(1000);
    }

    // GC
    public static void initializeGPIO() {

        for (int i = 0; i < size; i++) {

            buttonPressed[i] = false;
        }

        gpio = GpioFactory.getInstance();

        led[0] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "LED1", PinState.HIGH);
        led[1] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "LED2", PinState.HIGH);
        led[2] = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "LED3", PinState.HIGH);
        connected = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27, "ConnectedLED", PinState.HIGH);
        statusCom = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_26, "StatusCommunicationLED", PinState.HIGH);

        button[0] = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_DOWN);
        button[1] = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.PULL_DOWN);
        button[2] = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);
        startButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_DOWN);

    }

    //GC
    public void addListeners() {
        //initialize button listeners

        /* The idea was to put these in a loop. But there's some permission
         problems with that. Can't access index variable from inside class (try it)
         */
        button[0].addListener(new GpioPinListenerDigital() {

            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    buttonPressed[0] = true;
                }

            }
        });
        button[1].addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    buttonPressed[1] = true;
                }
            }
        });
        button[2].addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    buttonPressed[2] = true;
                }
            }
        });

        startButton.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState() == PinState.HIGH) {
                    startPressed = true;
                }
            }
        });

    }

    //GC
    public static void removeListeners() {
        //remove button listeners

        for (int i = 0; i < button.length; i++) {
            button[i].removeAllListeners();
        }
    }

    //GC
    public static void clearPressedFlags() {
        for (int i = 0; i < buttonPressed.length; i++) {
            buttonPressed[i] = false;
        }
        startPressed = false;
    }

    //GC
    public static void ShutdownGPIO() throws Exception {

        led[0].setShutdownOptions(true, PinState.HIGH);
        led[1].setShutdownOptions(true, PinState.HIGH);
        led[2].setShutdownOptions(true, PinState.HIGH);
        connected.setShutdownOptions(true, PinState.HIGH);
        statusCom.setShutdownOptions(true, PinState.LOW);
        removeListeners();

        for (int i = 0; i < led.length; i++) {
            gpio.unprovisionPin(led[i]);
            gpio.unprovisionPin(button[i]);
        }
        gpio.shutdown();
    }

    //GC
    public static void statusBlink() throws Exception {
        statusCom.high();
        try {
            statusCom.low();
            Thread.sleep(200);
            statusCom.high();
            Thread.sleep(200);

        } catch (InterruptedException ex) {
            throw new Exception("GameController.statusBlink() Exception: " + ex.toString());
        }

    }

    public final void getRuntimes() throws Exception {
        try {

            ledRuntime[0] = Double.parseDouble(readProperty("LED1"));
            ledRuntime[1] = Double.parseDouble(readProperty("LED2"));
            ledRuntime[2] = Double.parseDouble(readProperty("LED3"));
            Connected_Runtime = Double.parseDouble(readProperty("Connected"));

        } catch (Exception e) {
            GameClient.appendToLogFile("There was an error in getRuntimes: " + e.toString());
        }
    }

    public final void saveRuntimes() throws Exception {
        try {
            saveProperty("LED1", Double.toString(ledRuntime[0]));
            saveProperty("LED2", Double.toString(ledRuntime[1]));
            saveProperty("LED3", Double.toString(ledRuntime[2]));
            saveProperty("Connected", Double.toString(Connected_Runtime));

        } catch (Exception e) {
            GameClient.appendToLogFile("There was an error in saveRuntimes: " + e.toString());
        }

    }

    public static void partyLights() {
        if (currentMode == "Ready" || currentMode=="Startup") {
            currentMode = "Party";
            clearPressedFlags();
            Random leds = new Random();
            int led2 = 4;
            int led0 = 0;

            Random duration = new Random();
            int minTime = 150;
            int maxTime = 225;

            Random spins = new Random();
            int min = 1;
            int max = 4;

            int randomLED = leds.nextInt((led2 - led0) + 1) + led0;
            int randomDuration;
            int randomSpins;
            int lastLED = randomLED;

            while (!startPressed && !GameClient.quit) {
                GameClient.appendToLogFile("startpressed in party: "+startPressed);
                try {
                    // nextInt is normally exclusive of the top value,
                    // so add 1 to make it inclusive
                    randomLED = leds.nextInt((led2 - led0) + 1) + led0;
                    randomDuration = duration.nextInt((maxTime - minTime) + 1) + minTime;

                    if (randomLED < 3 && randomLED == lastLED) {
                        randomLED = leds.nextInt((led2 - led0) + 1) + led0;
                    }

                    if (randomLED < 3) {
                        led[randomLED].low();
                        Thread.sleep(randomDuration);
                        led[randomLED].high();
                        Thread.sleep(randomDuration);
                    }
                    if (randomLED == 3) {
                        randomSpins = spins.nextInt((max - min) + 1) + min;
                        for (int j = 0; j < randomSpins; j++) {
                            for (int i = 0; i < size; i++) {
                                led[i].low();
                                Thread.sleep(60);
                                led[i].high();
                                Thread.sleep(60);

                            }
                        }

                    }
                    if (randomLED == 4) {
                        randomSpins = spins.nextInt((max - min) + 1) + min;
                        for (int j = 0; j < randomSpins; j++) {
                            for (int i = size - 1; i > -1; i--) {
                                led[i].low();
                                Thread.sleep(60);
                                led[i].high();
                                Thread.sleep(60);

                            }
                        }

                    }

                    lastLED = randomLED;
                } catch (InterruptedException ex) {
                    Logger.getLogger(GameController.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            currentMode = "Ready";
        } else {

        }

    }

    @ThingworxServiceDefinition(name = "PlayGame", description = "Play A Game")
    @ThingworxServiceResult(
            name = CommonPropertyNames.PROP_RESULT,
            baseType = "INFOTABLE",
            description = "",
            aspects = {"dataShape:DVDemo.GameData"}
    )
    public InfoTable PlayGame(
            @ThingworxServiceParameter(name = "Mode", description = "Game Mode", baseType = "STRING") String Mode,
            @ThingworxServiceParameter(name = "Players", description = "Number of Players", baseType = "INTEGER") Integer Players,
            @ThingworxServiceParameter(name = "Difficulty", description = "Difficulty", baseType = "INTEGER") Integer Difficulty,
            @ThingworxServiceParameter(name = "Iterations", description = "VS Iterations", baseType = "INTEGER") Integer Iterations) throws Exception {

        //check for blocking, so we don't interfere with other games happening
        //that should be fixed now, but its an added layer, no point in removing
        vsData = new InfoTable(getDataShapeDefinition("DVDemo.GameData"));
        try {
            difficulty = Difficulty;
            long reactionDelay = 1600 / difficulty;
            while (!("Ready".equals(currentMode) || "Startup".equals(currentMode))) {
                startPressed = true;
               // GameClient.appendToLogFile("I wasn't ready but now I am!!!!: "+startPressed);

            }

            if (!blocking) {
                if ("VS".equals(Mode)) {
                    currentMode = "VS";
                    clearPressedFlags();
                    int players = Players;
                    VSrounds = Iterations;
                    HeadToHead game = new HeadToHead();
                    game.start(reactionDelay, players);
                    processScanRequest();

                } else if ("OnePlayer".equals(Mode)) {
                    currentMode = "1Player";
                    clearPressedFlags();
                    OnePlayer game = new OnePlayer();
                    game.start(reactionDelay, 1);
                    processScanRequest();

                }
            }
        } catch (Exception ex) {
            GameClient.appendToLogFile("There was a problem running GameController.PlayGame(): " + ex.toString());
        }
        currentMode = "Ready";
        return vsData;

    }

    @ThingworxServiceDefinition(name = "Blink", description = "Make the Lights Do Something")
    @ThingworxServiceResult(
            name = CommonPropertyNames.PROP_RESULT,
            baseType = "NOTHING",
            description = ""
    )

    public void Blink(
            @ThingworxServiceParameter(name = "Duration", description = "", baseType = "INTEGER") Integer Duration,
            @ThingworxServiceParameter(name = "Blinks", description = "", baseType = "INTEGER") Integer Blinks,
            @ThingworxServiceParameter(name = "LEDs", description = "", baseType = "INTEGER") Integer LEDs) throws Exception {
        currentMode = "Blink";

        for (int j = 0; j < Blinks; j++) {
            for (int i = 0; i < LEDs; i++) {
                led[i].low();
            }
            Thread.sleep(Duration);
            for (int i = 0; i < LEDs; i++) {
                led[i].high();
            }
            Thread.sleep(Duration);
        }

        currentMode = "Ready";
    }

    @ThingworxServiceDefinition(name = "Party", description = "Disco Lights")
    @ThingworxServiceResult(
            name = CommonPropertyNames.PROP_RESULT,
            baseType = "NOTHING",
            description = ""
    )

    public void Party() throws Exception {

        partyLights();

    }

    @ThingworxServiceDefinition(name = "cmdExec", description = "Run a command line process")
    @ThingworxServiceResult(
            name = CommonPropertyNames.PROP_RESULT,
            baseType = "STRING",
            description = ""
    )

    public static String cmdExec(@ThingworxServiceParameter(name = "cmdLine", description = "command to execute", baseType = "STRING") String cmdLine) {
        String line;
        String output = "";
        try {
            Process p = Runtime.getRuntime().exec(cmdLine);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                output += (line + '\n');
            }
            input.close();
        } catch (Exception ex) {
            GameClient.appendToLogFile("Problem executing command line: " + ex.toString());
        }
        return output;
    }

    @ThingworxServiceDefinition(name = "SetRuntime", description = "Set Runtime Values from TW")
    @ThingworxServiceResult(
            name = CommonPropertyNames.PROP_RESULT,
            baseType = "NOTHING",
            description = ""
    )
    public void SetRuntime(
            @ThingworxServiceParameter(name = "Property", description = "", baseType = "STRING") String property,
            @ThingworxServiceParameter(name = "Value", description = "", baseType = "NUMBER") Double value) throws Exception {

        try {
            if (property.equals("LED1")) {
                ledRuntime[0]=value;

            } else if (property.equals("LED2")) {
                ledRuntime[1]=value;

            } else if (property.equals("LED3")) {
                ledRuntime[2]=value;

            } else if (property.equals("Connected")) {
                Connected_Runtime=value;

            }
            else {
                throw new Exception("Incorrect property name: "+ property);
            }

        } catch (Exception e) {
            GameClient.appendToLogFile("There was an error in SetRuntimes: " + e.toString());
        }

    }

}
