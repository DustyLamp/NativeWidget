package net.realapps.nativewidget;

class PluginRegistrantException extends RuntimeException {
    public PluginRegistrantException() {
        super(
                "PluginRegistrantCallback is not set. Did you forget to call "
                        + "NativeWidget.setPluginRegistrant? See the README for instructions.");
    }
}