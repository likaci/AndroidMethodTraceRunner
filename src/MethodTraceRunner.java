import com.android.ddmlib.*;
import org.apache.commons.cli.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by likaci on 29/09/2017
 */
public class MethodTraceRunner {
    private static final String TAG = "MethodTraceRunner";
    private static String packageName;
    private static String outputFile;
    private static int traceDuration;

    public static void main(String[] args) throws IOException {
        parseArgs(args);

        //bridge
        AndroidDebugBridge.init(true);
        AndroidDebugBridge bridge = AndroidDebugBridge.createBridge();

        //device
        waitForDevice(bridge);
        IDevice[] devices = bridge.getDevices();
        if (devices.length > 1) {
            exitWithError("more than one device");
        } else if (devices.length == 0) {
            exitWithError("no device found");
        }
        IDevice device = devices[0];
        Log.d(TAG, "device: " + device);

        //client
        waitForClient(device, packageName);
        Client client = device.getClient(packageName);
        Log.d(TAG, "client: " + client);

        ClientData.setMethodProfilingHandler(new ClientData.IMethodProfilingHandler() {
            @Override
            public void onSuccess(String s, Client client) {
                Log.d(TAG, "onSuccess: " + s + " " + client);
            }

            @Override
            public void onSuccess(byte[] bytes, Client client) {
                Log.d(TAG, "onSuccess: " + client);

                BufferedOutputStream bs = null;

                try {
                    FileOutputStream fs = new FileOutputStream(new File(outputFile));
                    bs = new BufferedOutputStream(fs);
                    bs.write(bytes);
                    bs.close();
                    bs = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (bs != null) try {
                    bs.close();
                } catch (Exception ignored) {
                }

                Log.d(TAG, "trace file: " + outputFile);
                System.out.println("trace file saved at " + System.getProperty("user.dir") + File.separator + outputFile);
                System.exit(0);
            }

            @Override
            public void onStartFailure(Client client, String s) {
                exitWithError("onStartFailure: " + client + " " + s);
            }

            @Override
            public void onEndFailure(Client client, String s) {
                exitWithError("onEndFailure: " + client + " " + s);
            }

        });

        System.out.printf("will profile %s, device: %s, client: %s, for %d seconds%n", packageName, device, client, traceDuration);
        Log.d(TAG, "start Sampling Profiler");
        client.startSamplingProfiler(10, TimeUnit.MILLISECONDS);

        try {
            Log.d(TAG, "wait " + traceDuration + " seconds");
            Thread.sleep(traceDuration * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "stop Sampling Profiler");
        client.stopSamplingProfiler();
    }

    private static void parseArgs(String[] args) {
        Log.d(TAG, "args: " + Arrays.toString(args));

        Options options = new Options();
        options.addOption("h", false, "show help msg");
        options.addOption("v", false, "verbose log");
        options.addOption("p", true, "package name");
        options.addOption("o", true, "output file");
        options.addOption("t", true, "trace time");

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);

            if (cmd.hasOption("h")) {
                printHelpMsg(options);
                System.exit(0);
            }

            if (cmd.hasOption("v")) {
                DdmPreferences.setLogLevel(Log.LogLevel.DEBUG.getStringValue());
            }

            packageName = cmd.getOptionValue("p", "");
            if (packageName.isEmpty()) {
                exitWithError("PackageName not specified, use -p arg to set");
            }
            Log.i(TAG, "packageName: " + packageName);

            outputFile = cmd.getOptionValue("o", "t.trace");
            Log.i(TAG, "outputFile: " + outputFile);

            traceDuration = Integer.parseInt(cmd.getOptionValue("t", "5"));
            Log.d(TAG, "traceDuration: " + traceDuration);

        } catch (Exception e) {
            printHelpMsg(options);
            exitWithError(e);
        }

    }

    private static void printHelpMsg(Options options) {
        new HelpFormatter().printHelp("mtr", options);
    }

    private static void waitForDevice(AndroidDebugBridge bridge) {
        int count = 0;
        while (!bridge.hasInitialDeviceList()) {
            try {
                Thread.sleep(100);
                count++;
            } catch (InterruptedException ignored) {
            }
            if (count > 100) {
                exitWithError("wait for device time out");
                break;
            }
        }
    }

    private static void waitForClient(IDevice device, String packageName) {
        int count = 0;
        while (device.getClient(packageName) == null) {
            try {
                Thread.sleep(100);
                count++;
            } catch (InterruptedException ignored) {
            }
            if (count > 100) {
                exitWithError("wait for client time out");
                break;
            }
        }
    }

    private static void exitWithError(String msg) {
        exitWithError(new Exception(msg));
    }

    private static void exitWithError(Exception exception) {
        if (exception != null) {
            exception.printStackTrace();
        }
        System.exit(1);
    }

}
