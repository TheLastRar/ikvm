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

import cli.System.Drawing.Brush;
import cli.System.Drawing.Pen;
import cli.System.Drawing.SolidBrush;
import cli.System.Drawing.Region;
import cli.System.Drawing.Drawing2D.CompositingMode;
import cli.System.Drawing.Drawing2D.InterpolationMode;
import cli.System.Drawing.Drawing2D.Matrix;
import cli.System.Drawing.Drawing2D.PixelOffsetMode;
import cli.System.Drawing.Drawing2D.SmoothingMode;
import cli.System.Drawing.Text.TextRenderingHint;

/// <summary>
/// State to store/restore the state of a NetGraphics/Graphics object
/// </summary>
class NetGraphicsState {
    private Brush brush;
    private Pen pen;

    // Graphics State
    private Matrix Transform;
    private Region Clip;
    private SmoothingMode SmoothingMode;
    private PixelOffsetMode PixelOffsetMode;
    private TextRenderingHint TextRenderingHint;
    private InterpolationMode InterpolationMode;
    private CompositingMode CompositingMode;

    private boolean savedGraphics = false;

    public NetGraphicsState() {
    }

    public NetGraphicsState(NetGraphics netG) {
        saveGraphics(netG);
    }

    public void saveGraphics(NetGraphics netG) {
        if (netG == null) {
            return;
        }
        if (netG.getG() != null) {
            this.Transform = netG.getG().get_Transform();
            this.Clip = netG.getG().get_Clip();
            this.SmoothingMode = netG.getG().get_SmoothingMode();
            this.PixelOffsetMode = netG.getG().get_PixelOffsetMode();
            this.TextRenderingHint = netG.getG().get_TextRenderingHint();
            this.InterpolationMode = netG.getG().get_InterpolationMode();
            this.CompositingMode = netG.getG().get_CompositingMode();
            savedGraphics = true;
        }
        if (netG.pen != null && netG.brush != null) {
            pen = (Pen) netG.pen.Clone();
            brush = (Brush) netG.brush.Clone();
        }
    }

    public void restoreGraphics(NetGraphics netG) {
        if (netG == null) {
            return;
        }
        if (netG.getG() != null) {
            if (savedGraphics) {
                netG.getG().set_Transform(Transform);
                netG.getG().set_Clip(Clip);
                netG.getG().set_SmoothingMode(SmoothingMode);
                netG.getG().set_PixelOffsetMode(PixelOffsetMode);
                netG.setTextRenderingHint(TextRenderingHint);
                netG.getG().set_InterpolationMode(InterpolationMode);
                netG.getG().set_CompositingMode(CompositingMode);
            } else {
                // default values that Java used
                netG.getG().set_InterpolationMode(InterpolationMode.wrap(InterpolationMode.NearestNeighbor));
            }
        }
        if (pen != null && brush != null) {
            netG.pen = (Pen) pen.Clone();
            netG.brush = (Brush) brush.Clone();
        } else {
            netG.pen = new Pen(netG.color);
            netG.brush = new SolidBrush(netG.color);
            netG.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        }
    }
}
