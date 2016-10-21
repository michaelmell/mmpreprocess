package com.jug.mmpreprocess.fijiplugins;

import com.jug.mmpreprocess.MMPreprocess;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Date: October 2016
 */
public class MMPreprocessingPlugin implements PlugIn {

    @Override
    public void run(String s) {
        String currentDir = Prefs.getDefaultDirectory();


        GenericDialogPlus gd = new GenericDialogPlus("MMPreprocessing Configuration");
        gd.addDirectoryField("Input_folder", currentDir);
        gd.addDirectoryField("Output_folder", currentDir);
        gd.addNumericField("Number_of_Channels", 2, 0);
        gd.addNumericField("Channels_start_with (usually 0 or 1)", 1, 0);
        gd.addNumericField("Number_of_Time_points", 40, 0);
        gd.addNumericField("Time_points_start_with (usually 0 or 1)", 1, 0);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        String inputFolder = gd.getNextString();
        String outputFolder = gd.getNextString();
        int numberOfChannels = (int)gd.getNextNumber();
        int channelStartIndex = (int)gd.getNextNumber();
        int numberOfTimePoints = (int)gd.getNextNumber();
        int timePointStartIndex = (int)gd.getNextNumber();





        IJ.log("Hello preprocessing");

        String[] args = {
                "mmpreprocess",
                "-i",
                inputFolder,
                "-o",
                outputFolder,
                "-c",
                "" + (numberOfChannels),
                "-cmin",
                "" + channelStartIndex,
                "-tmin",
                "" + timePointStartIndex,
                "-tmax",
                "" + (timePointStartIndex + numberOfTimePoints - 1)
        };

        for (String param : args) {
            IJ.log("mmp params " + param);
        }


                /*new String[13];
        args[0] = "mmpreprocess";
        args[1] = "-i";
        args[2] = "/Users/rhaase/Projects/Florian_Jug_Myers_MoMA_Deployment/data/registered2";
        args[3] = "-o";
        args[4] = "/Users/rhaase/Projects/Florian_Jug_Myers_MoMA_Deployment/data/splitted";
        args[5] = "-c";
        args[6] = "2";
        args[7] = "-cmin";
        args[8] = "1";
        args[9] = "-tmin";
        args[10] = "1";
        args[11] = "-tmax";
        args[12] = "40";
        */
        MMPreprocess.running_as_Fiji_plugin = true;

        MMPreprocess.main(args);
    }



}
