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


        // -----------------------------------------
        // plugin configuration
        GenericDialogPlus gd = new GenericDialogPlus("MMPreprocessing Configuration");
        if (s.equals("file")) {
            gd.addFileField("Input_file", currentDir);
        } else {
            gd.addDirectoryField("Input_folder", currentDir);
        }
        gd.addDirectoryField("Output_folder", currentDir);
        gd.addNumericField("Number_of_Time_points", 40, 0);
        gd.addNumericField("Time_points_start_with (usually 0 or 1)", 1, 0);
        gd.addMessage("Advanced parameters");
        gd.addNumericField("Variance_threshold", 0.001, 8);
        gd.addNumericField("Lateral_offset", 40, 0);
        gd.addNumericField("Crop_width", 100, 0);
        gd.addCheckbox("Save results as image sequence", false);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        String inputFolderOrFile = gd.getNextString();
        String outputFolder = gd.getNextString();
        int numberOfTimePoints = (int)gd.getNextNumber();
        int timePointStartIndex = (int)gd.getNextNumber();
        double varianceThreshold = gd.getNextNumber();
        int lateralOffset = (int)gd.getNextNumber();
        int cropWidth = (int)gd.getNextNumber();
        boolean saveResultsAsImageSequence = gd.getNextBoolean();

        String[] args = {
                "mmpreprocess",
                "-i",
                inputFolderOrFile,
                "-o",
                outputFolder,
                "-tmin",
                "" + timePointStartIndex,
                "-tmax",
                "" + (timePointStartIndex + numberOfTimePoints - 1),
                "-vt",
                "" + varianceThreshold,
                "-lo",
                "" + lateralOffset,
                "-cw",
                "" + cropWidth,
                saveResultsAsImageSequence?"-so":""
        };

        // -----------------------------------------
        // for tracing
        for (String param : args) {
            IJ.log("mmp params " + param);
        }

        // -----------------------------------------
        // Actually run MMPreprocess
        MMPreprocess.running_as_Fiji_plugin = true;
        MMPreprocess.main(args);
    }



}
