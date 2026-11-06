package jp.amazonimagestudio;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class App extends JFrame {

    private final JLabel[] thumbnails = new JLabel[6];
    private final BufferedImage[] images = new BufferedImage[6];
    private final BufferedImage[] originalImages = new BufferedImage[6];

    private final ImageCanvas imageCanvas = new ImageCanvas();
    private final JLabel status = new JLabel("画像を開いて編集を開始してください");

    private int selectedIndex = 0;

    // サイズ設定
    private final JTextField widthField =
        new JTextField("2000", 6);

    private final JTextField heightField =
        new JTextField("2000", 6);

    private final JCheckBox keepAspectRatio =
        new JCheckBox("縦横比を維持する", true);

    private boolean updatingSizeFields = false;

    public App() {
        setTitle("Amazon Image Studio");
        setSize(1200, 760);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // 上部ツールバー
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton openButton = new JButton("画像を開く");
        JButton cropButton = new JButton("トリミング");
        JButton undoButton = new JButton("元に戻す");
        JButton saveButton = new JButton("画像を保存");
        JButton removeBackgroundButton =
            new JButton("背景自動削除");

        toolbar.add(openButton);
        toolbar.add(cropButton);
        toolbar.add(undoButton);
        toolbar.add(saveButton);
        toolbar.add(removeBackgroundButton);

        removeBackgroundButton.addActionListener(e -> {

    BufferedImage image = images[selectedIndex];

    if (image == null) {
        JOptionPane.showMessageDialog(
            this,
            "先に画像を開いてください。"
        );
        return;
    }

    int answer = JOptionPane.showConfirmDialog(
        this,
        "選択中の画像の背景を削除します。\n"
        + "処理には数秒かかる場合があります。\n"
        + "続けますか？",
        "背景自動削除",
        JOptionPane.YES_NO_OPTION
    );

    if (answer != JOptionPane.YES_OPTION) {
        return;
    }

    try {
        BufferedImage result =
            BackgroundRemover.removeBackground(image);

        // 選択中の画像を更新
        images[selectedIndex] = result;

        // メイン画面とプレビューを更新
        imageCanvas.setImage(result);
        updateThumbnails();

        status.setText("背景自動削除が完了しました。");

        JOptionPane.showMessageDialog(
            this,
            "背景を削除しました。\n"
            + "PNG形式で保存すると透明部分を保持できます。"
        );

    } catch (Exception ex) {

        JOptionPane.showMessageDialog(
            this,
            "背景削除に失敗しました。\n\n"
            + ex.getClass().getSimpleName()
            + ": " + ex.getMessage(),
            "エラー",
            JOptionPane.ERROR_MESSAGE
        );

        ex.printStackTrace();
    }
});

        // 上部ツールバーとサイズ設定
    JPanel topPanel = new JPanel(new BorderLayout());

    topPanel.add(toolbar, BorderLayout.NORTH);

    JPanel sizePanel = new JPanel(
        new FlowLayout(FlowLayout.LEFT)
    );

    sizePanel.setBorder(
        BorderFactory.createTitledBorder("Amazon向け画像サイズ")
    );

    String[] presets = {
        "自由入力",
        "1000 × 1000 px",
        "1600 × 1600 px",
        "2000 × 2000 px",
        "3000 × 3000 px",
        "1200 × 1600 px",
        "1600 × 1200 px"
    };

    JComboBox<String> presetBox = new JComboBox<>(presets);

    JButton resizeButton = new JButton("サイズ変更");

    // 正方形キャンバスの設定
    String[] canvasSizes = {
        "1000 × 1000 px",
        "1600 × 1600 px",
        "2000 × 2000 px",
        "3000 × 3000 px"
    };

    JComboBox<String> canvasSizeBox =
        new JComboBox<>(canvasSizes);

    canvasSizeBox.setSelectedItem("2000 × 2000 px");

    // 商品画像の大きさ（キャンバスに対する割合）
    JTextField productScaleField =
        new JTextField("90", 3);

    JButton squareCanvasButton =
        new JButton("白背景・正方形にする");

    sizePanel.add(new JLabel("プリセット："));
    sizePanel.add(presetBox);

    sizePanel.add(new JLabel("幅："));
    sizePanel.add(widthField);
    sizePanel.add(new JLabel("px"));

    sizePanel.add(new JLabel("高さ："));
    sizePanel.add(heightField);
    sizePanel.add(new JLabel("px"));

    sizePanel.add(keepAspectRatio);
    sizePanel.add(resizeButton);

    // 正方形キャンバス設定
    sizePanel.add(new JLabel("キャンバス："));
    sizePanel.add(canvasSizeBox);

    sizePanel.add(new JLabel("商品サイズ（%）："));
    sizePanel.add(productScaleField);

    sizePanel.add(squareCanvasButton);

    topPanel.add(sizePanel, BorderLayout.SOUTH);

    add(topPanel, BorderLayout.NORTH);

// プリセット選択時の処理
presetBox.addActionListener(e -> {
    String selected = (String) presetBox.getSelectedItem();

    if (selected == null || selected.equals("自由入力")) {
        return;
    }

    String[] dimensions = selected
        .replace(" px", "")
        .split(" × ");

    widthField.setText(dimensions[0]);
    heightField.setText(dimensions[1]);
});

// サイズ変更ボタン
resizeButton.addActionListener(e -> resizeImage());

// 白背景・正方形キャンバスの作成
squareCanvasButton.addActionListener(e ->
    createSquareCanvas(canvasSizeBox, productScaleField)
);
        // 中央の画像編集エリア
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        centerPanel.add(
            new JLabel("メイン編集画面：画像上をドラッグして範囲を選択"),
            BorderLayout.NORTH
        );

        imageCanvas.setBackground(Color.WHITE);
        imageCanvas.setBorder(
            BorderFactory.createLineBorder(Color.GRAY)
        );

        centerPanel.add(imageCanvas, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 右側の6枚のプレビュー
        JPanel thumbnailPanel = new JPanel(
            new GridLayout(3, 2, 8, 8)
        );

        thumbnailPanel.setPreferredSize(new Dimension(340, 0));
        thumbnailPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        for (int i = 0; i < 6; i++) {
            final int index = i;

            JPanel card = new JPanel(new BorderLayout());

            JLabel title = new JLabel(
                "画像 " + (i + 1),
                SwingConstants.CENTER
            );

            thumbnails[i] = new JLabel(
                "画像なし",
                SwingConstants.CENTER
            );

            thumbnails[i].setOpaque(true);
            thumbnails[i].setBackground(Color.WHITE);

            card.setBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2)
            );

            card.add(title, BorderLayout.NORTH);
            card.add(thumbnails[i], BorderLayout.CENTER);

            card.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectImage(index);
                }
            });

            thumbnailPanel.add(card);
        }

        add(thumbnailPanel, BorderLayout.EAST);

        status.setBorder(new EmptyBorder(8, 10, 8, 10));
        add(status, BorderLayout.SOUTH);

        // 画像を開く
        openButton.addActionListener(e -> openImage());

        // トリミング
        cropButton.addActionListener(e -> cropImage());

        // 元に戻す
        undoButton.addActionListener(e -> undoImage());

        // 保存
        saveButton.addActionListener(e -> saveImage());
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("商品画像を選択");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        try {
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "対応していない画像形式です。",
                    "エラー",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            images[selectedIndex] = image;
            originalImages[selectedIndex] = copyImage(image);

            imageCanvas.setImage(image);
            updateThumbnails();

            status.setText(
                "画像 " + (selectedIndex + 1) + " を読み込みました："
                    + image.getWidth() + " × " + image.getHeight()
            );

        } catch (IOException ex) {
            showError("画像を読み込めませんでした。", ex);
        }
    }

    private void selectImage(int index) {
        selectedIndex = index;
        imageCanvas.setImage(images[index]);
        updateThumbnails();

        status.setText("編集中：画像 " + (index + 1));
    }

    private void cropImage() {
        
        BufferedImage image = images[selectedIndex];

        if (image == null) {
            JOptionPane.showMessageDialog(this, "先に画像を開いてください。");
            return;
        }

        Rectangle crop = imageCanvas.getCropRectangle();

        if (crop == null || crop.width < 2 || crop.height < 2) {
            JOptionPane.showMessageDialog(
                this,
                "画像上でマウスをドラッグして、切り抜く範囲を選択してください。"
            );
            return;
        }

        // 選択範囲は元画像の座標に変換済み
        BufferedImage cropped = image.getSubimage(
            crop.x,
            crop.y,
            crop.width,
            crop.height
        );

        BufferedImage independentImage = copyImage(cropped);

        images[selectedIndex] = independentImage;
        imageCanvas.setImage(independentImage);
        updateThumbnails();

        status.setText(
            "トリミング完了：" + crop.width + " × " + crop.height
        );
    }
    private void resizeImage() {

        BufferedImage image = images[selectedIndex];

        if (image == null) {
            JOptionPane.showMessageDialog(
                this,
                "先に画像を開いてください。"
            );
            return;
        }

        int targetWidth;
        int targetHeight;

        try {
            targetWidth = Integer.parseInt(widthField.getText().trim());
            targetHeight = Integer.parseInt(heightField.getText().trim());

            if (targetWidth <= 0 || targetHeight <= 0
                || targetWidth > 10000 || targetHeight > 10000) {

                throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this,
                "幅と高さには1～10000の整数を入力してください。",
                "入力エラー",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // 縦横比を維持する場合
        if (keepAspectRatio.isSelected()) {

            double scale = Math.min(
                (double) targetWidth / image.getWidth(),
                (double) targetHeight / image.getHeight()
            );

            targetWidth = Math.max(
                1,
                (int) Math.round(image.getWidth() * scale)
            );

            targetHeight = Math.max(
                1,
                (int) Math.round(image.getHeight() * scale)
            );
        }

        // リサイズした画像を作成
        BufferedImage resized = new BufferedImage(
            targetWidth,
            targetHeight,
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2 = resized.createGraphics();

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, targetWidth, targetHeight);

        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        g2.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );

        g2.drawImage(
            image,
            0,
            0,
            targetWidth,
            targetHeight,
            null
        );

        g2.dispose();

        // 編集結果を保存
        images[selectedIndex] = resized;

        imageCanvas.setImage(resized);
        updateThumbnails();

        status.setText(
            "サイズ変更完了："
            + targetWidth + " × " + targetHeight + " px"
        );
    }

    private void createSquareCanvas(
        JComboBox<String> canvasSizeBox,
        JTextField productScaleField) {

    // 選択中の画像を取得
    BufferedImage image = images[selectedIndex];

    if (image == null) {
        JOptionPane.showMessageDialog(
            this,
            "先に画像を開いてください。"
        );
        return;
    }

    // キャンバスサイズを取得
    String selectedSize =
        (String) canvasSizeBox.getSelectedItem();

    if (selectedSize == null) {
        return;
    }

    int canvasSize;

    try {
        canvasSize = Integer.parseInt(
            selectedSize
                .replace(" px", "")
                .split(" × ")[0]
        );
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(
            this,
            "キャンバスサイズを確認してください。"
        );
        return;
    }

    // 商品画像の大きさを取得
    int productPercent;

    try {
        productPercent = Integer.parseInt(
            productScaleField.getText().trim()
        );

        if (productPercent < 10 || productPercent > 100) {
            throw new NumberFormatException();
        }

    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(
            this,
            "商品サイズは10～100の整数で入力してください。",
            "入力エラー",
            JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    // 正方形の白いキャンバスを作成
    BufferedImage canvas = new BufferedImage(
        canvasSize,
        canvasSize,
        BufferedImage.TYPE_INT_RGB
    );

    Graphics2D g2 = canvas.createGraphics();

    // 背景を白色にする
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, canvasSize, canvasSize);

    // 画質を維持するための設定
    g2.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC
    );

    g2.setRenderingHint(
        RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_QUALITY
    );

    g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
    );

    // 商品画像の縦横比を維持してサイズを計算
    double fitScale = Math.min(
        (double) canvasSize / image.getWidth(),
        (double) canvasSize / image.getHeight()
    );

    double productScale = fitScale * productPercent / 100.0;

    int newWidth = Math.max(
        1,
        (int) Math.round(image.getWidth() * productScale)
    );

    int newHeight = Math.max(
        1,
        (int) Math.round(image.getHeight() * productScale)
    );

    // キャンバスの中央に配置
    int x = (canvasSize - newWidth) / 2;
    int y = (canvasSize - newHeight) / 2;

    g2.drawImage(
        image,
        x,
        y,
        newWidth,
        newHeight,
        null
    );

    g2.dispose();

    // 選択中の画像を完成画像に置き換える
    images[selectedIndex] = canvas;

    // メイン画面と6枚のプレビューを更新
    imageCanvas.setImage(canvas);
    updateThumbnails();

    status.setText(
        "正方形キャンバス作成完了："
        + canvasSize + " × " + canvasSize + " px"
    );

    JOptionPane.showMessageDialog(
        this,
        "白背景の正方形画像を作成しました。\n"
        + "サイズ：" + canvasSize + " × " + canvasSize + " px\n"
        + "商品画像：" + newWidth + " × " + newHeight + " px\n"
        + "商品サイズ：" + productPercent + "%"
    );
}

    private void undoImage() {
        if (originalImages[selectedIndex] == null) {
            JOptionPane.showMessageDialog(
                this,
                "元に戻せる画像がありません。"
            );
            return;
        }

        images[selectedIndex] = copyImage(
            originalImages[selectedIndex]
        );

        imageCanvas.setImage(images[selectedIndex]);
        updateThumbnails();

        status.setText("画像 " + (selectedIndex + 1) + " を元に戻しました");
    }

    private void saveImage() {
        BufferedImage image = images[selectedIndex];

        if (image == null) {
            JOptionPane.showMessageDialog(this, "先に画像を開いてください。");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("PNG画像を保存");

        chooser.setSelectedFile(
            new File("amazon-image-" + (selectedIndex + 1) + ".png")
        );

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        // 拡張子がない場合は .png を追加
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }

        try {
            ImageIO.write(image, "png", file);

            status.setText("保存しました：" + file.getName());

        } catch (IOException ex) {
            showError("画像を保存できませんでした。", ex);
        }
    }

    private void updateThumbnails() {
        for (int i = 0; i < 6; i++) {
            if (images[i] == null) {
                thumbnails[i].setIcon(null);
                thumbnails[i].setText("画像なし");
            } else {
                thumbnails[i].setText("");

                thumbnails[i].setIcon(
                    createScaledIcon(images[i], 150, 140)
                );
            }
        }

        imageCanvas.repaint();
    }

    private ImageIcon createScaledIcon(
        BufferedImage image,
        int maxWidth,
        int maxHeight
    ) {
        double scale = Math.min(
            (double) maxWidth / image.getWidth(),
            (double) maxHeight / image.getHeight()
        );

        int width = Math.max(1, (int) (image.getWidth() * scale));
        int height = Math.max(1, (int) (image.getHeight() * scale));

        Image scaled = image.getScaledInstance(
            width,
            height,
            Image.SCALE_SMOOTH
        );

        return new ImageIcon(scaled);
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();

        return copy;
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            message + "\n" + ex.getMessage(),
            "エラー",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private class ImageCanvas extends JPanel {

        private BufferedImage image;

        private Point dragStart;
        private Point dragEnd;

        private Rectangle displayedImageBounds;

        ImageCanvas() {
            setPreferredSize(new Dimension(600, 500));

            MouseAdapter mouseHandler = new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    if (image == null || displayedImageBounds == null) {
                        return;
                    }

                    if (!displayedImageBounds.contains(e.getPoint())) {
                        return;
                    }

                    dragStart = e.getPoint();
                    dragEnd = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        dragEnd = clampPoint(
                            e.getPoint(),
                            displayedImageBounds
                        );

                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragStart != null) {
                        dragEnd = clampPoint(
                            e.getPoint(),
                            displayedImageBounds
                        );

                        repaint();
                    }
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        void setImage(BufferedImage image) {
            this.image = image;
            dragStart = null;
            dragEnd = null;
            repaint();
        }

        Rectangle getCropRectangle() {
            if (image == null
                || displayedImageBounds == null
                || dragStart == null
                || dragEnd == null) {
                return null;
            }

            int x1 = Math.min(dragStart.x, dragEnd.x);
            int y1 = Math.min(dragStart.y, dragEnd.y);
            int x2 = Math.max(dragStart.x, dragEnd.x);
            int y2 = Math.max(dragStart.y, dragEnd.y);

            double scaleX =
                (double) image.getWidth() / displayedImageBounds.width;

            double scaleY =
                (double) image.getHeight() / displayedImageBounds.height;

            int x = (int) Math.floor(
                (x1 - displayedImageBounds.x) * scaleX
            );

            int y = (int) Math.floor(
                (y1 - displayedImageBounds.y) * scaleY
            );

            int right = (int) Math.ceil(
                (x2 - displayedImageBounds.x) * scaleX
            );

            int bottom = (int) Math.ceil(
                (y2 - displayedImageBounds.y) * scaleY
            );

            x = Math.max(0, Math.min(x, image.getWidth() - 1));
            y = Math.max(0, Math.min(y, image.getHeight() - 1));

            right = Math.max(x + 1, Math.min(right, image.getWidth()));
            bottom = Math.max(y + 1, Math.min(bottom, image.getHeight()));

            return new Rectangle(
                x,
                y,
                right - x,
                bottom - y
            );
        }

        private Point clampPoint(Point point, Rectangle bounds) {
            int x = Math.max(
                bounds.x,
                Math.min(point.x, bounds.x + bounds.width)
            );

            int y = Math.max(
                bounds.y,
                Math.min(point.y, bounds.y + bounds.height)
            );

            return new Point(x, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (image == null) {
                g.setColor(Color.GRAY);
                g.drawString(
                    "画像を開いてください",
                    getWidth() / 2 - 60,
                    getHeight() / 2
                );
                displayedImageBounds = null;
                return;
            }

            int availableWidth = Math.max(1, getWidth() - 20);
            int availableHeight = Math.max(1, getHeight() - 20);

            double scale = Math.min(
                (double) availableWidth / image.getWidth(),
                (double) availableHeight / image.getHeight()
            );

            int width = Math.max(1, (int) (image.getWidth() * scale));
            int height = Math.max(1, (int) (image.getHeight() * scale));

            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;

            displayedImageBounds = new Rectangle(x, y, width, height);

            g.drawImage(image, x, y, width, height, null);

            if (dragStart != null && dragEnd != null) {
                int sx = Math.min(dragStart.x, dragEnd.x);
                int sy = Math.min(dragStart.y, dragEnd.y);
                int sw = Math.abs(dragEnd.x - dragStart.x);
                int sh = Math.abs(dragEnd.y - dragStart.y);

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(sx, sy, sw, sh);

                g2.dispose();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            App app = new App();
            app.setVisible(true);
        });
    }
}