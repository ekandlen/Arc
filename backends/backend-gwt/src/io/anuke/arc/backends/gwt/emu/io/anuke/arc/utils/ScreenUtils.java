package io.anuke.arc.util;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.GL20;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.typedarrays.shared.ArrayBufferView;

import java.nio.ByteBuffer;
import java.nio.HasArrayBufferView;

/**
 * Class with static helper methods that provide access to the default OpenGL FrameBuffer. These methods can be used to get the
 * entire screen content or a portion thereof.
 * @author espitz
 */
public final class ScreenUtils{

    /**
     * Returns the default framebuffer contents as a {@link TextureRegion} with a width and height equal to the current screen
     * size. The base {@link Texture} always has {@link Mathf#nextPowerOfTwo} dimensions and RGBA8888 {@link Format}. It can be
     * accessed via {@link TextureRegion#getTexture}. The texture is not managed and has to be reloaded manually on a context loss.
     * The returned TextureRegion is flipped along the Y axis by default.
     */
    public static TextureRegion getFrameBufferTexture(){
        final int w = Core.graphics.getBackBufferWidth();
        final int h = Core.graphics.getBackBufferHeight();
        return getFrameBufferTexture(0, 0, w, h);
    }

    /**
     * Returns a portion of the default framebuffer contents specified by x, y, width and height as a {@link TextureRegion} with
     * the same dimensions. The base {@link Texture} always has {@link Mathf#nextPowerOfTwo} dimensions and RGBA8888
     * {@link Format}. It can be accessed via {@link TextureRegion#getTexture}. This texture is not managed and has to be reloaded
     * manually on a context loss. If the width and height specified are larger than the framebuffer dimensions, the Texture will
     * be padded accordingly. Pixels that fall outside of the current screen will have RGBA values of 0.
     * @param x the x position of the framebuffer contents to capture
     * @param y the y position of the framebuffer contents to capture
     * @param w the width of the framebuffer contents to capture
     * @param h the height of the framebuffer contents to capture
     */
    public static TextureRegion getFrameBufferTexture(int x, int y, int w, int h){
        final int potW = Mathf.nextPowerOfTwo(w);
        final int potH = Mathf.nextPowerOfTwo(h);

        final Pixmap pixmap = getFrameBufferPixmap(x, y, w, h);
        final Pixmap potPixmap = new Pixmap(potW, potH, Format.RGBA8888);
        potPixmap.drawPixmap(pixmap, 0, 0);
        Texture texture = new Texture(potPixmap);
        TextureRegion textureRegion = new TextureRegion(texture, 0, h, w, -h);
        potPixmap.dispose();
        pixmap.dispose();

        return textureRegion;
    }

    public static Pixmap getFrameBufferPixmap(int x, int y, int w, int h){
        Core.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

        final Pixmap pixmap = new Pixmap(w, h, Format.RGBA8888);
        ByteBuffer pixels = BufferUtils.newByteBuffer(h * w * 4);
        Core.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
        putPixelsBack(pixmap, pixels);
        return pixmap;
    }

    public static void putPixelsBack(Pixmap pixmap, ByteBuffer pixels){
        if(pixmap.getWidth() == 0 || pixmap.getHeight() == 0) return;
        putPixelsBack(((HasArrayBufferView)pixels).getTypedArray(), pixmap.getWidth(), pixmap.getHeight(), pixmap.getContext());

    }

    private native static void putPixelsBack(ArrayBufferView pixels, int width, int height, Context2d ctx)/*-{
		var imgData = ctx.createImageData(width, height);
		var data = imgData.data;

		for (var i = 0, len = width * height * 4; i < len; i++) {
			data[i] = pixels[i] & 0xff;
		}
		ctx.putImageData(imgData, 0, 0);
	}-*/;

    /**
     * Returns the default framebuffer contents as a byte[] array with a length equal to screen width * height * 4. The byte[]
     * will always contain RGBA8888 data. Because of differences in screen and image origins the framebuffer contents should be
     * flipped along the Y axis if you intend save them to disk as a bitmap. Flipping is not a cheap operation, so use this
     * functionality wisely.
     * @param flipY whether to flip pixels along Y axis
     */
    public static byte[] getFrameBufferPixels(boolean flipY){
        final int w = Core.graphics.getBackBufferWidth();
        final int h = Core.graphics.getBackBufferHeight();
        return getFrameBufferPixels(0, 0, w, h, flipY);
    }

    /**
     * Returns a portion of the default framebuffer contents specified by x, y, width and height, as a byte[] array with a length
     * equal to the specified width * height * 4. The byte[] will always contain RGBA8888 data. If the width and height specified
     * are larger than the framebuffer dimensions, the Texture will be padded accordingly. Pixels that fall outside of the current
     * screen will have RGBA values of 0. Because of differences in screen and image origins the framebuffer contents should be
     * flipped along the Y axis if you intend save them to disk as a bitmap. Flipping is not a cheap operation, so use this
     * functionality wisely.
     * @param flipY whether to flip pixels along Y axis
     */
    public static byte[] getFrameBufferPixels(int x, int y, int w, int h, boolean flipY){
        Core.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        final ByteBuffer pixels = BufferUtils.newByteBuffer(w * h * 4);
        Core.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
        final int numBytes = w * h * 4;
        byte[] lines = new byte[numBytes];
        if(flipY){
            final int numBytesPerLine = w * 4;
            for(int i = 0; i < h; i++){
                pixels.position((h - i - 1) * numBytesPerLine);
                pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
            }
        }else{
            pixels.clear();
            pixels.get(lines);
        }
        return lines;

    }
}
