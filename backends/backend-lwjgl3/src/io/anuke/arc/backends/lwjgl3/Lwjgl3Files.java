package io.anuke.arc.backends.lwjgl3;

import io.anuke.arc.Files;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.*;

import java.io.File;

/**
 * @author mzechner
 * @author Nathan Sweet
 */
public final class Lwjgl3Files implements Files{
    public static final String externalPath = System.getProperty("user.home") + File.separator;
    public static final String localPath = new File("").getAbsolutePath() + File.separator;

    @Override
    public FileHandle getFileHandle(String fileName, FileType type){
        return new Lwjgl3FileHandle(fileName, type);
    }

    @Override
    public FileHandle classpath(String path){
        return new Lwjgl3FileHandle(path, FileType.Classpath);
    }

    @Override
    public FileHandle internal(String path){
        return new Lwjgl3FileHandle(path, FileType.Internal);
    }

    @Override
    public FileHandle external(String path){
        return new Lwjgl3FileHandle(path, FileType.External);
    }

    @Override
    public FileHandle absolute(String path){
        return new Lwjgl3FileHandle(path, FileType.Absolute);
    }

    @Override
    public FileHandle local(String path){
        return new Lwjgl3FileHandle(path, FileType.Local);
    }

    @Override
    public String getExternalStoragePath(){
        return externalPath;
    }

    @Override
    public boolean isExternalStorageAvailable(){
        return true;
    }

    @Override
    public String getLocalStoragePath(){
        return localPath;
    }

    @Override
    public boolean isLocalStorageAvailable(){
        return true;
    }

    /**
     * @author mzechner
     * @author Nathan Sweet
     */
    public static final class Lwjgl3FileHandle extends FileHandle{
        public Lwjgl3FileHandle(String fileName, FileType type){
            super(fileName, type);
        }

        public Lwjgl3FileHandle(File file, FileType type){
            super(file, type);
        }

        public FileHandle child(String name){
            if(file.getPath().length() == 0) return new Lwjgl3FileHandle(new File(name), type);
            return new Lwjgl3FileHandle(new File(file, name), type);
        }

        public FileHandle sibling(String name){
            if(file.getPath().length() == 0) throw new ArcRuntimeException("Cannot get the sibling of the root.");
            return new Lwjgl3FileHandle(new File(file.getParent(), name), type);
        }

        public FileHandle parent(){
            File parent = file.getParentFile();
            if(parent == null){
                if(type == FileType.Absolute)
                    parent = new File("/");
                else
                    parent = new File("");
            }
            return new Lwjgl3FileHandle(parent, type);
        }

        public File file(){
            if(type == FileType.External) return new File(externalPath, file.getPath());
            if(type == FileType.Local) return new File(localPath, file.getPath());
            return file;
        }
    }
}
