/*
  Copyright (C) 2010 Volker Berlin (i-net software)

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

import cli.System.Drawing.Color;
import cli.System.Drawing.Graphics;
import cli.System.Drawing.Drawing2D.CompositingMode;
import cli.System.Drawing.Imaging.ColorMatrix;
import cli.System.Drawing.Imaging.ImageAttributes;

class CompositeHelper {
    private final ImageAttributes imageAttributes = new ImageAttributes();

    /// <summary>
    /// Create a default CompositeHelper. Is used from Create only.
    /// </summary>
    CompositeHelper() {
    }

    static CompositeHelper Create(java.awt.Composite comp, Graphics graphics) {
        if (comp instanceof java.awt.AlphaComposite) {
            java.awt.AlphaComposite alphaComp = (java.awt.AlphaComposite) comp;
            float alpha = alphaComp.getAlpha();
            switch (alphaComp.getRule()) {
                case java.awt.AlphaComposite.CLEAR:
                    graphics.set_CompositingMode(CompositingMode.wrap(CompositingMode.SourceCopy));
                    return new ClearCompositeHelper();
                case java.awt.AlphaComposite.SRC:
                    graphics.set_CompositingMode(CompositingMode.wrap(CompositingMode.SourceCopy));
                    break;
                case java.awt.AlphaComposite.SRC_OVER:
                    graphics.set_CompositingMode(CompositingMode.wrap(CompositingMode.SourceOver));
                    break;
                case java.awt.AlphaComposite.DST:
                    graphics.set_CompositingMode(CompositingMode.wrap(CompositingMode.SourceOver));
                    alpha = 0.0F;
                    break;
                default:
                    graphics.set_CompositingMode(CompositingMode.wrap(CompositingMode.SourceOver));
                    System.out.println("AlphaComposite with Rule " + alphaComp.getRule() + " not supported.");
                    break;
            }
            if (alpha == 1.0) {
                return new CompositeHelper();
            } else {
                return new AlphaCompositeHelper(alpha);
            }
        } else {
            graphics.set_CompositingMode(CompositingMode.wrap(CompositingMode.SourceOver));
            System.out.println("Composite not supported: " + comp.getClass().getName());
            return new CompositeHelper();
        }
    }

    int GetArgb(java.awt.Color color) {
        return color.getRGB();
    }

    Color GetColor(java.awt.Color color) {
        return color == null ? Color.Empty : Color.FromArgb(GetArgb(color));
    }

    int ToArgb(Color color) {
        return color.ToArgb();
    }

    java.awt.Color GetColor(Color color) {
        return color == Color.Empty ? null : new java.awt.Color(ToArgb(color), true);
    }

    /// <summary>
    /// Get the ImageAttributes instance. Does not change it bcause it is not a
    /// copy.
    /// </summary>
    /// <returns></returns>
    ImageAttributes GetImageAttributes() {
        return imageAttributes;
    }
}

final class AlphaCompositeHelper extends CompositeHelper {
    private final float alpha;

    /// <summary>
    /// Create a AlphaCompositeHelper
    /// </summary>
    /// <param name="alpha">a value in the range from 0.0 to 1.0</param>
    AlphaCompositeHelper(float alpha) {
        this.alpha = alpha;
        ColorMatrix matrix = new ColorMatrix();
        matrix.set_Matrix33(alpha);
        GetImageAttributes().SetColorMatrix(matrix);
    }

    @Override
    int GetArgb(java.awt.Color color) {
        int argb = color.getRGB();
        int newAlpha = (int) ((0xff000000 & argb) * alpha + (float) 0x800000);
        int newArgb = (0xff000000 & newAlpha) | (0xffffff & argb);
        return newArgb;
    }

    @Override
    int ToArgb(Color color) {
        int argb = color.ToArgb();
        int newAlpha = (int) ((0xff000000 & argb) / alpha + (float) 0x800000);
        int newArgb = (0xff000000 & newAlpha) | (0xffffff & argb);
        return newArgb;
    }
}

final class ClearCompositeHelper extends CompositeHelper {

    ClearCompositeHelper() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.set_Matrix00(0f);
        matrix.set_Matrix11(0f);
        matrix.set_Matrix22(0f);
        matrix.set_Matrix33(0f);
        GetImageAttributes().SetColorMatrix(matrix);
    }

    @Override
    int GetArgb(java.awt.Color color) {
        return 0;
    }
}
