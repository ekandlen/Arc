package io.anuke.arc.backends.android.surfaceview;

import android.content.res.AssetFileDescriptor;
import io.anuke.arc.Core;
import io.anuke.arc.Files.FileType;
import io.anuke.arc.backends.android.surfaceview.ZipResourceFile.ZipEntryRO;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.ArcRuntimeException;

import java.io.*;

/** @author sarkanyi */
public class AndroidZipFileHandle extends AndroidFileHandle{
    private AssetFileDescriptor assetFd;
    private ZipResourceFile expansionFile;
    private String path;

    public AndroidZipFileHandle(String fileName){
        super(null, fileName, FileType.Internal);
        initialize();
    }

    public AndroidZipFileHandle(File file, FileType type){
        super(null, file, type);
        initialize();
    }

    private void initialize(){
        path = file.getPath().replace('\\', '/');
        expansionFile = ((AndroidFiles)Core.files).getExpansionFile();
        assetFd = expansionFile.getAssetFileDescriptor(getPath());

        // needed for listing entries and exists() of directories
        if(isDirectory())
            path += "/";
    }

    @Override
    public AssetFileDescriptor getAssetFileDescriptor(){
        return assetFd;
    }

    private String getPath(){
        return path;
    }

    @Override
    public InputStream read(){
        InputStream input = null;

        try{
            input = expansionFile.getInputStream(getPath());
        }catch(IOException ex){
            throw new ArcRuntimeException("Error reading file: " + file + " (ZipResourceFile)", ex);
        }
        return input;
    }

    @Override
    public FileHandle child(String name){
        if(file.getPath().length() == 0)
            return new AndroidZipFileHandle(new File(name), type);
        return new AndroidZipFileHandle(new File(file, name), type);
    }

    @Override
    public FileHandle sibling(String name){
        if(file.getPath().length() == 0)
            throw new ArcRuntimeException("Cannot get the sibling of the root.");
        return Core.files.getFileHandle(new File(file.getParent(), name).getPath(), type); //this way we can find the sibling even if it's not inside the obb
    }

    @Override
    public FileHandle parent(){
        File parent = file.getParentFile();
        if(parent == null)
            parent = new File("");
        return new AndroidZipFileHandle(parent.getPath());
    }

    @Override
    public FileHandle[] list(){
        ZipEntryRO[] zipEntries = expansionFile.getEntriesAt(getPath());
        FileHandle[] handles = new FileHandle[zipEntries.length];
        for(int i = 0, n = handles.length; i < n; i++)
            handles[i] = new AndroidZipFileHandle(zipEntries[i].mFileName);
        return handles;
    }

    @Override
    public FileHandle[] list(FileFilter filter){
        ZipEntryRO[] zipEntries = expansionFile.getEntriesAt(getPath());
        FileHandle[] handles = new FileHandle[zipEntries.length];
        int count = 0;
        for(int i = 0, n = handles.length; i < n; i++){
            FileHandle child = new AndroidZipFileHandle(zipEntries[i].mFileName);
            if(!filter.accept(child.file()))
                continue;
            handles[count] = child;
            count++;
        }
        if(count < zipEntries.length){
            FileHandle[] newHandles = new FileHandle[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    @Override
    public FileHandle[] list(FilenameFilter filter){
        ZipEntryRO[] zipEntries = expansionFile.getEntriesAt(getPath());
        FileHandle[] handles = new FileHandle[zipEntries.length];
        int count = 0;
        for(int i = 0, n = handles.length; i < n; i++){
            String path = zipEntries[i].mFileName;
            if(!filter.accept(file, path))
                continue;
            handles[count] = new AndroidZipFileHandle(path);
            count++;
        }
        if(count < zipEntries.length){
            FileHandle[] newHandles = new FileHandle[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    @Override
    public FileHandle[] list(String suffix){
        ZipEntryRO[] zipEntries = expansionFile.getEntriesAt(getPath());
        FileHandle[] handles = new FileHandle[zipEntries.length];
        int count = 0;
        for(int i = 0, n = handles.length; i < n; i++){
            String path = zipEntries[i].mFileName;
            if(!path.endsWith(suffix))
                continue;
            handles[count] = new AndroidZipFileHandle(path);
            count++;
        }
        if(count < zipEntries.length){
            FileHandle[] newHandles = new FileHandle[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    @Override
    public boolean isDirectory(){
        return assetFd == null;
    }

    @Override
    public long length(){
        return assetFd != null ? assetFd.getLength() : 0;
    }

    @Override
    public boolean exists(){
        return assetFd != null || expansionFile.getEntriesAt(getPath()).length != 0;
    }
}
