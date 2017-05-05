/**
 *
 */
package com.jug.mmpreprocess;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.jug.mmpreprocess.util.FloatTypeImgLoader;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.process.ImageStatistics;

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
	private static double SIGMA_X = 20.0;
	private static double SIGMA_Y = 0.0;

	private static File inputFolder;
	private static File outputFolder;

	// global parameters
	private static boolean DEBUG = false;             // may be modified in parseCommandLineArgs()
	private static boolean AUTO_ROTATE = true;        // may be modified in parseCommandLineArgs()
	private static double INTENSITY_THRESHOLD;        // set in parseCommandLineArgs()
	private static int BOTTOM_PADDING = 0;            // may be modified in parseCommandLineArgs()
	private static int TOP_PADDING = 25;              // may be modified in parseCommandLineArgs()
	private static int GL_MIN_LENGTH = 250;           // may be modified in parseCommandLineArgs()
	private static double VARIANCE_THRESHOLD = 0.001; // may be modified in parseCommandLineArgs()
	private static int LATERAL_OFFSET = 40;           // may be modified in parseCommandLineArgs()
	private static int GL_CROP_WIDTH = 100;           // may be modified in parseCommandLineArgs()
	private static boolean SEQUENCE_OUTPUT = false;   // may be modified in parseCommandLineArgs()
	private static boolean IS_FLUO_PREPROCESSING = false; // may be modified in parseCommandLineArgs()
	private static int FAKE_GL_WIDTH = -1;            // may be modified in parseCommandLineArgs() -- negative values will cause no fake-GL to be created!

	/**
	 * @param args
	 */
	public static void main( final String[] args ) {
		parseCommandLineArgs( args );

		System.out.println("maxtime: " + MAX_TIME);

		// assemble file-list to process
		final MMDataSource dataSource =
				new MMDataSource( inputFolder, NUM_CHANNELS, MIN_CHANNEL_IDX, MIN_TIME, MAX_TIME );

		double angle = 0;
		final MMDataFrame firstFrame = dataSource.getFrame( 0 );
		final MMDataFrame lastFrame = dataSource.getFrame( dataSource.size() - 1 );
		firstFrame.getChannel( 0 );
		lastFrame.getChannel( 0 );
		if ( AUTO_ROTATE ) {
			// compute tilt angles
			final double angle1 = MMUtils.computeTiltAngle( firstFrame, INTENSITY_THRESHOLD );
			final double angle2 = MMUtils.computeTiltAngle( lastFrame, INTENSITY_THRESHOLD );
			System.out.println( "\n" );
			System.out.println( "Angle for  1st frame: " + angle1 );
			System.out.println( "Angle for last frame: " + angle2 );
			System.out.println( "" );

			// safety net
			angle = ( angle1 + angle2 ) / 2;
			if ( Math.abs( angle1 - angle2 ) > 0.5 ) {
				System.out.println( "Angles are very different -- use only angle of 1st frame!" );
				angle = angle1;
			}
		}

		// rotate and compute crop ROI
		firstFrame.rotate( angle, BOTTOM_PADDING );
		final CropArea tightCropArea =
				firstFrame.computeTightCropArea(
						VARIANCE_THRESHOLD,
						GL_MIN_LENGTH,
						TOP_PADDING,
						BOTTOM_PADDING,
						DEBUG );

		//TODO average some to do stuff below, here I only take 1st frame
		firstFrame.crop( tightCropArea );

		// compute GL crop areas
		final List< CropArea > glCropAreas =
				firstFrame.computeGrowthLaneCropAreas( LATERAL_OFFSET, GL_CROP_WIDTH, SIGMA_X, SIGMA_Y );

		// debug
		if ( DEBUG ) {
			firstFrame.setGLCropAreas( glCropAreas );
			if ( FAKE_GL_WIDTH > 0 ) firstFrame.createFakeGLChannel( IS_FLUO_PREPROCESSING, FAKE_GL_WIDTH );
			firstFrame.saveGLCropsTo( outputFolder, true );
		}

		// crop GLs out of frames
		for ( int f = 0; f < dataSource.size(); f++ ) {
			final MMDataFrame frame = dataSource.getFrame( f );
			if ( f > 0 ) { // first one is already modified at this point (see above)
				frame.readImageDataIfNeeded();
				frame.rotate( angle, BOTTOM_PADDING );
				frame.crop( tightCropArea );

				frame.setGLCropAreas( glCropAreas );
				if ( FAKE_GL_WIDTH > 0 ) frame.createFakeGLChannel( IS_FLUO_PREPROCESSING, FAKE_GL_WIDTH );
				frame.saveGLCropsTo( outputFolder );
			}

			frame.dropImageData();
		}

		if (!SEQUENCE_OUTPUT) {
			convertImageSequenceFolderToStack(outputFolder);
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
		numChannelsOption.setRequired( false );

		final Option minChannelIdxOption =
				new Option( "cmin", "min_channel", true, "the smallest channel index (usually 0 or 1, default is 1)." );
		minChannelIdxOption.setRequired( false );

		final Option infolder = new Option( "i", "infolder", true, "folder to read data from" );
		infolder.setRequired( true );

		final Option outfolder =
				new Option( "o", "outfolder", true, "folder to write preprocessed data to (equals infolder if not given)" );
		outfolder.setRequired( false );

		final Option sigma =
				new Option( "s", "sigma", true, "sigma for smoothing prior to finding GLs (default value: 20.0)" );
		sigma.setRequired( false );

		final Option hasBrightNumbers =
				new Option( "bn", "bright_numbers", false, "use this option if the numbers below the GLs happen to be by far the brightest objects." );
		hasBrightNumbers.setRequired( false );

		final Option varianceThreshold =
				new Option( "vt", "variance_threshold", true, "used to identify the pixel rows containing growth channels (intesity variance per row)." );
		varianceThreshold.setRequired(false);

		final Option lateralOffset =
				new Option( "lo", "lateral_offset", true, "lateral offset in entire input images that will not be considered." );
		lateralOffset.setRequired(false);

		final Option cropWidth =
				new Option( "cw", "crop_width", true, "the widht of the cut images (centered on identified growth channel center lines)." );
		cropWidth.setRequired(false);

		final Option topPadding =
				new Option( "tp", "top_padding", true, "padding on top of final cut images (in pixels)." );
		cropWidth.setRequired( false );

		final Option bottomPadding =
				new Option( "bp", "bottom_padding", true, "padding at the bottom of final cut images (in pixels)." );
		cropWidth.setRequired( false );

		final Option sequenceOutput =
				new Option("so", "sequenceoutput", false, "use this option to output a sequence of .tif files as output.");

		final Option glMinLength =
				new Option( "gl_minl", "gl_min_length", true, "min length of the grwoth channels in this image (longer avoids erroneous cropping)." );

		final Option noAutoRotation =
				new Option( "norotation", "no_auto_rotation", false, "no automatically determined rotation will be performed." );

		final Option debugSwitch =
				new Option( "d", "debug", false, "print additional debug output." );

		final Option isFluo =
				new Option( "fluo", "is_fluo_preprocessing", false, "creates 'fake' phase contrast channel from fluor. channel 0." );
		final Option fakeGLWidth =
				new Option( "fglw", "fake_gl_width", true, "width of 'fake' GL to be made (in pixels)." );

		options.addOption( help );
		options.addOption( numChannelsOption );
		options.addOption( minChannelIdxOption );
		options.addOption( timeFirst );
		options.addOption( timeLast );
		options.addOption( infolder );
		options.addOption( outfolder );
		options.addOption( sigma );
		options.addOption( hasBrightNumbers );
		options.addOption( varianceThreshold );
		options.addOption( lateralOffset );
		options.addOption( cropWidth );
		options.addOption( topPadding );
		options.addOption( bottomPadding );
		options.addOption( sequenceOutput );
		options.addOption( glMinLength );
		options.addOption( noAutoRotation );
		options.addOption( debugSwitch );
		options.addOption( isFluo );
		options.addOption( fakeGLWidth );

		// get the commands parsed
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args );
		} catch ( final ParseException e1 ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"...",
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
			formatter.printHelp(
					"...",
					options );
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
				inputFolder = convertFileToTempFolder(inputFolder);
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

		if ( cmd.hasOption( "gl_minl" ) ) {
			GL_MIN_LENGTH = Integer.parseInt( cmd.getOptionValue( "gl_minl" ) );
		}

		// Determine min and max / num of channels by going through the input directory

		int min_c = Integer.MAX_VALUE;
		int max_c = Integer.MIN_VALUE;

		for (final File image : inputFolder.listFiles(MMUtils.tifFilter)) {
			final int c = FloatTypeImgLoader.getChannelFromFilename(image.getName());
			if (c < min_c) {
				min_c = c;
			}
			if (c > max_c) {
				max_c = c;
			}
		}
		//System.out.println("min_c: " + min_c);
		//System.out.println("max_c: " + max_c);
		MIN_CHANNEL_IDX = min_c;
		NUM_CHANNELS = max_c - min_c + 1;


		if ( cmd.hasOption( "tmin" ) ) {
			MIN_TIME = Integer.parseInt( cmd.getOptionValue( "tmin" ) );
		} else {
			int min_t = Integer.MAX_VALUE;
			for (final File image : inputFolder.listFiles(MMUtils.tifFilter)) {

				final int t = FloatTypeImgLoader.getTimeFromFilename(image.getName());
				if (t < min_t) {
					min_t = t;
				}
			}
			MIN_TIME = min_t;
		}

		if ( cmd.hasOption( "tmax" ) ) {
			MAX_TIME = Integer.parseInt( cmd.getOptionValue( "tmax" ) );
		} else {
			int max_t = Integer.MIN_VALUE;
			for (final File image : inputFolder.listFiles(MMUtils.tifFilter)) {

				final int t = FloatTypeImgLoader.getTimeFromFilename(image.getName());
				if (t > max_t) {
					max_t = t;
				}
			}
			MAX_TIME = max_t;

		}

		if ( cmd.hasOption( "s" ) ) {
			SIGMA_X = Double.parseDouble( cmd.getOptionValue( "s" ) );
			SIGMA_Y = 0.0;
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
		if ( cmd.hasOption( "tp" ) ) {
			TOP_PADDING = Integer.parseInt( cmd.getOptionValue( "tp" ) );
		}
		if ( cmd.hasOption( "bp" ) ) {
			BOTTOM_PADDING = Integer.parseInt( cmd.getOptionValue( "bp" ) );
		}

		// Parameters for fake phase contrast channel generation
		IS_FLUO_PREPROCESSING = cmd.hasOption( "fluo" );

		if ( cmd.hasOption( "fake_gl_width" ) ) {
			FAKE_GL_WIDTH = Integer.parseInt( cmd.getOptionValue( "fglw" ) );
		}

		SEQUENCE_OUTPUT = cmd.hasOption("so");
		DEBUG = cmd.hasOption( "d" );
		AUTO_ROTATE = !cmd.hasOption( "norotation" );
	}

	private static void convertImageSequenceFolderToStack(final File file) {
		for (final File folder : file.listFiles(MMUtils.folderFilter)) {
			ImageStack stack = null;

			File firstFile = null;

			final File[] filelist = folder.listFiles(MMUtils.tifFilter);
			Arrays.sort(filelist);


			int minTime = 0;
			int maxTime = 0;

			for (final File image : filelist) {


				final ImagePlus imp = new ImagePlus(image.getAbsolutePath());
				if (imp.getNChannels() == 1 && imp.getNSlices() == 1) {
					final int timeFromFilename = FloatTypeImgLoader.getTimeFromFilename(image.getName());
					if (firstFile == null || stack == null) {
						firstFile = image;
						stack = new ImageStack(imp.getWidth(), imp.getHeight());

						minTime = timeFromFilename;
						maxTime = timeFromFilename;
					}
					if (minTime > timeFromFilename) {
						minTime = timeFromFilename;
					}
					if (maxTime < timeFromFilename) {
						maxTime = timeFromFilename;
					}
					System.out.println("packing " + image.getName() + " " + imp.getWidth() + "/" + imp.getHeight() + "/" + imp.getNChannels() + "/" + imp.getNSlices() + "/" + imp.getNFrames());
					stack.addSlice(imp.getProcessor());
				}
			}
			if ( stack == null ) {
				break;
			}
			final ImagePlus impStack = new ImagePlus( file.getName(), stack );
			final int numFrames = maxTime - minTime + 1;
			final int numSlices = impStack.getNSlices() / NUM_CHANNELS / numFrames;

			System.out.println( "convert from stack with " + impStack.getNSlices() );

			System.out.println( "convert to hyperstack with " + NUM_CHANNELS + " channels" );
			System.out.println( "convert to hyperstack with " + numSlices + " slices" );
			System.out.println( "convert to hyperstack with " + numFrames + " frames" );

			final ImagePlus hyperStack = HyperStackConverter.toHyperStack( impStack, NUM_CHANNELS, numSlices, numFrames );

			String newFilename = firstFile.getName();
			newFilename = newFilename.replace( String.format( "_c%04d", MIN_CHANNEL_IDX ), "" );
			newFilename = newFilename.replace( String.format( "_t%04d", MIN_TIME ), "" );
			newFilename = folder.getAbsolutePath() + "/" + newFilename;

			System.out.println( "Save to: " + newFilename );

			for ( final File image : folder.listFiles( MMUtils.tifFilter ) ) {
				image.delete();
			}

			for ( int c = 1; c <= hyperStack.getNChannels(); c++ ) {
				hyperStack.setC( c );
				final ImageStatistics stats = hyperStack.getStatistics( ImageStatistics.MIN_MAX );
				hyperStack.setDisplayRange( stats.min, stats.max );
			}
			IJ.saveAsTiff( hyperStack, newFilename );
		}
	}

	private static File convertFileToTempFolder(final File file) {
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

		final String property = "java.io.tmpdir";
		String tempDir = System.getProperty(property);

		if (!tempDir.endsWith("/")) {
			tempDir = tempDir + "/";
		}

		//System.out.println("OS current temporary directory is " + tempDir);
		// System.out.println("input file: " + );
		// makeDir(tempDir);
		// System.exit(0);

		final String dirname = "tmp";
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


		final ImagePlus imp = IJ.openImage(file.getAbsolutePath());

		for (int t = 1; t <= imp.getNFrames(); t++ ) {
			//imp.setT(t);
			for (int c = 1; c <= imp.getNChannels(); c++ ) {
				//imp.setC(c);
				final ImagePlus slice = new Duplicator().run(imp, c,c,1,1, t,t);

				final String newLocation = path + file.getName() + "_t" + String.format("%04d", t)  + "_c" + String.format("%04d", c) + ".tif";
				System.out.println("Saving to " + newLocation);
				IJ.saveAsTiff(slice, newLocation);
				new File(newLocation).deleteOnExit();
			}
		}
		tempDirectory.deleteOnExit();
		return new File(path);
	}
}
