package ikvm.awt;

import cli.System.Drawing.FontFamily;
import cli.System.Drawing.Text.InstalledFontCollection;

import java.awt.GraphicsDevice;
import java.awt.image.BufferedImage;
import java.util.Locale;

import sun.java2d.SunGraphicsEnvironment;

public class NetGraphicsEnviroment extends SunGraphicsEnvironment {

	@Override
	public boolean isDisplayLocal() {
		throw new NoSuchMethodError();
	}

	@Override
	public java.awt.Graphics2D createGraphics(BufferedImage bi) {
		return new BitmapGraphics(bi.getBitmap(), bi);
	}

	@Override
	public java.awt.Font[] getAllFonts() {
		throw new NoSuchMethodError();
	}

	@Override
	public String[] getAvailableFontFamilyNames() {
		throw new NoSuchMethodError();
	}

	@Override
	public String[] getAvailableFontFamilyNames(Locale locale) {
		throw new NoSuchMethodError();
	}

	@Override
	public GraphicsDevice getDefaultScreenDevice() {
		throw new NoSuchMethodError();
	}

	@Override
	public GraphicsDevice[] getScreenDevices() {
		throw new NoSuchMethodError();
	}
}
