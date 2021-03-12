package resourceUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SuperFastTextureUtil {

    public static void main(String[] args) throws IOException {
        File f = new File("src/main/resources/misc/alienFontProcessed.png");
        BufferedImage img = ImageIO.read(f);
        List<BufferedImage> chars = splitImageAndRemoveEmpty(img, 7, 7, 16);
        BufferedImage reasambled = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = reasambled.createGraphics();
        System.out.println(chars.size());
        int size = 512 / 7;
        int index = 0;
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                if (index < chars.size())
                    g.drawImage(chars.get(index), x * size, y * size, size, size, null);
                index++;
            }
        }
        g.dispose();
        File f2 = new File("src/main/resources/misc/alienFontShrinked.png");
        ImageIO.write(reasambled, "PNG", f2);
        int ind = 0;
        for (BufferedImage i : chars) {
            File f3 = new File("src/main/resources/misc/alienFontchar" + ind + ".png");
            ImageIO.write(i, "PNG", f3);
            ind++;
        }
    }

    private static List<BufferedImage> splitImageAndRemoveEmpty(BufferedImage img, float xPart, float yParts, int remove) {
        List<BufferedImage> rtList = new ArrayList<>();
        float subWidth = img.getWidth() / xPart;
        float subHeight = img.getHeight() / yParts;
        for (int y = 0; y < yParts; y++) {
            for (int x = 0; x < xPart; x++) {
                int px = (int) (x * subWidth) + remove;
                int py = (int) (y * subHeight) + remove;
                int w = (int) subWidth - 2 * remove;
                int h = (int) subHeight - 2 * remove;
                BufferedImage subImage = img.getSubimage(px, py, w, h);
                if (containsImage(subImage)) {
                    rtList.add(subImage);
                }
            }
        }
        return rtList;
    }

    private static boolean containsImage(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Color pixelColor = new Color(img.getRGB(x, y), true);
                if (pixelColor.getAlpha() != 0) {
                    return true;
                }
            }
        }
        return false;
    }

}
