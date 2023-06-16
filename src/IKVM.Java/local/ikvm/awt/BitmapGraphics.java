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
import cli.System.Drawing.Graphics;
import cli.System.Drawing.GraphicsUnit;
import cli.System.Drawing.Size;
import cli.System.Drawing.SizeF;
import cli.System.Drawing.Rectangle;

import java.awt.image.BufferedImage;

class BitmapGraphics extends NetGraphics {
    private final Bitmap bitmap;
    private final BufferedImage image;

    BitmapGraphics(Bitmap bitmap, Object destination, java.awt.Font font, Color fgcolor, Color bgcolor) {
        super(createGraphics(bitmap), destination, font, fgcolor, bgcolor);
        this.bitmap = bitmap;
        image = destination instanceof BufferedImage ? (BufferedImage) destination : null;
    }

    BitmapGraphics(Bitmap bitmap, Object destination) {
        this(bitmap, destination, null, Color.get_White(), Color.get_Black());
    }

    @Override
    Graphics getG() {
        if (image != null) {
            image.toBitmap();
        }
        return super.getG();
    }

    @Override
    protected SizeF GetSize() {
        return Size.op_Implicit(bitmap.get_Size());
    }

    private static Graphics createGraphics(Bitmap bitmap) {
        // lock to prevent the exception
        // System.InvalidOperationException: Object is currently in use elsewhere
        synchronized (bitmap) {
            return Graphics.FromImage(bitmap);
        }
    }

    @Override
    public java.awt.Graphics create() {
        try {
            BitmapGraphics newGraphics = (BitmapGraphics) clone();
            newGraphics.init(createGraphics(bitmap));
            return newGraphics;
        } catch (CloneNotSupportedException e) {
            System.out.println("Create Failed");
            return null;
        }
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        Bitmap copy = new Bitmap(width, height);
        try {
            Graphics gCopy = Graphics.FromImage(copy);
            try {
                gCopy.DrawImage(bitmap, new Rectangle(0, 0, width, height), x, y, width, height,
                        GraphicsUnit.wrap(GraphicsUnit.Pixel));
            } finally {
                gCopy.Dispose();
            }
            getG().DrawImageUnscaled(copy, x + dx, y + dy);
        } finally {
            copy.Dispose();
        }
    }
}
