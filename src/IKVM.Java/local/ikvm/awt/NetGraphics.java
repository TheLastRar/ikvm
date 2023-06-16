/*
  Copyright (C) 2002, 2004, 2005, 2006, 2007 Jeroen Frijters
  Copyright (C) 2006 Active Endpoints, Inc.
  Copyright (C) 2006 - 2014 Volker Berlin (i-net software)
  Copyright (C) 2011 Karsten Heinrich (i-net software)

  This software is provided 'as-is', without any express or implied
  warranty.  In no event will the authors be held liable for any damages
  arising from the use of this software.

  Permission is granted to anyone to use this software for any purpose,
  including commercial applications, and to alter it and redistribute it
  freely, subject to the following restrictions:

  1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software
     in a product, an acknowledgment in the product documentation would be
     appreciated but is not required.
  2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.
  3. This notice may not be removed or altered from any source distribution.

  Jeroen Frijters
  jeroen@frijters.net 

*/
package ikvm.awt;

import cli.System.Drawing.Bitmap;
import cli.System.Drawing.Color;
import cli.System.Drawing.Font;
import cli.System.Drawing.FontFamily;
import cli.System.Drawing.FontStyle;
import cli.System.Drawing.Graphics;
import cli.System.Drawing.GraphicsUnit;
import cli.System.Drawing.Brush;
import cli.System.Drawing.Image;
import cli.System.Drawing.Pen;
import cli.System.Drawing.Point;
import cli.System.Drawing.PointF;
import cli.System.Drawing.SizeF;
import cli.System.Drawing.SolidBrush;
import cli.System.Drawing.StringFormat;
import cli.System.Drawing.StringFormatFlags;
import cli.System.Drawing.StringTrimming;
import cli.System.Drawing.TextureBrush;
import cli.System.Drawing.Rectangle;
import cli.System.Drawing.RectangleF;
import cli.System.Drawing.Region;
import cli.System.Drawing.Drawing2D.ColorBlend;
import cli.System.Drawing.Drawing2D.CompositingMode;
import cli.System.Drawing.Drawing2D.DashCap;
import cli.System.Drawing.Drawing2D.DashStyle;
import cli.System.Drawing.Drawing2D.GraphicsPath;
import cli.System.Drawing.Drawing2D.InterpolationMode;
import cli.System.Drawing.Drawing2D.LinearGradientBrush;
import cli.System.Drawing.Drawing2D.Matrix;
import cli.System.Drawing.Drawing2D.PathGradientBrush;
import cli.System.Drawing.Drawing2D.PixelOffsetMode;
import cli.System.Drawing.Drawing2D.SmoothingMode;
import cli.System.Drawing.Drawing2D.WrapMode;
import cli.System.Drawing.Text.TextRenderingHint;

import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

abstract class NetGraphics extends java.awt.Graphics2D {
    private Graphics graphics;
    private java.awt.Color javaColor;
    private java.awt.Paint javaPaint;
    Color color;
    private Color bgcolor;
    private java.awt.Font font;
    private java.awt.Stroke stroke;
    private static java.awt.BasicStroke defaultStroke = new java.awt.BasicStroke();
    private Font netfont;
    private int baseline;
    Brush brush;
    Pen pen;
    private CompositeHelper composite;
    private java.awt.Composite javaComposite = java.awt.AlphaComposite.SrcOver;
    private Object textAntialiasHint;
    private Object fractionalHint = java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;

    private static Map<String, Integer> baselines = new HashMap<String, Integer>();

    static final StringFormat FORMAT = new StringFormat(StringFormat.get_GenericTypographic());

    static {
        FORMAT.set_FormatFlags(StringFormatFlags.wrap(
                StringFormatFlags.MeasureTrailingSpaces | StringFormatFlags.NoWrap | StringFormatFlags.FitBlackBox));
        FORMAT.set_Trimming(StringTrimming.wrap(StringTrimming.None));
    }

