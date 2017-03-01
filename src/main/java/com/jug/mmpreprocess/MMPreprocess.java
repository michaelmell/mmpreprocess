/**
 *
 */
package com.jug.mmpreprocess;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.plugin.StackWriter;
import java.io.File;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author jug
 */
public class MMPreprocess {

	public static final int EXIT_STATUS_COULDNOTLOAD = 1;
	public static final int EXIT_STATUS_COULDNOTLOAD_AS_FLOATTYPE = 1;

	public static boolean running_as_Fiji_plugin = false;

	// things to come via command-line arguments
	private static String OUTPUT_PATH = "";
	private static int MIN_CHANNEL_IDX = 1;
	private static int NUM_CHANNELS = 2;
	private static int MIN_TIME = -1;
	private static int MAX_TIME = -1;

	private static File inputFolder;
	private static File outputFolder;

	// global parameters
	private static double INTENSITY_THRESHOLD;   // set in parseCommandLineArgs()
	private static final int BOTTOM_PADDING = 25;
	private static final int TOP_PADDING = 25;
	private static final int GL_MIN_LENGTH = 250;
	private static double VARIANCE_THRESHOLD = 0.001;// 0.00001; // may be modified in parseCommandLineArgs()
	private static int LATERAL_OFFSET = 40; //10; // may be modified in parseCommandLineArgs()
	private static int GL_CROP_WIDTH = 100; //40; // may be modified in parseCommandLineArgs()

	/**
	 * @param args
	 */
	public static void main( final String[] args ) {
		parseCommandLineArgs( args );

		// assemble file-list to process
		final MMDataSource dataSource =
				new MMDataSource( inputFolder, NUM_CHANNELS, MIN_CHANNEL_IDX, MIN_TIME, MAX_TIME );

		// compute tilt angle
		final MMDataFrame firstFrame = dataSource.getFrame( 0 );
		final MMDataFrame lastFrame = dataSource.getFrame( dataSource.size() - 1 );
		final double angle1 = MMUtils.computeTiltAngle( firstFrame, INTENSITY_THRESHOLD );
		final double angle2 = MMUtils.computeTiltAngle( lastFrame, INTENSITY_THRESHOLD );
		System.out.println( "\n" );
		System.out.println( "Angle for  1st frame: " + angle1 );
		System.out.println( "Angle for last frame: " + angle2 );
		System.out.println( "" );

		// safety net
		double angle = ( angle1 + angle2 ) / 2;
		if ( Math.abs( angle1 - angle2 ) > 0.5 ) {
//			if ( angle1 != 0 ) {
			System.out.println( "Angles are very different -- use only angle of 1st frame!" );
			angle = angle1;
//			} else {
//				System.out.println( "Angles are very different -- use only angle of lat frame (because he one of the first frame is '0.0'!" );
//				angle = angle2;
//			}
		}

		// rotate and compute crop ROI
		firstFrame.rotate( angle, BOTTOM_PADDING );
		final CropArea tightCropArea =
				firstFrame.computeTightCropArea(
						VARIANCE_THRESHOLD,
						GL_MIN_LENGTH,
						TOP_PADDING,
						BOTTOM_PADDING );

		//TODO average some to do stuff below, here I only take 1st frame
		firstFrame.crop( tightCropArea );

		// compute GL crop areas
		final List< CropArea > glCropAreas =
				firstFrame.computeGrowthLaneCropAreas( LATERAL_OFFSET, GL_CROP_WIDTH );

		// debug
		firstFrame.setGLCropAreas( glCropAreas );
		firstFrame.saveGLCropsTo( outputFolder, true );

		// crop GLs out of frames
		for ( int f = 0; f < dataSource.size(); f++ ) {
			final MMDataFrame frame = dataSource.getFrame( f );
			frame.readImageDataIfNeeded();
			if ( f > 0 ) { // first one is already modified at this point (see above)
				frame.rotate( angle, BOTTOM_PADDING );
				frame.crop( tightCropArea );
			}

			frame.setGLCropAreas( glCropAreas );
			frame.saveGLCropsTo( outputFolder );

			frame.dropImageData();
		}

//		new ImageJ();
//		ImageJFunctions.show( firstFrame.getChannel( 0 ), "DEBUG" );
		if (!running_as_Fiji_plugin) {
			System.exit(0);
		} else {
			return;
		}
	}

