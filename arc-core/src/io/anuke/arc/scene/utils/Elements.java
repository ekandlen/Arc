package io.anuke.arc.scene.utils;

import io.anuke.arc.func.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.ImageButton.*;
import io.anuke.arc.scene.ui.TextButton.*;

public class Elements{

    public static CheckBox newCheck(String text, Boolc listener){
        CheckBox button = new CheckBox(text);
        if(listener != null)
            button.changed(() -> listener.get(button.isChecked()));
        return button;
    }

    public static TextButton newButton(String text, Runnable listener){
        TextButton button = new TextButton(text);
        if(listener != null)
            button.changed(listener);

        return button;
    }

    public static TextButton newButton(String text, TextButtonStyle style, Runnable listener){
        TextButton button = new TextButton(text, style);
        if(listener != null)
            button.changed(listener);

        return button;
    }

    public static ImageButton newImageButton(Drawable icon, Runnable listener){
        ImageButton button = new ImageButton(icon);
        if(listener != null)
            button.changed(listener);
        return button;
    }

    public static ImageButton newImageButton(Drawable icon, float size, Runnable listener){
        ImageButton button = new ImageButton(icon);
        button.resizeImage(size);
        if(listener != null)
            button.changed(listener);
        return button;
    }

    public static ImageButton newImageButton(ImageButtonStyle style, Drawable icon, float size, Runnable listener){
        ImageButton button = new ImageButton(icon, style);
        button.resizeImage(size);
        if(listener != null)
            button.changed(listener);
        return button;
    }

    public static ImageButton newImageButton(Drawable icon, float size, Color color, Runnable listener){
        ImageButton button = new ImageButton(icon);
        button.resizeImage(size);
        button.getImage().setColor(color);
        if(listener != null)
            button.changed(listener);
        return button;
    }

    public static TextField newField(String text, Cons<String> listener){
        TextField field = new TextField(text);
        if(listener != null){
            field.changed(() -> {
                if(field.isValid()){
                    listener.get(field.getText());
                }
            });
        }

        return field;
    }
}
