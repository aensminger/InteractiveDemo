package demo;

/*
 * Distribution 1.01
 */
import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.common.SecurityClaims;
import com.thingworx.types.primitives.LocationPrimitive;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Alex
 */
public class GameClient extends ConnectedThingClient {

    private static String SN;
    private static String ThingName;
    private static String appKey;
    private static String host;
    Double start_runtime;
    Double end_runtime;
    private static final int max_logLines = 10000;
    public static boolean runAgain = true;
    public static boolean quit = false;
    public static boolean reconnect = true;
    public static boolean restart = true;

    public GameClient(ClientConfigurator config) throws Exception {
        super(config);

    }

    public static void getConnectionInfo() {
        FileInputStream fis = null;
        try {
            //grab these parameteres from the config file
            File fin = new File("/opt/InteractiveDemo/config.txt");
            fis = new FileInputStream(fin);
            //Construct BufferedReader from InputStreamReader
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            SN = br.readLine(); //SN
            appKey = br.readLine(); //appKey
            host = br.readLine(); //host
            ThingName = "DVDemo1.Device." + SN;
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GameClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GameClient.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(GameClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public static void get_location() {

        try {
            //connect to api
            String host = "http://ip-api.com/json";
            CloseableHttpClient httpclient = HttpClientBuilder.create().build();
            HttpGet getRequest = new HttpGet(host);
            HttpResponse httpResponse = httpclient.execute(getRequest);
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(httpResponse.getEntity().getContent()));

            //read result, pass into JSON object
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            //parse json
            JSONObject obj = new JSONObject(result.toString());
            double lat = obj.getDouble("lat");
            double lon = obj.getDouble("lon");
            String externalIP=obj.getString("query");
            GameController.externalIP=externalIP;

            LocationPrimitive loc = new LocationPrimitive(lat, lon, 0.0);

            GameController.location = loc;

            //Check the Error Code
            Integer StatusCode = httpResponse.getStatusLine().getStatusCode();

            if (StatusCode == 401) {
                throw new RuntimeException("Unauthorized");

            } else if (StatusCode != 200) {
                throw new RuntimeException("Failed with HTTP error code : " + StatusCode);
            }

        } catch (IOException ex) {
            appendToLogFile("IOException in get_location: " + ex.toString());
        } catch (JSONException ex) {
            appendToLogFile("JSONException in get_location: " + ex.toString());
        } catch (RuntimeException ex) {
            appendToLogFile("RuntimeException in get_location: " + ex.toString());
        }

    }
    
    public static void getLocalIP() {
        try {
            String command ="ifconfig eth0";
            String output ="";
            Process p = Runtime.getRuntime().exec(command);
             BufferedReader input = new BufferedReader
            (new InputStreamReader(p.getInputStream()));
             //read the first one, and leave it
             input.readLine();
             //take the second one in
             output=input.readLine();
             input.close();
             //this will look like this: internalIP is:inet addr:10.0.0.31  Bcast:10.0.0.255  Mask:255.255.255.0
             //now parse it to grab the inet addr
             
             int start=output.indexOf("inet addr:")+10;
             if (start>10) {
             int end=output.indexOf("Bcast:");
             output=output.substring(start, end);
             output.replaceAll("\\s+","");
             String internalIP=output;
             GameController.internalIP=internalIP;
             }
            
            
        } catch (IOException ex) {
            appendToLogFile("There was an issue in getLocalIP: "+ex.toString());
        }
    }

    //this is basically a "CheckInternetConnection", but more specific
    public static boolean isConnectedtoTW(boolean checkThingConnection) throws JSONException {
        boolean returnThis;
        try {
            /*
             maybe this should read the isConnected variable for full effect? 
             also, this is weirdly redundant but the client.isConnected() doesn't
             work for my application for some reason.
             */

            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(7 * 1000).build();
            CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            HttpHost target = new HttpHost(host, 443, "https");

            String URL = "/Thingworx/Things/" + ThingName + "/Properties/isConnected";
            HttpGet getRequest = new HttpGet(URL);
            getRequest.addHeader("Accept", "application/json-compressed");
            getRequest.addHeader("appKey", appKey);
            getRequest.addHeader("Content-Type", "application/json");

            HttpResponse httpResponse = httpclient.execute(target, getRequest);

            int responseCode = httpResponse.getStatusLine().getStatusCode();

            //if the code is anything but 200, it will return false
            returnThis = (responseCode == 200);
            if (checkThingConnection) {

                JSONObject json = new JSONObject(EntityUtils.toString(httpResponse.getEntity(), "UTF-8"));
                String value = json.getJSONArray("rows").getJSONObject(0).get("_0").toString();
                if (value == "true") {
                    returnThis = true;
                } else {
                    returnThis = false;
                }

            }
            return returnThis;
        } catch (IOException ex) {
            //this may happen in a timeout scenario. either way, false return
            return false;

        }

    }

    public static void appendToLogFile(String entry) {
        try {
            File fout = new File("/opt/InteractiveDemo/logs/ClientLog.txt");
            boolean append = checkLogLines(fout);
            FileWriter fos = new FileWriter(fout, append);
            final BufferedWriter log = new BufferedWriter(fos);
            Date d = new Date();
            log.write(d.toString() + entry);
            log.newLine();
            log.close();
        } catch (Exception ex) {
            System.out.println("There was an error writing to the log: ");
            ex.printStackTrace();
        }

    }

    public static boolean checkLogLines(File fout) throws Exception {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fout));
            int lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            reader.close();
            boolean append = true;
            if (lines > max_logLines) {
                append = false;
            }
            return append;
        } catch (FileNotFoundException ex) {
            throw new Exception(ex.getCause());
        } catch (IOException ex) {
            throw new Exception(ex.getCause());
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                throw new Exception(ex.getCause());
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {


        while (restart) {
            //open up the log file and wrap everything in a try-catch
            getConnectionInfo();
            
            String URI = "wss://" + host + ":443/Thingworx/WS";
            int scanRate = 1000;
            try {


                appendToLogFile("----Opening Client");
                //set config file and initialize
                ClientConfigurator config = new ClientConfigurator();
                config.setUri(URI);
                config.setReconnectInterval(15);
                config.setConnectTimeout(5 * 60 * 1000); //i should look at this guy
                SecurityClaims claims = SecurityClaims.fromAppKey(appKey);
                config.setSecurityClaims(claims);
                config.setName("GameGateway");
                config.setAsSDKType();
                //OUTER:

                final GameClient client = new GameClient(config);
                HashMap<String, String> virtualDirs = new HashMap<String, String>();
                virtualDirs.put("logs", File.separator+"opt"+File.separator+"InteractiveDemo"+ File.separator + "logs");
                virtualDirs.put("home", File.separator+"opt"+File.separator+"InteractiveDemo");
                virtualDirs.put("init.d", File.separator+"etc"+File.separator+"init.d");


                final GameController gc = new GameController(ThingName, "Interactive Demo", client, virtualDirs);
                
                client.bindThing(gc);

                get_location();
                getLocalIP();
                

                //start client
                client.start();
                GameController.clearPressedFlags();
                //try to connect to ThWx, if not, bail after timeout,
                long now = System.currentTimeMillis();
                long timeout = now + 20000; //2 min
                //client.checkConnection();
                while (!client.isConnected() && now < timeout) {
                    GameController.statusBlink();
                    now = System.currentTimeMillis();
                }
                //read runtimes from files
                gc.getRuntimes();
                GameController.currentMode="Startup";

            //main "loop" of sorts
                while (!quit) {
                    //set up shutdown hook for clean shutdown
                    final Thread mainThread = Thread.currentThread();
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            quit = true;
                            reconnect = false;
                            restart=false;
                            appendToLogFile("The program has been asked to quit.");
                            try {
                                GameController.connected.high();
                                GameController.statusCom.low();
                                GameController.currentMode="Offline";
                                gc.scanDevice();
                                GameController.ShutdownGPIO();
                                mainThread.join();

                            } catch (Exception ex) {
                                appendToLogFile("The client has been shut down: " + ex.toString());
                            }

                        }
                    });

                    while (client.isConnected() && !client.isShutdown() && !quit) {
                        now = System.currentTimeMillis();
                        //turn on connection indicator LED
                        GameController.connected.low();

                        //if the connection breaks at any point, break out and quit
                        if (!isConnectedtoTW(false)) {
                            GameController.connected.high();
                            reconnect=true;
                            break;
                        }
                        
                        Thread.sleep(scanRate);

                        //update box RunTime
                        long runtime = System.currentTimeMillis() - now;
                        Double runtime1 = ((double) runtime) / 1000 / 60; //convert to minutes
                        GameController.Connected_Runtime += runtime1;
                        gc.scanDevice();
                    }
                    if (quit) {
                        GameController.statusCom.high();
                        GameController.connected.high();
                       reconnect=false;
                    }
                    while (reconnect) {
                        appendToLogFile("Failed Connection: Trying to reconnnect.");
                        GameController.statusBlink();
                        if (isConnectedtoTW(false)) {
                            appendToLogFile("Connection Back Up. Re-binding and syncing.");
                            gc.synchronizeState();
                            reconnect = false;
                        }
                    }
                    if (!quit) {
                        client.connect();
                    }

                }

            } catch (Exception ex) {
                appendToLogFile("There was an issue in MAIN: " + ex.toString());
            }
            GameController.statusCom.high();
        }
    }
}
