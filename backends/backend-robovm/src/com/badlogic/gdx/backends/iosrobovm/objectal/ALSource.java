package com.badlogic.gdx.backends.iosrobovm.objectal;

import org.robovm.apple.foundation.NSObject;
import org.robovm.objc.ObjCRuntime;
import org.robovm.objc.annotation.Method;
import org.robovm.objc.annotation.NativeClass;
import org.robovm.objc.annotation.Property;
import org.robovm.rt.bro.annotation.Library;

/**
 * @author Niklas Therning
 */
@Library(Library.INTERNAL)
@NativeClass
public class ALSource extends NSObject{

    static{
        ObjCRuntime.bind(ALSource.class);
    }

    @Property(selector = "sourceId")
    public native int getSourceId();

    @Property(selector = "state")
    public native int getState();

    @Method(selector = "stop")
    public native void stop();

    @Property(selector = "paused")
    public native boolean isPaused();

    @Property(selector = "setPaused:")
    public native void setPaused(boolean paused);

    @Method(selector = "setVolume:")
    public native void setVolume(float volume);

    @Method(selector = "setPitch:")
    public native void setPitch(float pitch);

    @Method(selector = "setPan:")
    public native void setPan(float pan);

    @Method(selector = "setLooping:")
    public native void setLooping(boolean shouldLoop);

}
