package com.icthh.xm.ms.entity.util;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.mortennobel.imagescaling.AdvancedResizeOp.UnsharpenMask;
import com.mortennobel.imagescaling.ResampleOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.imageio.ImageIO;

public class ImageResizeUtil {

    private ImageResizeUtil() {

    }

    public static InputStream resize(InputStream is, int newSize) throws IOException {
        BufferedImage sourceImage = ImageIO.read(is);

        if (sourceImage == null) {
            throw new BusinessException("Source image is null");
        }

        Integer newHeight;
        Integer newWidth;

        if (sourceImage.getHeight() > sourceImage.getWidth()) {
            BigDecimal ration = new BigDecimal(newSize)
                .divide(new BigDecimal(sourceImage.getHeight()), 2, RoundingMode.HALF_UP);
            newHeight = newSize;
            newWidth = (int) Math.round(sourceImage.getWidth() * ration.doubleValue());
        } else if (sourceImage.getHeight() < sourceImage.getWidth()) {
            BigDecimal ration = new BigDecimal(newSize)
                .divide(new BigDecimal(sourceImage.getWidth()), 2, RoundingMode.HALF_UP);
            newWidth = newSize;
            newHeight = (int) Math.round(sourceImage.getHeight() * ration.doubleValue());
        } else {
            newWidth = newSize;
            newHeight = newSize;
        }

        if (newWidth < 3) {
            newWidth = 3;
        }
        if (newHeight < 3) {
            newHeight = 3;
        }

        BufferedImage scaledImage = scale(sourceImage, newHeight, newWidth);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(scaledImage, "png", baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    private static BufferedImage scale(BufferedImage sourceImage, Integer newHeight, Integer newWidth) {
        ResampleOp resizeOp = new ResampleOp(newWidth, newHeight);
        resizeOp.setUnsharpenMask(UnsharpenMask.Normal);

        return resizeOp.filter(sourceImage, null);
    }
}
