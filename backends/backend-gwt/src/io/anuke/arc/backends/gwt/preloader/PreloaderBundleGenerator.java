package io.anuke.arc.backends.gwt.preloader;

import io.anuke.arc.backends.gwt.preloader.AssetFilter.AssetType;
import io.anuke.arc.util.ArcRuntimeException;
import com.google.gwt.core.ext.*;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Copies assets from the path specified in the modules gdx.assetpath configuration property to the war/ folder and generates the
 * assets.txt file. The type of a file is determined by an {@link AssetFilter}, which is either created by instantiating the class
 * specified in the gdx.assetfilterclass property, or falling back to the {@link DefaultAssetFilter}.
 * @author mzechner
 */
public class PreloaderBundleGenerator extends Generator{
    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName){
        System.out.println(new File(".").getAbsolutePath());
        String assetPath = getAssetPath(context);
        String assetOutputPath = getAssetOutputPath(context);
        if(assetOutputPath == null){
            assetOutputPath = "war/";
        }
        AssetFilter assetFilter = getAssetFilter(context);

        FileWrapper source = new FileWrapper(assetPath);
        if(!source.exists()){
            source = new FileWrapper("../" + assetPath);
            if(!source.exists())
                throw new RuntimeException("assets path '" + assetPath
                + "' does not exist. Check your gdx.assetpath property in your GWT project's module gwt.xml file");
        }
        if(!source.isDirectory())
            throw new RuntimeException("assets path '" + assetPath
            + "' is not a directory. Check your gdx.assetpath property in your GWT project's module gwt.xml file");
        System.out.println("Copying resources from " + assetPath + " to " + assetOutputPath);
        System.out.println(source.file.getAbsolutePath());
        FileWrapper target = new FileWrapper("assets/"); // this should always be the war/ directory of the GWT project.
        System.out.println(target.file.getAbsolutePath());
        if(!target.file.getAbsolutePath().replace("\\", "/").endsWith(assetOutputPath + "assets")){
            target = new FileWrapper(assetOutputPath + "assets/");
        }
        if(target.exists()){
            if(!target.deleteDirectory()) throw new RuntimeException("Couldn't clean target path '" + target + "'");
        }
        ArrayList<Asset> assets = new ArrayList<Asset>();
        copyDirectory(source, target, assetFilter, assets);

        // Now collect classpath files and copy to assets
        List<String> classpathFiles = getClasspathFiles(context);
        for(String classpathFile : classpathFiles){
            if(assetFilter.accept(classpathFile, false)){
                try{
                    InputStream is = context.getClass().getClassLoader().getResourceAsStream(classpathFile);
                    FileWrapper dest = target.child(classpathFile);
                    dest.write(is, false);
                    assets.add(new Asset(dest, assetFilter.getType(dest.path())));
                    is.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }

        HashMap<String, ArrayList<Asset>> bundles = new HashMap<String, ArrayList<Asset>>();
        for(Asset asset : assets){
            String bundleName = assetFilter.getBundleName(asset.file.path());
            if(bundleName == null){
                bundleName = "assets";
            }
            ArrayList<Asset> bundleAssets = bundles.get(bundleName);
            if(bundleAssets == null){
                bundleAssets = new ArrayList<Asset>();
                bundles.put(bundleName, bundleAssets);
            }
            bundleAssets.add(asset);
        }

        for(Entry<String, ArrayList<Asset>> bundle : bundles.entrySet()){
            StringBuilder sb = new StringBuilder();
            for(Asset asset : bundle.getValue()){
                String path = asset.file.path().replace('\\', '/').replace(assetOutputPath, "").replaceFirst("assets/", "");
                if(path.startsWith("/")) path = path.substring(1);
                sb.append(asset.type.code);
                sb.append(":");
                sb.append(path);
                sb.append(":");
                sb.append(asset.file.isDirectory() ? 0 : asset.file.length());
                sb.append(":");
                String mimetype = URLConnection.guessContentTypeFromName(asset.file.name());
                sb.append(mimetype == null ? "application/unknown" : mimetype);
                sb.append("\n");
            }
            target.child(bundle.getKey() + ".txt").writeString(sb.toString(), false);
        }
        return createDummyClass(logger, context);
    }

    private void copyFile(FileWrapper source, FileWrapper dest, AssetFilter filter, ArrayList<Asset> assets){
        if(!filter.accept(dest.path(), false)) return;
        try{
            assets.add(new Asset(dest, filter.getType(dest.path())));
            dest.write(source.read(), false);
        }catch(Exception ex){
            throw new ArcRuntimeException("Error copying source file: " + source + "\n" //
            + "To destination: " + dest, ex);
        }
    }

    private void copyDirectory(FileWrapper sourceDir, FileWrapper destDir, AssetFilter filter, ArrayList<Asset> assets){
        if(!filter.accept(destDir.path(), true)) return;
        assets.add(new Asset(destDir, AssetType.Directory));
        destDir.mkdirs();
        FileWrapper[] files = sourceDir.list();
        for(int i = 0, n = files.length; i < n; i++){
            FileWrapper srcFile = files[i];
            FileWrapper destFile = destDir.child(srcFile.name());
            if(srcFile.isDirectory())
                copyDirectory(srcFile, destFile, filter, assets);
            else
                copyFile(srcFile, destFile, filter, assets);
        }
    }

    private AssetFilter getAssetFilter(GeneratorContext context){
        ConfigurationProperty assetFilterClassProperty = null;
        try{
            assetFilterClassProperty = context.getPropertyOracle().getConfigurationProperty("gdx.assetfilterclass");
        }catch(BadPropertyValueException e){
            return new DefaultAssetFilter();
        }
        if(assetFilterClassProperty.getValues().size() == 0){
            return new DefaultAssetFilter();
        }
        String assetFilterClass = assetFilterClassProperty.getValues().get(0);
        if(assetFilterClass == null) return new DefaultAssetFilter();
        try{
            return (AssetFilter)Class.forName(assetFilterClass).newInstance();
        }catch(Exception e){
            throw new RuntimeException("Couldn't instantiate custom AssetFilter '" + assetFilterClass
            + "', make sure the class is public and has a public default constructor", e);
        }
    }

    private String getAssetPath(GeneratorContext context){
        ConfigurationProperty assetPathProperty = null;
        try{
            assetPathProperty = context.getPropertyOracle().getConfigurationProperty("gdx.assetpath");
        }catch(BadPropertyValueException e){
            throw new RuntimeException(
            "No gdx.assetpath defined. Add <set-configuration-property name=\"gdx.assetpath\" value=\"relative/path/to/assets/\"/> to your GWT projects gwt.xml file");
        }
        if(assetPathProperty.getValues().size() == 0){
            throw new RuntimeException(
            "No gdx.assetpath defined. Add <set-configuration-property name=\"gdx.assetpath\" value=\"relative/path/to/assets/\"/> to your GWT projects gwt.xml file");
        }
        String paths = assetPathProperty.getValues().get(0);
        if(paths == null){
            throw new RuntimeException(
            "No gdx.assetpath defined. Add <set-configuration-property name=\"gdx.assetpath\" value=\"relative/path/to/assets/\"/> to your GWT projects gwt.xml file");
        }else{
            ArrayList<String> existingPaths = new ArrayList<String>();
            String[] tokens = paths.split(",");
            for(String token : tokens){
                System.out.println(token);
                if(new FileWrapper(token).exists() || new FileWrapper("../" + token).exists()){
                    return token;
                }
            }
            throw new RuntimeException(
            "No valid gdx.assetpath defined. Fix <set-configuration-property name=\"gdx.assetpath\" value=\"relative/path/to/assets/\"/> in your GWT projects gwt.xml file");
        }
    }

    private String getAssetOutputPath(GeneratorContext context){
        ConfigurationProperty assetPathProperty = null;
        try{
            assetPathProperty = context.getPropertyOracle().getConfigurationProperty("gdx.assetoutputpath");
        }catch(BadPropertyValueException e){
            return null;
        }
        if(assetPathProperty.getValues().size() == 0){
            return null;
        }
        String paths = assetPathProperty.getValues().get(0);
        if(paths == null){
            return null;
        }else{
            ArrayList<String> existingPaths = new ArrayList<String>();
            String[] tokens = paths.split(",");
            String path = null;
            for(String token : tokens){
                if(new FileWrapper(token).exists() || new FileWrapper(token).mkdirs()){
                    path = token;
                }
            }
            if(path != null && !path.endsWith("/")){
                path += "/";
            }
            return path;
        }
    }

    private List<String> getClasspathFiles(GeneratorContext context){
        List<String> classpathFiles = new ArrayList<String>();
        try{
            ConfigurationProperty prop = context.getPropertyOracle().getConfigurationProperty("gdx.files.classpath");
            for(String value : prop.getValues()){
                classpathFiles.add(value);
            }
        }catch(BadPropertyValueException e){
            // Ignore
        }
        return classpathFiles;
    }

    private String createDummyClass(TreeLogger logger, GeneratorContext context){
        String packageName = "io.anuke.arc.backends.gwt.preloader";
        String className = "PreloaderBundleImpl";
        ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, className);
        composer.addImplementedInterface(packageName + ".PreloaderBundle");
        PrintWriter printWriter = context.tryCreate(logger, packageName, className);
        if(printWriter == null){
            return packageName + "." + className;
        }
        SourceWriter sourceWriter = composer.createSourceWriter(context, printWriter);
        sourceWriter.commit(logger);
        return packageName + "." + className;
    }

    private class Asset{
        FileWrapper file;
        AssetType type;

        public Asset(FileWrapper file, AssetType type){
            this.file = file;
            this.type = type;
        }
    }
}
