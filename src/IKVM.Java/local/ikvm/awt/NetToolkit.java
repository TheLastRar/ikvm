package ikvm.awt;

import java.net.URL;
import java.util.Map;

public final class NetToolkit extends sun.awt.SunToolkit implements ikvm.awt.IkvmToolkit {
    public NetToolkit() {
    }

    @Override
    protected java.awt.EventQueue getSystemEventQueueImpl() {
        throw new NoSuchMethodError();
    }

    @Override
    protected void loadSystemColors(int[] systemColors) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.ButtonPeer createButton(java.awt.Button target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.CanvasPeer createCanvas(java.awt.Canvas target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.PanelPeer createPanel(java.awt.Panel target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.TextFieldPeer createTextField(java.awt.TextField target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.LabelPeer createLabel(java.awt.Label target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.ListPeer createList(java.awt.List target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.CheckboxPeer createCheckbox(java.awt.Checkbox target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.ScrollbarPeer createScrollbar(java.awt.Scrollbar target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.ScrollPanePeer createScrollPane(java.awt.ScrollPane target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.TextAreaPeer createTextArea(java.awt.TextArea target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.ChoicePeer createChoice(java.awt.Choice target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.FramePeer createFrame(java.awt.Frame target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.WindowPeer createWindow(java.awt.Window target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.DialogPeer createDialog(java.awt.Dialog target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.MenuBarPeer createMenuBar(java.awt.MenuBar target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.MenuPeer createMenu(java.awt.Menu target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.PopupMenuPeer createPopupMenu(java.awt.PopupMenu target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.MenuItemPeer createMenuItem(java.awt.MenuItem target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.FileDialogPeer createFileDialog(java.awt.FileDialog target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.CheckboxMenuItemPeer createCheckboxMenuItem(java.awt.CheckboxMenuItem target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.FontPeer getFontPeer(String name, int style) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.KeyboardFocusManagerPeer getKeyboardFocusManagerPeer() {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Dimension getScreenSize() {
        throw new NoSuchMethodError();
    }

    @Override
    public int getScreenResolution() {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.image.ColorModel getColorModel() {
        throw new NoSuchMethodError();
    }

    @Override
    public void sync() {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Image getImage(String filename) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Image getImage(URL url) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Image createImage(String filename) {
        return getImage(filename);
    }

    @Override
    public java.awt.Image createImage(URL url) {
        return getImage(url);
    }

    @Override
    public java.awt.Image createImage(byte[] imagedata, int imageoffset, int imagelength) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.PrintJob getPrintJob(java.awt.Frame frame, String jobtitle, java.util.Properties props) {
        throw new NoSuchMethodError();
    }

    @Override
    public void beep() {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.datatransfer.Clipboard getSystemClipboard() {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.dnd.DragGestureRecognizer createDragGestureRecognizer(java.lang.Class abstractRecognizerClass,
            java.awt.dnd.DragSource ds, java.awt.Component c, int srcActions, java.awt.dnd.DragGestureListener dgl) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.dnd.peer.DragSourceContextPeer createDragSourceContextPeer(java.awt.dnd.DragGestureEvent dge) {
        throw new NoSuchMethodError();
    }

    @Override
    public Map mapInputMethodHighlight(java.awt.im.InputMethodHighlight highlight) {
        throw new NoSuchMethodError();
    }

    @Override
    protected java.awt.peer.DesktopPeer createDesktopPeer(java.awt.Desktop target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Dimension getBestCursorSize(int preferredWidth, int preferredHeight) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Cursor createCustomCursor(java.awt.Image cursor, java.awt.Point hotSpot, String name) {
        throw new NoSuchMethodError();
    }

    @Override
    protected void initializeDesktopProperties() {
        // throw new NoSuchMethodError();
    }

    @Override
    protected Object lazilyLoadDesktopProperty(String name) {
        System.out.println(name);
        return null;
    }

    @Override
    protected java.awt.peer.MouseInfoPeer getMouseInfoPeer() {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean areExtraMouseButtonsEnabled() {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Insets getScreenInsets(java.awt.GraphicsConfiguration gc) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean isFrameStateSupported(int state) {
        throw new NoSuchMethodError();
    }

    // Implementations of interface IkvmToolkit

    @Override
    public sun.print.PrintPeer getPrintPeer() {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Shape outline(java.awt.Font javaFont, java.awt.font.FontRenderContext frc, String text, float x,
            float y) {
        throw new NoSuchMethodError();
    }

    // Implementations of interface SunToolkit

    @Override
    public boolean isModalExclusionTypeSupported(java.awt.Dialog.ModalExclusionType dmet) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean isModalityTypeSupported(java.awt.Dialog.ModalityType type) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.Window createInputMethodWindow(String __p1, sun.awt.im.InputContext __p2) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.RobotPeer createRobot(java.awt.Robot r, java.awt.GraphicsDevice screen) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.SystemTrayPeer createSystemTray(java.awt.SystemTray target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.TrayIconPeer createTrayIcon(java.awt.TrayIcon target) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.im.spi.InputMethodDescriptor getInputMethodAdapterDescriptor() {
        throw new NoSuchMethodError();
    }

    @Override
    public int getScreenHeight() {
        throw new NoSuchMethodError();
    }

    @Override
    public int getScreenWidth() {
        throw new NoSuchMethodError();
    }

    @Override
    public void grab(java.awt.Window window) {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean isDesktopSupported() {
        throw new NoSuchMethodError();
    }

    @Override
    public boolean isTraySupported() {
        throw new NoSuchMethodError();
    }

    @Override
    protected boolean syncNativeQueue(long l) {
        throw new NoSuchMethodError();
    }

    @Override
    public void ungrab(java.awt.Window window) {
        throw new NoSuchMethodError();
    }

    @Override
    public java.awt.peer.FramePeer createLightweightFrame(sun.awt.LightweightFrame lf) {
        throw new NoSuchMethodError();
    }

    @Override
    public sun.awt.datatransfer.DataTransferer getDataTransferer() {
        throw new NoSuchMethodError();
    }
}
