package jp.amazonimagestudio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import nu.pattern.OpenCV;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class BackgroundRemover {

    public static BufferedImage removeBackground(
            BufferedImage original) throws Exception {

        if (original == null) {
            throw new IllegalArgumentException(
                "画像が読み込まれていません。"
            );
        }

        // OpenCVのネイティブライブラリを読み込む
        OpenCV.loadLocally();

        int width = original.getWidth();
        int height = original.getHeight();

        if (width < 10 || height < 10) {
            throw new IllegalArgumentException(
                "画像が小さすぎます。"
            );
        }

        // BufferedImageをPNGバイト列に変換
        ByteArrayOutputStream output =
            new ByteArrayOutputStream();

        ImageIO.write(original, "png", output);

        MatOfByte inputBytes =
            new MatOfByte(output.toByteArray());

        // OpenCVで画像を読み込む
        Mat source = Imgcodecs.imdecode(
            inputBytes,
            Imgcodecs.IMREAD_COLOR
        );

        inputBytes.release();

        if (source.empty()) {
            source.release();

            throw new IllegalArgumentException(
                "画像をOpenCVで読み込めませんでした。"
            );
        }

        Mat mask = new Mat();
        Mat backgroundModel = new Mat();
        Mat foregroundModel = new Mat();

        try {
            // 画像の端を背景とみなすための矩形を作る
            int marginX = Math.max(1, width / 10);
            int marginY = Math.max(1, height / 10);

            Rect rectangle = new Rect(
                marginX,
                marginY,
                width - marginX * 2,
                height - marginY * 2
            );

            // GrabCutで前景と背景を推定する
            Imgproc.grabCut(
                source,
                mask,
                rectangle,
                backgroundModel,
                foregroundModel,
                5,
                Imgproc.GC_INIT_WITH_RECT
            );

            // 透明部分を持つ画像を作成
            BufferedImage result = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB
            );

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    double[] maskValue = mask.get(y, x);

                    int label = (int) maskValue[0];

                    // 前景と推定された部分を残す
                    boolean foreground =
                        label == Imgproc.GC_FGD
                        || label == Imgproc.GC_PR_FGD;

                    int pixel = original.getRGB(x, y);

                    int alpha = foreground ? 255 : 0;

                    // 背景は透明、前景は不透明にする
                    int argb =
                        (pixel & 0x00FFFFFF)
                        | (alpha << 24);

                    result.setRGB(x, y, argb);
                }
            }

            return result;

        } finally {
            source.release();
            mask.release();
            backgroundModel.release();
            foregroundModel.release();
        }
    }
}