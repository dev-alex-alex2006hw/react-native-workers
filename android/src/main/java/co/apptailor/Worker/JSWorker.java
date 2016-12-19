package co.apptailor.Worker;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Random;

import co.apptailor.Worker.core.ReactContextBuilder;
import co.apptailor.Worker.core.WorkerSelfModule;

public class JSWorker {
    private String id;

    private String jsSlugname;
    private ReactApplicationContext reactContext;

    public JSWorker(String jsSlugname){
        this(jsSlugname, true);
    }

    public JSWorker(String jsSlugname, Boolean random) {
        if (random){
            this.id = jsSlugname + Math.abs(new Random().nextInt());
        }
        else{
          this.id = jsSlugname + "0";
        }
        this.jsSlugname = jsSlugname;
    }

    public String getWorkerId() {
        return this.id;
    }

    public String getName() {
        return jsSlugname;
    }

    public void runFromContext(ReactApplicationContext context, ReactContextBuilder reactContextBuilder) throws Exception {
        if (reactContext != null) {
            return;
        }

        reactContext = reactContextBuilder.build();

        WorkerSelfModule workerSelfModule = reactContext.getNativeModule(WorkerSelfModule.class);
        workerSelfModule.initialize(id, context);
    }

    public void postMessage(String message) {
        if (reactContext != null) {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("WorkerMessage", message);
        }
    }

    public void onHostResume() {
        if (reactContext != null) {
            reactContext.onHostResume(null);
        }
    }

    public void onHostPause() {
        if (reactContext != null) {
            reactContext.onHostPause();
        }
    }

    public void terminate() {
        if (reactContext != null) {
            reactContext.onHostPause();
            reactContext.destroy();
            reactContext = null;
        }
    }
}
