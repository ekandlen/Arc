package io.anuke.arc.assets.loaders.resolvers;

import io.anuke.arc.Core;
import io.anuke.arc.assets.loaders.FileHandleResolver;
import io.anuke.arc.backends.gwt.GwtFileHandle;
import io.anuke.arc.files.FileHandle;

public class ResolutionFileResolver implements FileHandleResolver{
    protected final FileHandleResolver baseResolver;
    protected final Resolution[] descriptors;
    public ResolutionFileResolver(FileHandleResolver baseResolver, Resolution... descriptors){
        this.baseResolver = baseResolver;
        this.descriptors = descriptors;
    }

    public static Resolution choose(Resolution... descriptors){
        int width = 0;
        if(Core.graphics.getWidth() > Core.graphics.getHeight()){
            width = Core.graphics.getHeight();
        }else{
            width = Core.graphics.getWidth();
        }

        Resolution bestDesc = null;
        // Find lowest.
        int best = Integer.MAX_VALUE;
        for(int i = 0, n = descriptors.length; i < n; i++){
            if(descriptors[i].portraitWidth < best){
                best = descriptors[i].portraitWidth;
                bestDesc = descriptors[i];
            }
        }
        // Find higher, but not over the screen res.
        best = Integer.MAX_VALUE;
        for(int i = 0, n = descriptors.length; i < n; i++){
            if(descriptors[i].portraitWidth <= width){
                best = descriptors[i].portraitWidth;
                bestDesc = descriptors[i];
            }
        }
        return bestDesc;
    }

    @Override
    public FileHandle resolve(String fileName){
        Resolution bestDesc = choose(descriptors);
        FileHandle originalHandle = new GwtFileHandle(fileName);
        FileHandle handle = baseResolver.resolve(resolve(originalHandle, bestDesc.folder));
        if(!handle.exists()) handle = baseResolver.resolve(fileName);
        return handle;
    }

    protected String resolve(FileHandle originalHandle, String suffix){
        return originalHandle.parent() + "/" + suffix + "/" + originalHandle.name();
    }

    public static class Resolution{
        public final int portraitWidth;
        public final int portraitHeight;
        public final String folder;

        public Resolution(int portraitWidth, int portraitHeight, String folder){
            this.portraitWidth = portraitWidth;
            this.portraitHeight = portraitHeight;
            this.folder = folder;
        }
    }
}
