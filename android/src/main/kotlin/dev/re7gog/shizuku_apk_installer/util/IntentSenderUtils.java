package dev.re7gog.shizuku_apk_installer.util;

import android.content.IIntentSender;
import android.content.IntentSender;

import java.lang.reflect.InvocationTargetException;

public class IntentSenderUtils {

    @SuppressWarnings({"JavaReflectionMemberAccess"})
    public static IntentSender newInstance(IIntentSender binder) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        return IntentSender.class.getConstructor(IIntentSender.class).newInstance(binder);
    }
}
