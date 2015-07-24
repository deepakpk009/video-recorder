/*-----------------------------------
VideoRecorder v0.1
-------------------------------------
a video recorder module using JMF
-------------------------------------
Developed By : deepak pk
Email : deepakpk009@yahoo.in
-------------------------------------
This Project is Licensed under LGPL
-------------------------------------

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.deepak.videorecorder;

import java.awt.*;
import java.io.IOException;
import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.datasink.*;

/*
 * class which provides the video recorder
 */
public class VideoRecorder {

    // the media locator
    private MediaLocator mediaLocator = null;
    // player media handler for rendering and controlling time based media data
    private Player player = null;
    // The Processor processes data and creates an output in the destination
    // format required
    private Processor processor = null;
    // takes a DataSource as input and renders the output to a specified destination
    private DataSink dataSink = null;
    private TheDataSinkListener dataSinkListener = null;

    /*
     * method to start recording the video and save it to the file specified
     */
    public void startRecording(String saveFile) throws NoDataSourceException, IOException, NoPlayerException, CannotRealizeException, NoDataSinkException, IncompatibleSourceException {
        // get the default video device
        mediaLocator = new MediaLocator("vfw://0");

        //create a source from the device
        DataSource dataSource = null;
        dataSource = Manager.createDataSource(mediaLocator);

        /*
         * Clone the video source so it can be displayed and used to capture
         * the video at the same time. Trying to use the same source for two
         * purposes would cause a "source is in use" error
         */
        DataSource cloneableDS = Manager.createCloneableDataSource(dataSource);
        DataSource PlayerVidDS = cloneableDS;

        // The video capture code will use the clone which is controlled by the player
        DataSource CaptureVidDS = ((javax.media.protocol.SourceCloneable) cloneableDS).createClone();

        /*
         * Display video by starting the player on the source clonable data source
         * the clones are fed data stopping the player will stop the video flow
         * to the clone data source
         */
        player = Manager.createRealizedPlayer(PlayerVidDS);
        player.start();

        // get the default audio device and create an audio data source
        mediaLocator = new MediaLocator("javasound://0");
        DataSource audioDataSource = Manager.createDataSource(mediaLocator);

        // merge audio and video data sources
        DataSource mixedDataSource = null;
        DataSource dsArray[] = new DataSource[2];
        // this is a cloned datasource and is controlled by the master clonable data source
        dsArray[0] = CaptureVidDS;
        dsArray[1] = audioDataSource;
        mixedDataSource = javax.media.Manager.createMergingDataSource(dsArray);

        // setup output file format to msvideo
        FileTypeDescriptor outputType = new FileTypeDescriptor(FileTypeDescriptor.MSVIDEO);
        // setup output video and audio data format
        Format outputFormat[] = new Format[2];
        //outputFormat[0] = new VideoFormat(VideoFormat.RGB);
        outputFormat[0] = new VideoFormat(VideoFormat.YUV);
        outputFormat[1] = new AudioFormat(AudioFormat.LINEAR);

        // create a new processor
        ProcessorModel processorModel = new ProcessorModel(mixedDataSource, outputFormat, outputType);
        processor = Manager.createRealizedProcessor(processorModel);

        // get the output of the processor to be used as the datasink input
        DataSource source = processor.getDataOutput();

        // create a File protocol MediaLocator with the location of the file to which bits are to be written
        MediaLocator mediadestination = new MediaLocator("file:" + saveFile);
        // create a datasink to create the video file
        dataSink = Manager.createDataSink(source, mediadestination);
        // create a listener to control the datasink
        dataSinkListener = new TheDataSinkListener();
        dataSink.addDataSinkListener(dataSinkListener);
        dataSink.open();

        // now start the datasink and processor
        dataSink.start();
        processor.start();
    }

    /**
     * method to stop the recording process
     */
    public void stopRecording() {
        // Stop the processor doing the movie capture first
        processor.stop();
        processor.close();

        // Closing the processor will end the data stream to the data sink.
        // Wait for the end of stream to occur before closing the datasink
        dataSinkListener.waitEndOfStream(10);
        dataSink.close();

        // stop and close the player which closes the video data source
        player.stop();
        player.close();

    }

    /*
     * method which returns the the visual component for display in gui
     */
    public Component getVisualComponent() {
        return player.getVisualComponent();
    }
}

/**
 *Control the ending of the program prior to closing the data sink
 */
class TheDataSinkListener implements DataSinkListener {

    boolean endOfStream = false;

    // Flag the ending of the data stream
    public void dataSinkUpdate(DataSinkEvent event) {
        if (event instanceof javax.media.datasink.EndOfStreamEvent) {
            endOfStream = true;
        }
    }

    /**
     * Cause the current thread to sleep if the data stream is still available.
     * This makes certain that JMF threads are done prior to closing the data sink
     * and finalizing the output file
     */
    public void waitEndOfStream(long checkTimeMs) {
        while (!endOfStream) {
            try {
                //Thread.currentThread().sleep(checkTimeMs);
                Thread.sleep(checkTimeMs);
            } catch (InterruptedException ie) {
                //exception handling here
            }
        }
    }
}