    protected NetGraphics(Graphics g, Object destination, java.awt.Font font, Color fgcolor, Color bgcolor) // : base(
                                                                                                            // new
                                                                                                            // sun.java2d.SurfaceData(destination)
                                                                                                            // )
    {
        if (font == null) {
            font = new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12);
        }
        this.font = font;
        netfont = font.getNetFont();
        this.color = fgcolor;
        this.bgcolor = bgcolor;
        composite = CompositeHelper.Create(javaComposite, g);
        init(g);
    }

    /// <summary>
    /// The current C# Graphics
    /// </summary>
    Graphics getG() {
        return graphics;
    }

    void setG(Graphics value) {
        graphics = value;
    }

    protected void init(Graphics graphics) {
        NetGraphicsState state = new NetGraphicsState();
        state.saveGraphics(this);
        setG(graphics);
        state.restoreGraphics(this);
    }

    /// <summary>
    /// Get the size of the graphics. This is used as a hind for some hacks.
    /// </summary>
    /// <returns></returns>
    protected SizeF GetSize() {
        return getG().get_ClipBounds().get_Size();
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        boolean newBrush = bgcolor != Color.Empty;
        Brush br = newBrush ? new SolidBrush(bgcolor) : brush;
        try {
            Graphics g = getG();
            CompositingMode tempMode = g.get_CompositingMode();
            g.set_CompositingMode(CompositingMode.wrap(CompositingMode.SourceCopy));
            g.FillRectangle(br, x, y, width, height);
            g.set_CompositingMode(tempMode);
        } finally {
            if (newBrush) {
                br.Dispose();
            }
        }
    }

    @Override
    public void clipRect(int x, int y, int w, int h) {
        getG().IntersectClip(new Rectangle(x, y, w, h));
    }

    @Override
    public void clip(java.awt.Shape shape) {
        if (shape == null) {
            // note that ComponentGraphics overrides clip() to throw a NullPointerException
            // when shape is null
            getG().ResetClip();
        } else {
            getG().IntersectClip(new Region(J2C.ConvertShape(shape)));
        }
    }

    @Override
    public void dispose() {
        if (pen != null)
            pen.Dispose();
        if (brush != null)
            brush.Dispose();
        graphics.Dispose(); // for dispose we does not need to synchronize the buffer of a bitmap
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        getG().DrawArc(pen, x, y, width, height, 360 - startAngle - arcAngle, arcAngle);
    }

    @Override
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) data[offset + i];
        }
        drawChars(chars, 0, length, x, y);
    }

    @Override
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        drawString(new String(data, offset, length), x, y);
    }

    @Override
    public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            java.awt.Color color, java.awt.image.ImageObserver observer) {
        Image image = J2C.ConvertImage(img);
        if (image == null) {
            return false;
        }
        Rectangle destRect = new Rectangle(dx1, dy1, dx2 - dx1, dy2 - dy1);
        Graphics g = getG();
        Brush brush = new SolidBrush(composite.GetColor(color));
        try {
            g.FillRectangle(brush, destRect);
        } finally {
            brush.Dispose();
        }
        synchronized (image) {
            g.DrawImage(image, destRect, sx1, sy1, sx2 - sx1, sy2 - sy1, GraphicsUnit.wrap(GraphicsUnit.Pixel),
                    composite.GetImageAttributes());
        }
        return true;
    }

    @Override
    public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            java.awt.image.ImageObserver observer) {
        Image image = J2C.ConvertImage(img);
        if (image == null) {
            return false;
        }
        Rectangle destRect = new Rectangle(dx1, dy1, dx2 - dx1, dy2 - dy1);
        synchronized (image) {
            getG().DrawImage(image, destRect, sx1, sy1, sx2 - sx1, sy2 - sy1, GraphicsUnit.wrap(GraphicsUnit.Pixel),
                    composite.GetImageAttributes());
        }
        return true;
    }

    @Override
    public boolean drawImage(java.awt.Image img, int x, int y, int width, int height, java.awt.Color bgcolor,
            java.awt.image.ImageObserver observer) {
        Image image = J2C.ConvertImage(img);
        if (image == null) {
            return false;
        }
        Brush brush = new SolidBrush(composite.GetColor(bgcolor));
        try {
            getG().FillRectangle(brush, x, y, width, height);
        } finally {
            brush.Dispose();
        }
        synchronized (image) {
            getG().DrawImage(image, new Rectangle(x, y, width, height), 0, 0, image.get_Width(), image.get_Height(),
                    GraphicsUnit.wrap(GraphicsUnit.Pixel), composite.GetImageAttributes());
        }
        return true;
    }

    @Override
    public boolean drawImage(java.awt.Image img, int x, int y, java.awt.Color bgcolor,
            java.awt.image.ImageObserver observer) {
        if (img == null) {
            return false;
        }
        return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), bgcolor, observer);
    }

    @Override
    public boolean drawImage(java.awt.Image img, int x, int y, int width, int height,
            java.awt.image.ImageObserver observer) {
        Image image = J2C.ConvertImage(img);
        if (image == null) {
            return false;
        }
        synchronized (image) {
            getG().DrawImage(image, new Rectangle(x, y, width, height), 0, 0, image.get_Width(), image.get_Height(),
                    GraphicsUnit.wrap(GraphicsUnit.Pixel), composite.GetImageAttributes());
        }
        return true;
    }

    @Override
    public boolean drawImage(java.awt.Image img, int x, int y, java.awt.image.ImageObserver observer) {
        if (img == null) {
            return false;
        }
        return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), observer);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        // HACK DrawLine doesn't appear to draw the last pixel, so for single pixel
        // lines, we have
        // a freaky workaround
        if (x1 == x2 && y1 == y2) {
            getG().DrawLine(pen, x1, y1, x1 + 0.01f, y2 + 0.01f);
        } else {
            getG().DrawLine(pen, x1, y1, x2, y2);
        }
    }

    @Override
    public void drawOval(int x, int y, int w, int h) {
        getG().DrawEllipse(pen, x, y, w, h);
    }

    @Override
    public void drawPolygon(java.awt.Polygon polygon) {
        drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    @Override
    public void drawPolygon(int[] aX, int[] aY, int aLength) {
        Point[] points = new Point[aLength];
        for (int i = 0; i < aLength; i++) {
            points[i].set_X(aX[i]);
            points[i].set_Y(aY[i]);
        }
        getG().DrawPolygon(pen, points);
    }

    /// <summary>
    /// Draw a sequence of connected lines
    /// </summary>
    /// <param name="aX">Array of x coordinates</param>
    /// <param name="aY">Array of y coordinates</param>
    /// <param name="aLength">Length of coordinate arrays</param>
    @Override
    public void drawPolyline(int[] aX, int[] aY, int aLength) {
        for (int i = 0; i < aLength - 1; i++) {
            Point point1 = new Point(aX[i], aY[i]);
            Point point2 = new Point(aX[i + 1], aY[i + 1]);
            getG().DrawLine(pen, point1, point2);
        }
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        getG().DrawRectangle(pen, x, y, width, height);
    }

    /// <summary>
    /// Apparently there is no rounded rec function in .Net. Draw the
    /// rounded rectangle by using lines and arcs.
    /// </summary>
    @Override
    public void drawRoundRect(int x, int y, int w, int h, int arcWidth, int arcHeight) {
        GraphicsPath gp = J2C.ConvertRoundRect(x, y, w, h, arcWidth, arcHeight);
        try {
            getG().DrawPath(pen, gp);
        } finally {
            gp.Dispose();
        }
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        java.awt.Paint p = getPaint();
        java.awt.Color c = getColor();
        java.awt.Color brighter = c.brighter();
        java.awt.Color darker = c.darker();

        if (!raised) {
            setColor(darker);
        } else if (p != c) {
            setColor(c);
        }
        fillRect(x + 1, y + 1, width - 2, height - 2);
        setColor(raised ? brighter : darker);
        fillRect(x, y, 1, height);
        fillRect(x + 1, y, width - 2, 1);
        setColor(raised ? darker : brighter);
        fillRect(x + 1, y + height - 1, width - 1, 1);
        fillRect(x + width - 1, y, 1, height - 1);
        setPaint(p);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        getG().FillPie(brush, x, y, width, height, 360 - startAngle - arcAngle, arcAngle);
    }

    @Override
    public void fillOval(int x, int y, int w, int h) {
        getG().FillEllipse(brush, x, y, w, h);
    }

    @Override
    public void fillPolygon(java.awt.Polygon polygon) {
        fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    @Override
    public void fillPolygon(int[] aX, int[] aY, int aLength) {
        Point[] points = new Point[aLength];
        for (int i = 0; i < aLength; i++) {
            points[i].set_X(aX[i]);
            points[i].set_Y(aY[i]);
        }
        getG().FillPolygon(brush, points);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        getG().FillRectangle(brush, x, y, width, height);
    }

    @Override
    public void fillRoundRect(int x, int y, int w, int h, int arcWidth, int arcHeight) {
        GraphicsPath gp = J2C.ConvertRoundRect(x, y, w, h, arcWidth, arcHeight);
        getG().FillPath(brush, gp);
        gp.Dispose();
    }

    @Override
    public java.awt.Shape getClip() {
        return getClipBounds();
    }

    @Override
    public java.awt.Rectangle getClipBounds(java.awt.Rectangle r) {
        Graphics g = getG();
        Region clip = g.get_Clip(); // Was using?
        {
            if (!clip.IsInfinite(g)) {
                RectangleF rec = clip.GetBounds(g);
                r.x = (int) rec.get_X();
                r.y = (int) rec.get_Y();
                r.width = (int) rec.get_Width();
                r.height = (int) rec.get_Height();
            }
            return r;
        }
    }

    @Override
    public java.awt.Rectangle getClipBounds() {
        Graphics g = getG();
        Region clip = g.get_Clip(); // Was using?
        {
            if (clip.IsInfinite(g)) {
                return null;
            }
            RectangleF rec = clip.GetBounds(g);
            return C2J.ConvertRectangle(rec);
        }
    }

    @Deprecated
    @Override
    public java.awt.Rectangle getClipRect() {
        return getClipBounds();
    }

    @Override
    public java.awt.Color getColor() {
        if (javaColor == null) {
            javaColor = composite.GetColor(color);
        }
        return javaColor;
    }

    @Override
    public java.awt.Font getFont() {
        return font;
    }

    @Override
    public java.awt.FontMetrics getFontMetrics(java.awt.Font f) {
        return sun.font.FontDesignMetrics.getMetrics(f);
    }

    @Override
    public java.awt.FontMetrics getFontMetrics() {
        return sun.font.FontDesignMetrics.getMetrics(font);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        getG().set_Clip(new Region(new Rectangle(x, y, width, height)));
    }

    @Override
    public void setClip(java.awt.Shape shape) {
        Graphics g = getG();
        if (shape == null) {
            Region clip = g.get_Clip();
            clip.MakeInfinite();
            g.set_Clip(clip);
        } else {
            g.set_Clip(new Region(J2C.ConvertShape(shape)));
        }
    }

    @Override
    public void setColor(java.awt.Color color) {
        if (color == null || color == this.javaPaint) {
            // Does not change the color, if it is null like in SunGraphics2D
            return;
        }
        this.javaPaint = this.javaColor = color;
        this.color = composite.GetColor(color);
        if (brush instanceof SolidBrush) {
            ((SolidBrush) brush).set_Color(this.color);
        } else {
            brush.Dispose();
            brush = new SolidBrush(this.color);
        }
        pen.set_Color(this.color);
        pen.set_Brush(brush);
    }

    @Override
    public void setFont(java.awt.Font f) {
        if (f != null && f != font) {
            netfont = f.getNetFont();
            font = f;
            baseline = getBaseline(netfont, getG().get_TextRenderingHint());
        }
    }

    @Override
    public void setPaintMode() {
        throw new NoSuchMethodError();
    }

    @Override
    public void setXORMode(java.awt.Color param) {
        if (param == null) {
            throw new java.lang.IllegalArgumentException("null XORColor");
        }
        throw new NoSuchMethodError();
    }

    @Override
    public void translate(int x, int y) {
        Graphics g = getG();
        Matrix transform = g.get_Transform();
        transform.Translate(x, y);
        g.set_Transform(transform);
    }

    @Override
    public void draw(java.awt.Shape shape) {
        GraphicsPath gp = J2C.ConvertShape(shape);
        try {
            getG().DrawPath(pen, gp);
        } finally {
            gp.Dispose();
        }
    }

    @Override
    public boolean drawImage(java.awt.Image img, java.awt.geom.AffineTransform xform, ImageObserver observer) {
        if (img == null) {
            return true;
        }

        if (xform == null || xform.isIdentity()) {
            return drawImage(img, 0, 0, null, observer);
        }

        NetGraphics clone = (NetGraphics) create();
        clone.transform(xform);
        boolean rendered = clone.drawImage(img, 0, 0, null, observer);
        clone.dispose();
        return rendered;
    }

    @Override
    public void drawImage(java.awt.image.BufferedImage image, BufferedImageOp op, int x, int y) {
        if (op == null) {
            drawImage(image, x, y, null);
        } else {
            if (!(op instanceof AffineTransformOp)) {
                drawImage(op.filter(image, null), x, y, null);
            } else {
                try {
                    throw new UnsupportedOperationException();
                } catch (UnsupportedOperationException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }

    @Override
    public void drawRenderedImage(java.awt.image.RenderedImage img, java.awt.geom.AffineTransform xform) {
        if (img == null) {
            return;
        }

        // BufferedImage case: use a simple drawImage call
        if (img instanceof BufferedImage) {
            BufferedImage bufImg = (BufferedImage) img;
            drawImage(bufImg, xform, null);
            return;
        }
        throw new UnsupportedOperationException(
                "drawRenderedImage not implemented for images which are not BufferedImages.");
    }

    @Override
    public void drawRenderableImage(java.awt.image.renderable.RenderableImage image,
            java.awt.geom.AffineTransform xform) {
        throw new NoSuchMethodError();
    }

    @Override
    public void drawString(String str, int x, int y) {
        drawString(str, (float) x, (float) y);
    }

    @Override
    public void drawString(String text, float x, float y) {
        if (text.length() == 0) {
            return;
        }
        Graphics g = getG();
        CompositingMode origCM = g.get_CompositingMode();
        try {
            if (origCM != CompositingMode.wrap(CompositingMode.SourceOver)) {
                // Java has a different behaviar for AlphaComposite and Text Antialiasing
                g.set_CompositingMode(CompositingMode.wrap(CompositingMode.SourceOver));
            }

            boolean fractional = isFractionalMetrics();
            if (fractional || !sun.font.StandardGlyphVector.isSimpleString(font, text)) {
                g.DrawString(text, netfont, brush, x, y - baseline, FORMAT);
            } else {
                // fixed metric for simple text, we position every character to simulate the
                // Java behaviour
                java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, isAntiAlias(),
                        fractional);
                sun.font.FontDesignMetrics metrics = sun.font.FontDesignMetrics.getMetrics(font, frc);
                y -= baseline;
                for (int i = 0; i < text.length(); i++) {
                    g.DrawString(text.substring(i, 1), netfont, brush, x, y, FORMAT);
                    x += metrics.charWidth(text.charAt(i));
                }
            }
        } finally {
            if (origCM.Value != CompositingMode.SourceOver) {
                g.set_CompositingMode(origCM);
            }
        }
    }

    @Override
    public void drawString(java.text.AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float) x, (float) y);
    }

    @Override
    public void drawString(java.text.AttributedCharacterIterator iterator, float x, float y) {
        if (iterator == null) {
            throw new java.lang.NullPointerException("AttributedCharacterIterator is null");
        }
        if (iterator.getBeginIndex() == iterator.getEndIndex()) {
            return; /* nothing to draw */
        }
        java.awt.font.TextLayout tl = new java.awt.font.TextLayout(iterator, getFontRenderContext());
        tl.draw(this, x, y);
    }

    @Override
    public void fill(java.awt.Shape shape) {
        getG().FillPath(brush, J2C.ConvertShape(shape));
    }

    @Override
    public boolean hit(java.awt.Rectangle rect, java.awt.Shape s, boolean onStroke) {
        if (onStroke) {
            // TODO use stroke
            // s = stroke.createStrokedShape(s);
        }
        return s.intersects(rect);
    }

    @Override
    public java.awt.GraphicsConfiguration getDeviceConfiguration() {
        throw new NoSuchMethodError();
        // return new NetGraphicsConfiguration(Screen.PrimaryScreen);
    }

    @Override
    public void setComposite(java.awt.Composite comp) {
        if (javaComposite == comp) {
            return;
        }
        if (comp == null) {
            throw new java.lang.IllegalArgumentException("null Composite");
        }
        this.javaComposite = comp;
        java.awt.Paint oldPaint = getPaint(); // getPaint() is never null
        composite = CompositeHelper.Create(comp, getG());
        javaPaint = null;
        setPaint(oldPaint);
    }

    @Override
    public void setPaint(java.awt.Paint paint) {
        if (paint instanceof java.awt.Color) {
            setColor((java.awt.Color) paint);
            return;
        }

        if (paint == null || this.javaPaint == paint) {
            return;
        }
        this.javaPaint = paint;

        if (paint instanceof java.awt.GradientPaint) {
            java.awt.GradientPaint gradient = (java.awt.GradientPaint) paint;
            LinearGradientBrush linear;
            if (gradient.isCyclic()) {
                linear = new LinearGradientBrush(
                        J2C.ConvertPoint(gradient.getPoint1()),
                        J2C.ConvertPoint(gradient.getPoint2()),
                        composite.GetColor(gradient.getColor1()),
                        composite.GetColor(gradient.getColor2()));
            } else {
                // HACK because .NET does not support continue gradient like Java else Tile
                // Gradient
                // that we receize the rectangle very large (factor z) and set 4 color values
                // a exact solution will calculate the size of the Graphics with the current
                // transform
                Color color1 = composite.GetColor(gradient.getColor1());
                Color color2 = composite.GetColor(gradient.getColor2());
                float x1 = (float) gradient.getPoint1().getX();
                float x2 = (float) gradient.getPoint2().getX();
                float y1 = (float) gradient.getPoint1().getY();
                float y2 = (float) gradient.getPoint2().getY();
                float diffX = x2 - x1;
                float diffY = y2 - y1;
                final float z = 60; // HACK zoom factor, with a larger factor .NET will make the gradient wider.
                linear = new LinearGradientBrush(
                        new PointF(x1 - z * diffX, y1 - z * diffY),
                        new PointF(x2 + z * diffX, y2 + z * diffY),
                        color1,
                        color1);
                ColorBlend colorBlend = new ColorBlend(4);
                Color[] colors = colorBlend.get_Colors();
                colors[0] = colors[1] = color1;
                colors[2] = colors[3] = color2;
                float[] positions = colorBlend.get_Positions();
                positions[1] = z / (2 * z + 1);
                positions[2] = (z + 1) / (2 * z + 1);
                positions[3] = 1.0f;
                linear.set_InterpolationColors(colorBlend);
            }
            linear.set_WrapMode(WrapMode.wrap(WrapMode.TileFlipXY));
            brush = linear;
            pen.set_Brush(brush);
            return;
        }

        if (paint instanceof java.awt.TexturePaint) {
            java.awt.TexturePaint texture = (java.awt.TexturePaint) paint;
            Bitmap txtr = J2C.ConvertImage(texture.getImage());
            java.awt.geom.Rectangle2D anchor = texture.getAnchorRect();
            TextureBrush txtBrush;
            brush = txtBrush = new TextureBrush(txtr, new Rectangle(0, 0, txtr.get_Width(), txtr.get_Height()),
                    composite.GetImageAttributes());
            txtBrush.TranslateTransform((float) anchor.getX(), (float) anchor.getY());
            txtBrush.ScaleTransform((float) anchor.getWidth() / txtr.get_Width(),
                    (float) anchor.getHeight() / txtr.get_Height());
            txtBrush.set_WrapMode(WrapMode.wrap(WrapMode.Tile));
            pen.set_Brush(brush);
            return;
        }

        if (paint instanceof java.awt.LinearGradientPaint) {
            java.awt.LinearGradientPaint gradient = (java.awt.LinearGradientPaint) paint;
            PointF start = J2C.ConvertPoint(gradient.getStartPoint());
            PointF end = J2C.ConvertPoint(gradient.getEndPoint());

            java.awt.Color[] javaColors = gradient.getColors();
            ColorBlend colorBlend;
            Color[] colors;
            boolean noCycle = gradient.getCycleMethod() == java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
            if (noCycle) {
                // HACK because .NET does not support continue gradient like Java else Tile
                // Gradient
                // that we receize the rectangle very large (factor z) and set 2 additional
                // color values
                // an exact solution will calculate the size of the Graphics with the current
                // transform
                float diffX = end.get_X() - start.get_X();
                float diffY = end.get_Y() - start.get_Y();
                SizeF size = GetSize();
                // HACK zoom factor, with a larger factor .NET will make the gradient wider.
                float z = Math.min(10, Math.max(size.get_Width() / diffX, size.get_Height() / diffY));
                start.set_X(start.get_X() - z * diffX);
                start.set_Y(start.get_Y() - z * diffY);
                end.set_X(end.get_X() + z * diffX);
                end.set_Y(end.get_Y() + z * diffY);

                colorBlend = new ColorBlend(javaColors.length + 2);
                colors = colorBlend.get_Colors();
                float[] fractions = gradient.getFractions();
                float[] positions = colorBlend.get_Positions();
                for (int i = 0; i < javaColors.length; i++) {
                    colors[i + 1] = composite.GetColor(javaColors[i]);
                    positions[i + 1] = (z + fractions[i]) / (2 * z + 1);
                }
                colors[0] = colors[1];
                colors[colors.length - 1] = colors[colors.length - 2];
                positions[positions.length - 1] = 1.0f;
            } else {
                colorBlend = new ColorBlend(javaColors.length);
                colors = colorBlend.get_Colors();
                colorBlend.set_Positions(gradient.getFractions());
                for (int i = 0; i < javaColors.length; i++) {
                    colors[i] = composite.GetColor(javaColors[i]);
                }
            }
            LinearGradientBrush linear = new LinearGradientBrush(start, end, colors[0], colors[colors.length - 1]);
            linear.set_InterpolationColors(colorBlend);
            switch (gradient.getCycleMethod()) {
                case NO_CYCLE:
                case REFLECT:
                    linear.set_WrapMode(WrapMode.wrap(WrapMode.TileFlipXY));
                    break;
                case REPEAT:
                    linear.set_WrapMode(WrapMode.wrap(WrapMode.Tile));
                    break;
            }
            brush = linear;
            pen.set_Brush(brush);
            return;
        }

        if (paint instanceof java.awt.RadialGradientPaint) {
            java.awt.RadialGradientPaint gradient = (java.awt.RadialGradientPaint) paint;
            GraphicsPath path = new GraphicsPath();
            SizeF size = GetSize();

            PointF center = J2C.ConvertPoint(gradient.getCenterPoint());

            float radius = gradient.getRadius();
            int factor = (int) Math.ceil(Math.max(size.get_Width(), size.get_Height()) / radius);

            float diameter = radius * factor;
            path.AddEllipse(center.get_X() - diameter, center.get_Y() - diameter, diameter * 2, diameter * 2);

            java.awt.Color[] javaColors = gradient.getColors();
            float[] fractions = gradient.getFractions();
            int length = javaColors.length;
            ColorBlend colorBlend = new ColorBlend(length * factor);
            Color[] colors = colorBlend.get_Colors();
            float[] positions = colorBlend.get_Positions();

            for (int c = 0, j = length - 1; j >= 0;) {
                positions[c] = (1 - fractions[j]) / factor;
                colors[c++] = composite.GetColor(javaColors[j--]);
            }

            java.awt.MultipleGradientPaint.CycleMethod cycle = gradient.getCycleMethod();
            for (int f = 1; f < factor; f++) {
                int off = f * length;
                for (int c = 0, j = length - 1; j >= 0; j--, c++) {
                    switch (cycle) {
                        case REFLECT:
                            if (f % 2 == 0) {
                                positions[off + c] = (f + 1 - fractions[j]) / factor;
                                colors[off + c] = colors[c];
                            } else {
                                positions[off + c] = (f + fractions[c]) / factor;
                                colors[off + c] = colors[j];
                            }
                            break;
                        case NO_CYCLE:
                            positions[off + c] = (f + 1 - fractions[j]) / factor;
                            break;
                        default: // CycleMethod.REPEAT
                            positions[off + c] = (f + 1 - fractions[j]) / factor;
                            colors[off + c] = colors[c];
                            break;
                    }
                }
            }
            if (cycle == java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE && factor > 1) {
                System.arraycopy(colors, 0, colors, colors.length - length, length);
                Color color = colors[length - 1];
                for (int i = colors.length - length - 1; i >= 0; i--) {
                    colors[i] = color;
                }
            }

            PathGradientBrush pathBrush = new PathGradientBrush(path);
            pathBrush.set_CenterPoint(center);
            pathBrush.set_InterpolationColors(colorBlend);

            brush = pathBrush;
            pen.set_Brush(brush);
            return;
        }

        // generic paint to brush conversion for custom paints
        // the tranform of the graphics should not change between the creation and it
        // usage
        Matrix transform = getG().get_Transform(); // using?
        {
            SizeF size = GetSize();
            int width = (int) size.get_Width();
            int height = (int) size.get_Height();
            java.awt.Rectangle bounds = new java.awt.Rectangle(0, 0, width, height);

            java.awt.PaintContext context = paint.createContext(ColorModel.getRGBdefault(), bounds, bounds,
                    C2J.ConvertMatrix(transform), getRenderingHints());
            WritableRaster raster = (WritableRaster) context.getRaster(0, 0, width, height);
            BufferedImage txtrImage = new BufferedImage(context.getColorModel(), raster, true, null);
            Bitmap txtr = J2C.ConvertImage(txtrImage);

            TextureBrush txtBrush;
            brush = txtBrush = new TextureBrush(txtr, new Rectangle(0, 0, width, height),
                    composite.GetImageAttributes());
            transform.Invert();
            txtBrush.set_Transform(transform);
            txtBrush.set_WrapMode(WrapMode.wrap(WrapMode.Tile));
            pen.set_Brush(brush);
            return;
        }
    }

    @Override
    public void setStroke(java.awt.Stroke stroke) {
        if (this.stroke != null && this.stroke == stroke) {
            return;
        }
        this.stroke = stroke;
        if (stroke instanceof java.awt.BasicStroke) {
            java.awt.BasicStroke s = (java.awt.BasicStroke) stroke;

            pen = new Pen(pen.get_Brush(), s.getLineWidth());

            SetLineJoin(s);
            SetLineDash(s);
        } else {
            System.out.println("Unknown Stroke type: " + stroke.getClass().getName());
        }
    }

    private void SetLineJoin(java.awt.BasicStroke s) {
        pen.set_MiterLimit(s.getMiterLimit());
        pen.set_LineJoin(J2C.ConvertLineJoin(s.getLineJoin()));
    }

    private void SetLineDash(java.awt.BasicStroke s) {
        float[] dash = s.getDashArray();
        if (dash == null) {
            pen.set_DashStyle(DashStyle.wrap(DashStyle.Solid));
        } else {
            if (dash.length % 2 == 1) {
                int len = dash.length;
                dash = Arrays.copyOf(dash, len * 2);
                // Array.Copy(dash, 0, dash, len, len);
            }
            float lineWidth = s.getLineWidth();
            if (lineWidth > 1) // for values < 0 there is no correctur needed
            {
                for (int i = 0; i < dash.length; i++) {
                    // dividing by line thickness because of the representation difference
                    dash[i] = dash[i] / lineWidth;
                }
            }
            // To fix the problem where solid style in Java can be represented at { 1.0, 0.0
            // }.
            // In .NET, however, array can only have positive value
            if (dash.length == 2 && dash[dash.length - 1] == 0) {
                dash = Arrays.copyOf(dash, 1);
            }

            float dashPhase = s.getDashPhase();
            // correct the dash cap
            switch (s.getEndCap()) {
                case java.awt.BasicStroke.CAP_BUTT:
                    pen.set_DashCap(DashCap.wrap(DashCap.Flat));
                    break;
                case java.awt.BasicStroke.CAP_ROUND:
                    pen.set_DashCap(DashCap.wrap(DashCap.Round));
                    break;
                case java.awt.BasicStroke.CAP_SQUARE:
                    pen.set_DashCap(DashCap.wrap(DashCap.Flat));
                    // there is no equals DashCap in .NET, we need to emulate it
                    dashPhase += lineWidth / 2;
                    for (int i = 0; i < dash.length; i++) {
                        if (i % 2 == 0) {
                            dash[i] += 1;
                        } else {
                            dash[i] = Math.max(0.00001F, dash[i] - 1);
                        }
                    }
                    break;
                default:
                    System.out.println("Unknown dash cap type:" + s.getEndCap());
                    break;
            }

            // calc the dash offset
            if (lineWidth > 0) {
                // dividing by line thickness because of the representation difference
                pen.set_DashOffset(dashPhase / lineWidth);
            } else {
                // thickness == 0
                if (dashPhase > 0) {
                    lineWidth = 0.001F;
                    pen.set_Width(lineWidth); // hack to prevent a division with 0
                    pen.set_DashOffset(dashPhase / lineWidth);
                } else {
                    pen.set_DashOffset(0);
                }
            }

            // set the final dash pattern
            pen.set_DashPattern(dash);
        }
    }

    @Override
    public void setRenderingHint(java.awt.RenderingHints.Key hintKey, Object hintValue) {
        Graphics g = getG();
        if (hintKey == java.awt.RenderingHints.KEY_ANTIALIASING) {
            if (hintValue == java.awt.RenderingHints.VALUE_ANTIALIAS_DEFAULT) {
                g.set_SmoothingMode(SmoothingMode.wrap(SmoothingMode.Default));
                g.set_PixelOffsetMode(PixelOffsetMode.wrap(PixelOffsetMode.Default));
                return;
            }
            if (hintValue == java.awt.RenderingHints.VALUE_ANTIALIAS_OFF) {
                g.set_SmoothingMode(SmoothingMode.wrap(SmoothingMode.None));
                g.set_PixelOffsetMode(PixelOffsetMode.wrap(PixelOffsetMode.Default));
                return;
            }
            if (hintValue == java.awt.RenderingHints.VALUE_ANTIALIAS_ON) {
                g.set_SmoothingMode(SmoothingMode.wrap(SmoothingMode.AntiAlias));
                g.set_PixelOffsetMode(PixelOffsetMode.wrap(PixelOffsetMode.HighQuality));
                return;
            }
            return;
        }
        if (hintKey == java.awt.RenderingHints.KEY_INTERPOLATION) {
            if (hintValue == java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR) {
                g.set_InterpolationMode(InterpolationMode.wrap(InterpolationMode.HighQualityBilinear));
                return;
            }
            if (hintValue == java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC) {
                g.set_InterpolationMode(InterpolationMode.wrap(InterpolationMode.HighQualityBicubic));
                return;
            }
            if (hintValue == java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
                g.set_InterpolationMode(InterpolationMode.wrap(InterpolationMode.NearestNeighbor));
                return;
            }
            return;
        }
        if (hintKey == java.awt.RenderingHints.KEY_TEXT_ANTIALIASING) {
            if (hintValue == java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT ||
                    hintValue == java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_OFF) {
                setTextRenderingHint(TextRenderingHint.wrap(TextRenderingHint.SingleBitPerPixelGridFit));
                textAntialiasHint = hintValue;
                return;
            }
            if (hintValue == java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON) {
                setTextRenderingHint(TextRenderingHint.wrap(TextRenderingHint.AntiAlias));
                textAntialiasHint = hintValue;
                return;
            }
            return;
        }
        if (hintKey == java.awt.RenderingHints.KEY_FRACTIONALMETRICS) {
            if (hintValue == java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT ||
                    hintValue == java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_OFF ||
                    hintValue == java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON) {
                fractionalHint = hintValue;
            }
            return;
        }

    }

    @Override
    public Object getRenderingHint(java.awt.RenderingHints.Key hintKey) {
        return getRenderingHints().get(hintKey);
    }

    @Override
    public void setRenderingHints(java.util.Map hints) {
        addRenderingHints(hints);
        // TODO all not included values should reset to default, but was is default?
    }

    @Override
    public void addRenderingHints(java.util.Map hints) {
        Iterator iterator = hints.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
            setRenderingHint((java.awt.RenderingHints.Key) entry.getKey(), entry.getValue());
        }
    }

    @Override
    public java.awt.RenderingHints getRenderingHints() {
        Graphics g = getG();
        java.awt.RenderingHints hints = new java.awt.RenderingHints(null);
        switch (g.get_SmoothingMode().Value) {
            case SmoothingMode.Default:
                hints.put(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_DEFAULT);
                break;
            case SmoothingMode.None:
                hints.put(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
                break;
            case SmoothingMode.AntiAlias:
                hints.put(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                break;
        }

        switch (g.get_InterpolationMode().Value) {
            case InterpolationMode.Bilinear:
            case InterpolationMode.HighQualityBilinear:
                hints.put(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                break;
            case InterpolationMode.Bicubic:
            case InterpolationMode.HighQualityBicubic:
                hints.put(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                break;
            case InterpolationMode.NearestNeighbor:
                hints.put(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                break;
        }

        hints.put(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, textAntialiasHint);
        hints.put(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, fractionalHint);
        return hints;
    }

    @Override
    public void translate(double x, double y) {
        Graphics g = getG();
        Matrix transform = g.get_Transform();
        transform.Translate((float) x, (float) y);
        g.set_Transform(transform);
    }

    private static double RadiansToDegrees(double radians) {
        return radians * (180 / Math.PI);
    }

    @Override
    public void rotate(double theta) {
        Graphics g = getG();
        Matrix transform = g.get_Transform();
        transform.Rotate((float) RadiansToDegrees(theta));
        g.set_Transform(transform);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        Graphics g = getG();
        Matrix transform = g.get_Transform();
        transform.Translate((float) x, (float) y);
        transform.Rotate((float) RadiansToDegrees(theta));
        transform.Translate(-(float) x, -(float) y);
        g.set_Transform(transform);
    }

    @Override
    public void scale(double scaleX, double scaleY) {
        Graphics g = getG();
        Matrix transform = g.get_Transform(); // using?
        {
            transform.Scale((float) scaleX, (float) scaleY);
            g.set_Transform(transform);
        }
    }

    @Override
    public void shear(double shearX, double shearY) {
        Graphics g = getG();
        Matrix transform = g.get_Transform(); // using?
        {
            transform.Shear((float) shearX, (float) shearY);
            g.set_Transform(transform);
        }
    }

    @Override
    public void transform(java.awt.geom.AffineTransform tx) {
        Graphics g = getG();
        Matrix transform = g.get_Transform(); // using?
        Matrix matrix = J2C.ConvertTransform(tx);
        try {
            transform.Multiply(matrix);
            g.set_Transform(transform);
        } finally {
            matrix.Dispose();
        }
    }

    @Override
    public void setTransform(java.awt.geom.AffineTransform tx) {
        getG().set_Transform(J2C.ConvertTransform(tx));
    }

    @Override
    public java.awt.geom.AffineTransform getTransform() {
        Matrix matrix = getG().get_Transform(); // using?
        {
            return C2J.ConvertMatrix(matrix);
        }
    }

    @Override
    public java.awt.Paint getPaint() {
        if (javaPaint == null) {
            javaPaint = composite.GetColor(color);
        }
        return javaPaint;
    }

    @Override
    public java.awt.Composite getComposite() {
        return javaComposite;
    }

    @Override
    public void setBackground(java.awt.Color backcolor) {
        bgcolor = backcolor == null ? Color.Empty : Color.FromArgb(backcolor.getRGB());
    }

    @Override
    public java.awt.Color getBackground() {
        return bgcolor == Color.Empty ? null : new java.awt.Color(bgcolor.ToArgb(), true);
    }

    @Override
    public java.awt.Stroke getStroke() {
        if (stroke == null) {
            return defaultStroke;
        }
        return stroke;
    }

    void setTextRenderingHint(TextRenderingHint hint) {
        getG().set_TextRenderingHint(hint);
        baseline = getBaseline(netfont, hint);
    }

    /// <summary>
    /// Caclulate the baseline from a font and a TextRenderingHint
    /// </summary>
    /// <param name="font">the font</param>
    /// <param name="hint">the used TextRenderingHint</param>
    /// <returns></returns>
    private static int getBaseline(Font font, TextRenderingHint hint) {
        synchronized (baselines) {
            String key = font.ToString() + hint.ToString();
            int baseline;
            if (!baselines.containsKey(key)) {
                FontFamily family = font.get_FontFamily();
                FontStyle style = font.get_Style();
                float ascent = family.GetCellAscent(style);
                float lineSpace = family.GetLineSpacing(style);

                baseline = (int) Math.round(font.GetHeight() * ascent / lineSpace);

                // Until this point the calulation use only the Font. But with different
                // TextRenderingHint there are smal differences.
                // There is no API that calulate the offset from TextRenderingHint that we
                // messure it.
                final int w = 3;
                final int h = 3;

                Bitmap bitmap = new Bitmap(w, h);
                try {
                    Graphics g = Graphics.FromImage(bitmap);
                    g.set_TextRenderingHint(hint);
                    g.FillRectangle(new SolidBrush(Color.get_White()), 0, 0, w, h);
                    g.DrawString("A", font, new SolidBrush(Color.get_Black()), 0, -baseline, FORMAT);
                    g.DrawString("X", font, new SolidBrush(Color.get_Black()), 0, -baseline, FORMAT);
                    g.Dispose();

                    int y = 0;
                    LINE: while (y < h) {
                        for (int x = 0; x < w; x++) {
                            Color color = bitmap.GetPixel(x, y);
                            if (color.GetBrightness() < 0.5) {
                                // there is a black pixel, we continue in the next line.
                                baseline++;
                                y++;
                                continue LINE;
                            }
                        }
                        break; // there was a line without black pixel
                    }
                } finally {
                    bitmap.Dispose();
                }

                baselines.put(key, baseline);
            } else {
                baseline = baselines.get(key);
            }
            return baseline;
        }
    }

    private boolean isAntiAlias() {
        switch (getG().get_TextRenderingHint().Value) {
            case TextRenderingHint.AntiAlias:
            case TextRenderingHint.AntiAliasGridFit:
            case TextRenderingHint.ClearTypeGridFit:
                return true;
            default:
                return false;
        }
    }

    private boolean isFractionalMetrics() {
        return fractionalHint == java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
    }

    @Override
    public java.awt.font.FontRenderContext getFontRenderContext() {
        return new java.awt.font.FontRenderContext(getTransform(), isAntiAlias(), isFractionalMetrics());
    }

    @Override
    public void drawGlyphVector(java.awt.font.GlyphVector gv, float x, float y) {
        Graphics g = getG();
        java.awt.font.FontRenderContext frc = gv.getFontRenderContext();
        Matrix currentMatrix = null;
        Font currentFont = netfont;
        TextRenderingHint currentHint = g.get_TextRenderingHint();
        int currentBaseline = baseline;
        try {
            java.awt.Font javaFont = gv.getFont();
            if (javaFont != null) {
                netfont = javaFont.getNetFont();
            }
            TextRenderingHint hint;
            if (frc.isAntiAliased()) {
                if (frc.usesFractionalMetrics()) {
                    hint = TextRenderingHint.wrap(TextRenderingHint.AntiAlias);
                } else {
                    hint = TextRenderingHint.wrap(TextRenderingHint.AntiAliasGridFit);
                }
            } else {
                if (frc.usesFractionalMetrics()) {
                    hint = TextRenderingHint.wrap(TextRenderingHint.SingleBitPerPixel);
                } else {
                    hint = TextRenderingHint.wrap(TextRenderingHint.SingleBitPerPixelGridFit);
                }
            }
            g.set_TextRenderingHint(hint);
            baseline = getBaseline(netfont, hint);
            if (!frc.getTransform().equals(getTransform())) {
                // save the old context and use the transformation from the renderContext
                currentMatrix = g.get_Transform();
                g.set_Transform(J2C.ConvertTransform(frc.getTransform()));
            }
            drawString(J2C.ConvertGlyphVector(gv), x, y);
        } finally {
            // Restore the old context if needed
            g.set_TextRenderingHint(currentHint);
            baseline = currentBaseline;
            netfont = currentFont;
            if (currentMatrix != null) {
                g.set_Transform(currentMatrix);
            }
        }
    }
}
