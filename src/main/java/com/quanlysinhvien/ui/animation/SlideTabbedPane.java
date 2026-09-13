package com.quanlysinhvien.ui.animation;

import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Custom JTabbedPane supporting smooth horizontal slide and fade animation (200ms)
 * between tab switches, with full HiDPI display scaling support, clean lifecycle hooks,
 * and 100% preservation of JTabbedPane contracts.
 */
public class SlideTabbedPane extends JTabbedPane {
    private boolean animationEnabled = true;
    private boolean isAnimating = false;
    private float animProgress = 0.0f;
    private int slideDirection = 1; // 1: left to right, -1: right to left
    private Rectangle contentBounds;
    private BufferedImage oldImage;
    private BufferedImage newImage;
    private Timer animTimer;

    public SlideTabbedPane() {
        super();
        initStyle();
    }

    public SlideTabbedPane(int tabPlacement) {
        super(tabPlacement);
        initStyle();
    }

    public SlideTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        super(tabPlacement, tabLayoutPolicy);
        initStyle();
    }

    private void initStyle() {
        setFont(UITheme.FONT_BOLD);
        setBackground(UITheme.CANVAS_BG);
        setForeground(UITheme.TEXT_PRIMARY);
        setDoubleBuffered(true);
    }

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    @Override
    public void setSelectedIndex(int targetIndex) {
        int oldIndex = getSelectedIndex();
        if (targetIndex == oldIndex || targetIndex < 0 || targetIndex >= getTabCount()) {
            super.setSelectedIndex(targetIndex);
            return;
        }

        if (!animationEnabled || !isShowing() || getWidth() <= 0 || getHeight() <= 0) {
            super.setSelectedIndex(targetIndex);
            return;
        }

        Component oldComp = (oldIndex >= 0 && oldIndex < getTabCount()) ? getComponentAt(oldIndex) : null;
        Component newComp = getComponentAt(targetIndex);

        if (oldComp == null || newComp == null || oldComp.getWidth() <= 0 || oldComp.getHeight() <= 0) {
            super.setSelectedIndex(targetIndex);
            return;
        }

        stopAnimation();

        Rectangle bounds = oldComp.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) {
            super.setSelectedIndex(targetIndex);
            return;
        }

        try {
            // Retrieve display scaling factor to ensure crisp vector-level HiDPI rendering
            GraphicsConfiguration gc = getGraphicsConfiguration();
            AffineTransform at = (gc != null) ? gc.getDefaultTransform() : null;
            double scaleX = (at != null && at.getScaleX() > 0) ? at.getScaleX() : 1.0;
            double scaleY = (at != null && at.getScaleY() > 0) ? at.getScaleY() : 1.0;

            int imgW = Math.max(1, (int) Math.round(bounds.width * scaleX));
            int imgH = Math.max(1, (int) Math.round(bounds.height * scaleY));

            Color bg = getBackground() != null ? getBackground() : UITheme.CANVAS_BG;

            // Snapshot old component while it is still selected and visible
            oldImage = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gOld = oldImage.createGraphics();
            if (scaleX != 1.0 || scaleY != 1.0) {
                gOld.scale(scaleX, scaleY);
            }
            gOld.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gOld.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gOld.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gOld.setColor(bg);
            gOld.fillRect(0, 0, bounds.width, bounds.height);
            oldComp.paint(gOld);
            gOld.dispose();

            slideDirection = (targetIndex > oldIndex) ? 1 : -1;

            // Switch selected tab in JTabbedPane first, making newComp visible and laid out
            super.setSelectedIndex(targetIndex);

            // Ensure newComp is laid out to the content bounds
            if (newComp.getWidth() <= 0 || newComp.getHeight() <= 0) {
                newComp.setBounds(bounds);
                newComp.validate();
            }

            Rectangle newBounds = newComp.getBounds();
            contentBounds = (newBounds != null && newBounds.width > 0 && newBounds.height > 0)
                    ? new Rectangle(newBounds)
                    : new Rectangle(bounds);

            // Snapshot new component now that it is visible and positioned
            newImage = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gNew = newImage.createGraphics();
            if (scaleX != 1.0 || scaleY != 1.0) {
                gNew.scale(scaleX, scaleY);
            }
            gNew.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gNew.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gNew.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gNew.setColor(bg);
            gNew.fillRect(0, 0, bounds.width, bounds.height);
            newComp.paint(gNew);
            gNew.dispose();

            startAnimation();
        } catch (Throwable t) {
            stopAnimation();
            super.setSelectedIndex(targetIndex);
        }
    }

    private synchronized void startAnimation() {
        isAnimating = true;
        animProgress = 0.0f;
        final float step = 16.0f / 200.0f; // 200ms duration at ~60 FPS

        animTimer = new Timer(16, e -> {
            if (!isShowing()) {
                stopAnimation();
                return;
            }
            animProgress += step;
            if (animProgress >= 1.0f) {
                animProgress = 1.0f;
                stopAnimation();
                return;
            }
            if (contentBounds != null) {
                repaint(contentBounds.x, contentBounds.y, contentBounds.width, contentBounds.height);
            } else {
                repaint();
            }
        });
        animTimer.start();
    }

    private synchronized void stopAnimation() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
            animTimer = null;
        }
        isAnimating = false;
        animProgress = 0.0f;
        if (oldImage != null) {
            oldImage.flush();
            oldImage = null;
        }
        if (newImage != null) {
            newImage.flush();
            newImage = null;
        }
        if (contentBounds != null) {
            repaint(contentBounds.x, contentBounds.y, contentBounds.width, contentBounds.height);
            contentBounds = null;
        } else {
            repaint();
        }
    }

    @Override
    public void removeNotify() {
        stopAnimation();
        super.removeNotify();
    }

    @Override
    public void removeTabAt(int index) {
        stopAnimation();
        super.removeTabAt(index);
    }

    @Override
    public void removeAll() {
        stopAnimation();
        super.removeAll();
    }

    @Override
    public void paintChildren(Graphics g) {
        super.paintChildren(g);

        BufferedImage img1 = oldImage;
        BufferedImage img2 = newImage;
        Rectangle bounds = contentBounds;

        if (isAnimating && img1 != null && img2 != null && bounds != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.clipRect(bounds.x, bounds.y, bounds.width, bounds.height);

            float ease = Easing.easeOutCubic(animProgress);
            int w = bounds.width;
            int oldX = (int) (-slideDirection * w * ease);
            int newX = (int) (slideDirection * w * (1.0f - ease));

            // Fill canvas background to avoid smearing
            Color bg = getBackground() != null ? getBackground() : UITheme.CANVAS_BG;
            g2.setColor(bg);
            g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

            // Draw old component sliding out (HiDPI scaling mapped to content bounds)
            float oldAlpha = Math.max(0.0f, Math.min(1.0f, 1.0f - ease));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, oldAlpha));
            g2.drawImage(img1, bounds.x + oldX, bounds.y, bounds.width, bounds.height, null);

            // Draw new component sliding in (HiDPI scaling mapped to content bounds)
            float newAlpha = Math.max(0.0f, Math.min(1.0f, ease));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, newAlpha));
            g2.drawImage(img2, bounds.x + newX, bounds.y, bounds.width, bounds.height, null);

            g2.dispose();
        }
    }
}
