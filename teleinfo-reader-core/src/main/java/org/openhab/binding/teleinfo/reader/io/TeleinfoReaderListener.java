package org.openhab.binding.teleinfo.reader.io;

import org.openhab.binding.teleinfo.reader.Frame;

public interface TeleinfoReaderListener {

    void onFrameReceived(final TeleinfoReader reader, final Frame frame);

}