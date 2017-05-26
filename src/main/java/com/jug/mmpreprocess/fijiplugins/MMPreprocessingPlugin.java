package com.jug.mmpreprocess.fijiplugins;

import com.jug.mmpreprocess.MMPreprocess;

import fiji.util.gui.GenericDialogPlus;
import ij.Prefs;
import ij.plugin.PlugIn;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Date: October 2016
 */
public class MMPreprocessingPlugin implements PlugIn {

    @Override
    public void run(final String s) {
        final String currentDir = Prefs.getDefaultDirectory();


        // -----------------------------------------
        // plugin configuration
        final GenericDialogPlus gd = new GenericDialogPlus("MMPreprocessing Configuration");
        if (s.equals("file")) {
            gd.addFileField("Input_file", currentDir);
        } else {
            gd.addDirectoryField("Input_folder", currentDir);
        }
        gd.addDirectoryField("Output_folder", currentDir);
        gd.addNumericField("Number_of_Time_points (enter -1 to process all)", -1, 0);
        gd.addNumericField("Time_points_start_with (usually 0 or 1)", 1, 0);

		gd.addMessage( "Full-view parameters:" );
		gd.addCheckbox( "Auto_rotation (based on bright main channel)", true );
		gd.addNumericField( "Variance_threshold", 0.001, 8 );
		gd.addNumericField( "GL_min_length (in pixel)", 250, 0 );
		gd.addNumericField( "Row_smoothing_sigma (in pixel)", 20, 1 );
		gd.addNumericField( "Lateral_offset (in pixel)", 40, 0 );

		gd.addMessage( "Fluorescence preproc? (Auto-rot off?)" );
		gd.addCheckbox( "No_phase_contrast", false );
		gd.addNumericField( "Fake_GL_width (in pixel)", -1, 0 );

		gd.addMessage( "Single-channel parameters:" );
		gd.addNumericField( "Crop_width (in pixel)", 100, 0 );
		gd.addNumericField( "Top_padding (in pixel)", 20, 1 );
		gd.addNumericField( "Bottom_padding (in pixel)", 20, 1 );

		gd.addMessage( "Output related parameters:" );
		gd.addCheckbox( "Results_as_sequence", false );
		gd.addCheckbox( "Show_debug_output", false );

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        final String inputFolderOrFile = gd.getNextString();
        final String outputFolder = gd.getNextString();
        final int numberOfTimePoints = (int)gd.getNextNumber();
        final int timePointStartIndex = (int)gd.getNextNumber();

		final boolean doAutoRotation = gd.getNextBoolean();
        final double varianceThreshold = gd.getNextNumber();
		final int glMinLength = ( int ) gd.getNextNumber();
		final double rowSmoothingSigma = gd.getNextNumber();
        final int lateralOffset = (int)gd.getNextNumber();

        final boolean isFluoPreprocessing = gd.getNextBoolean();
        final int fakeGLWidth = ( int ) gd.getNextNumber();

        final int cropWidth = (int)gd.getNextNumber();
		final int topPadding = ( int ) gd.getNextNumber();
		final int bottomPadding = ( int ) gd.getNextNumber();

		final boolean saveResultsAsImageSequence = gd.getNextBoolean();
		final boolean showDebugOutput = gd.getNextBoolean();

        final String[] args = {
                "mmpreprocess",
                "-i",
                inputFolderOrFile,
                "-o",
                outputFolder,
                "-tmin",
                "" + timePointStartIndex,
                "-tmax",
                "" + (timePointStartIndex + numberOfTimePoints - 1),
                isFluoPreprocessing?"-fluo":"",
                "-fake_gl_width",
                "" + fakeGLWidth,
                "-vt",
                "" + varianceThreshold,
                "-gl_minl",
                "" + glMinLength,
                "-lo",
                "" + lateralOffset,
                "-cw",
                "" + cropWidth,
                saveResultsAsImageSequence?"-so":"",
                "-s",
                "" + rowSmoothingSigma,
                "-tp",
                "" + topPadding,
                "-bp",
                "" + bottomPadding,
                doAutoRotation?"":"-norotation",
                showDebugOutput?"-d":""
        };

        // -----------------------------------------
        // for tracing
        System.out.println("mmp parameters: ");
        for (final String param : args) {
            System.out.println(" " + param);
        }

        // -----------------------------------------
        // Actually run MMPreprocess
        MMPreprocess.running_as_Fiji_plugin = true;
        MMPreprocess.main(args);
    }



}
