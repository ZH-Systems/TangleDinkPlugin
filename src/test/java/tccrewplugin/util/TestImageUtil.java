package tccrewplugin.util;

import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class TestImageUtil {

    public Image getExample() {
        Random rand = ThreadLocalRandom.current();
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()).getRGB());
        return image;
    }
}

