/**
 *
 */
package com.jug.mmpreprocess;

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

	// things to come via command-line arguments
	private static boolean HEADLESS = false;
	private static String OUTPUT_PATH = "";
	private static int MIN_CHANNEL_IDX = 1;
	private static int NUM_CHANNELS = 2;
	private static int MIN_TIME = -1;
	private static int MAX_TIME = -1;

	private static File inputFolder;
	private static File outputFolder;

	// global parameters
	private static final double INTENSITY_THRESHOLD = 0.25;
	private static final int BOTTOM_PADDING = 25;
	private static final int TOP_PADDING = 25;
	private static final int GL_MIN_LENGTH = 250;
	private static final double VARIANCE_THRESHOLD = 0.001;
	private static final int LATERAL_OFFSET = 50;
	private static final int GL_CROP_WIDTH = 100;

	/**
	 * @param args
	 */
	public static void main( final String[] args ) {
		parseCommandLine( args );

		// assemble file-list to process
		final MMDataSource dataSource =
				new MMDataSource( inputFolder, NUM_CHANNELS, MIN_CHANNEL_IDX );

		// compute tilt angle
		final MMDataFrame firstFrame = dataSource.getFrame( 0 );
		final MMDataFrame lastFrame = dataSource.getFrame( dataSource.size() - 1 );
		final double angle1 = MMUtils.computeTiltAngle( firstFrame, INTENSITY_THRESHOLD );
		final double angle2 = MMUtils.computeTiltAngle( lastFrame, INTENSITY_THRESHOLD );
		System.out.println( "\n" );
		System.out.println( "Angle for 1st frame: " + angle1 );
		System.out.println( "Angle for 2nd frame: " + angle2 );
		System.out.println( "" );
		final double angle = ( angle1 + angle2 ) / 2;

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
			if ( frame.readImageDataIfNeeded() ) {
				frame.rotate( angle, BOTTOM_PADDING );
				frame.crop( tightCropArea );
			}

			frame.setGLCropAreas( glCropAreas );
			frame.saveGLCropsTo( outputFolder );

			frame.dropImageData();
		}

//		new ImageJ();
//		ImageJFunctions.show( firstFrame.getChannel( 0 ), "DEBUG" );
		System.exit( 0 );
	}

	/**
	 * @param args
	 */
	private static void parseCommandLine( final String[] args ) {

		// create Options object & the parser
		final Options options = new Options();
		final CommandLineParser parser = new BasicParser();
		// defining command line options
		final Option help = new Option( "help", "print this message" );

		final Option headless =
				new Option( "h", "headless", false, "start without user interface (note: input-folder must be given!)" );
		headless.setRequired( false );

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

		options.addOption( help );
		options.addOption( headless );
		options.addOption( numChannelsOption );
		options.addOption( minChannelIdxOption );
		options.addOption( timeFirst );
		options.addOption( timeLast );
		options.addOption( infolder );
		options.addOption( outfolder );
		// get the commands parsed
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args );
		} catch ( final ParseException e1 ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"... -p [props-file] -i [in-folder] -o [out-folder] -c <num-channels> -cmin [start-channel-ids] -tmin [idx] -tmax [idx] [-headless]",
					"",
					options,
					"Error: " + e1.getMessage() );
			System.exit( 0 );
		}

		if ( cmd.hasOption( "help" ) ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "... -i <in-folder> -o [out-folder] [-headless]", options );
			System.exit( 0 );
		}

		if ( cmd.hasOption( "h" ) ) {
			System.out.println( ">>> Starting MM in headless mode." );
			HEADLESS = true;
			if ( !cmd.hasOption( "i" ) ) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "Headless-mode requires option '-i <in-folder>'...", options );
				System.exit( 0 );
			}
		}

		inputFolder = null;
		if ( cmd.hasOption( "i" ) ) {
			inputFolder = new File( cmd.getOptionValue( "i" ) );

			if ( !inputFolder.isDirectory() ) {
				System.out.println( "Error: Input folder is not a directory!" );
				System.exit( 2 );
			}
			if ( !inputFolder.canRead() ) {
				System.out.println( "Error: Input folder cannot be read!" );
				System.exit( 2 );
			}
		}

		outputFolder = null;
		if ( !cmd.hasOption( "o" ) ) {
			outputFolder = new File( inputFolder.getAbsolutePath() + "/output/" );
			OUTPUT_PATH = outputFolder.getAbsolutePath();
		} else {
			outputFolder = new File( cmd.getOptionValue( "o" ) );

			if ( !outputFolder.isDirectory() ) {
				System.out.println( "Error: Output folder is not a directory!" );
				System.exit( 3 );
			}
			if ( !outputFolder.canWrite() ) {
				System.out.println( "Error: Output folder cannot be written to!" );
				System.exit( 3 );
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
	}

}
