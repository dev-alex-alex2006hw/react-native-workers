package co.apptailor.Worker.core;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

public class UIManagerStubModule extends ReactContextBaseJavaModule {

    public UIManagerStubModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }
    @ReactNative
    @Override
    public String getName() {
        return "UIManager";
    }
}