	/**
	 * @param args
	 */
	private static void parseCommandLineArgs( final String[] args ) {

		// create Options object & the parser
		final Options options = new Options();
		final CommandLineParser parser = new BasicParser();
		// defining command line options
		final Option help = new Option( "help", "print this message" );

		final Option timeFirst =
				new Option( "tmin", "min_time", true, "first time-point to be processed" );
		timeFirst.setRequired( false );

		final Option timeLast =
				new Option( "tmax", "max_time", true, "last time-point to be processed" );
		timeLast.setRequired( false );

		final Option numChannelsOption =
				new Option( "c", "channels", true, "number of channels to be loaded and analyzed." );
		numChannelsOption.setRequired( true );

		final Option minChannelIdxOption =
				new Option( "cmin", "min_channel", true, "the smallest channel index (usually 0 or 1, default is 1)." );
		minChannelIdxOption.setRequired( false );

		final Option infolder = new Option( "i", "infolder", true, "folder to read data from" );
		infolder.setRequired( false );

		final Option outfolder =
				new Option( "o", "outfolder", true, "folder to write preprocessed data to (equals infolder if not given)" );
		outfolder.setRequired( false );

		final Option hasBrightNumbers =
				new Option( "bn", "bright_numbers", false, "use this option if the numbers below the GLs happen to be by far the brightest objects." );
		hasBrightNumbers.setRequired( false );

		final Option varianceThreshold =
				new Option("vt", "variance_threshold", true, "variance threshold help text missing!");
		varianceThreshold.setRequired(false);

		final Option lateralOffset =
				new Option("lo", "lateral_offset", true, "lateral offset help text missing!");
		lateralOffset.setRequired(false);

		final Option cropWidth =
				new Option("cw", "crop_width", true, "crop width help text missing!");
		cropWidth.setRequired(false);


		options.addOption( help );
		options.addOption( numChannelsOption );
		options.addOption( minChannelIdxOption );
		options.addOption( timeFirst );
		options.addOption( timeLast );
		options.addOption( infolder );
		options.addOption( outfolder );
		options.addOption( hasBrightNumbers );
		options.addOption( varianceThreshold );
		options.addOption( lateralOffset );
		options.addOption( cropWidth );


		// get the commands parsed
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args );
		} catch ( final ParseException e1 ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"... -i [in-folder] -o [out-folder] -c <num-channels> -cmin [start-channel-ids] -tmin [idx] -tmax [idx]",
					"",
					options,
					"Error: " + e1.getMessage() );
			if (!running_as_Fiji_plugin) {
				System.exit(0);
			} else {
				return;
			}
		}

		if ( cmd.hasOption( "help" ) ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "... -i <in-folder> -o [out-folder] [-headless]", options );
			if (!running_as_Fiji_plugin) {
				System.exit(0);
			} else {
				return;
			}
		}

		inputFolder = null;
		File originalInputFolder = null;
		if ( cmd.hasOption( "i" ) ) {
			inputFolder = new File( cmd.getOptionValue( "i" ) );
			originalInputFolder = inputFolder;

			if (!inputFolder.isDirectory()) {
				inputFolder = convertToFolder(inputFolder);
			}

			if ( !inputFolder.isDirectory() ) {
				System.out.println( "Error: Input folder is not a directory!" );
				if (!running_as_Fiji_plugin) {
					System.exit(2);
				} else {
					return;
				}
			}
			if ( !inputFolder.canRead() ) {
				System.out.println( "Error: Input folder cannot be read!" );
				if (!running_as_Fiji_plugin) {
					System.exit(2);
				} else {
					return;
				}
			}
		}

		outputFolder = null;
		if ( !cmd.hasOption( "o" ) ) {
			if (originalInputFolder.isDirectory()) {
				outputFolder = new File(originalInputFolder.getAbsolutePath() + "/output/");
			} else {
				outputFolder = new File(originalInputFolder.getAbsolutePath() + "_output/");
			}
			OUTPUT_PATH = outputFolder.getAbsolutePath();
		} else {
			outputFolder = new File( cmd.getOptionValue( "o" ) );
			if (! outputFolder.exists()) {
				outputFolder.mkdirs();
			}
			if ( !outputFolder.isDirectory() ) {
				System.out.println( "Error: Output folder is not a directory!" );
				if (!running_as_Fiji_plugin) {
					System.exit(3);
				} else {
					return;
				}
			}
			if ( !outputFolder.canWrite() ) {
				System.out.println( "Error: Output folder cannot be written to!" );
				if (!running_as_Fiji_plugin) {
					System.exit(3);
				} else {
					return;
				}
			}

			OUTPUT_PATH = outputFolder.getAbsolutePath();
		}

		if ( cmd.hasOption( "cmin" ) ) {
			MIN_CHANNEL_IDX = Integer.parseInt( cmd.getOptionValue( "cmin" ) );
		}
		if ( cmd.hasOption( "c" ) ) {
			NUM_CHANNELS = Integer.parseInt( cmd.getOptionValue( "c" ) );
		}

		if ( cmd.hasOption( "tmin" ) ) {
			MIN_TIME = Integer.parseInt( cmd.getOptionValue( "tmin" ) );
		}
		if ( cmd.hasOption( "tmax" ) ) {
			MAX_TIME = Integer.parseInt( cmd.getOptionValue( "tmax" ) );
		}

		if ( cmd.hasOption( "bn" ) ) {
			INTENSITY_THRESHOLD = 0.15;
		} else {
			INTENSITY_THRESHOLD = 0.25;
		}

		if (cmd.hasOption("vt")) {
			VARIANCE_THRESHOLD = Double.parseDouble(cmd.getOptionValue("vt"));
		}
		if (cmd.hasOption("lo")) {
			LATERAL_OFFSET = Integer.parseInt(cmd.getOptionValue("lo"));
		}
		if (cmd.hasOption("cw")) {
			GL_CROP_WIDTH = Integer.parseInt(cmd.getOptionValue("cw"));
		}
	}


	private static File convertToFolder(File file) {
		// in case there is something wrong, return the file as it was. the main routine will check that and make a
		// proper error message
		if (!file.exists()) {
			System.out.print("file doesn't exist");
			return file;
		}
		if (!file.isFile()) {
			System.out.print("file isn't a file");
			return file;
		}

		String property = "java.io.tmpdir";
		String tempDir = System.getProperty(property);

		if (!tempDir.endsWith("/")) {
			tempDir = tempDir + "/";
		}

		//System.out.println("OS current temporary directory is " + tempDir);
		// System.out.println("input file: " + );
		// makeDir(tempDir);
		// System.exit(0);

		String dirname = "tmp";
		int counter = 0;
		File tempDirectory;
		while ((tempDirectory = new File(tempDir + dirname + counter)).exists()) {
			counter ++;
		}
		tempDirectory.mkdirs();

		String path = tempDirectory.getAbsolutePath();
		if (!path.endsWith("/")) {
			path = path + "/";
		}

		path = path + file.getName() + "/";
		new File(path).mkdirs();


		ImagePlus imp = IJ.openImage(file.getAbsolutePath());

		for (int t = 1; t <= imp.getNFrames(); t++ ) {
			//imp.setT(t);
			for (int c = 1; c <= imp.getNChannels(); c++ ) {
				//imp.setC(c);
				ImagePlus slice = new Duplicator().run(imp, c,c,1,1, t,t);

				String newLocation = path + file.getName() + "_t" + String.format("%04d", t)  + "_c" + String.format("%04d", c);
				System.out.println("Saving to" + newLocation);
				IJ.saveAsTiff(slice, newLocation);
				new File(newLocation).deleteOnExit();
			}
		}
		tempDirectory.deleteOnExit();
		return new File(path);
	}
}
