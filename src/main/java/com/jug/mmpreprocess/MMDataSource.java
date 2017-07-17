/**
 *
 */
package com.jug.mmpreprocess;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jug.mmpreprocess.util.FloatTypeImgLoader;


/**
 * @author jug
 */
public class MMDataSource {

	private final String[] extensions = { "tif", "tiff" };

	private final List< MMDataFrame > dataFrames = new ArrayList< MMDataFrame >();

	/**
	 * @param inputFolder
	 */
	public MMDataSource(
			final File inputFolder,
			final int minChannelIdx,
			final int minTime,
			int maxTime ) {
		sanityChecks( inputFolder );

		if ( maxTime < 0 ) {
			maxTime = Integer.MAX_VALUE;
		}

		final File[] fileArray =
				inputFolder.listFiles( /*new ExtensionFileFilter( extensions, ".tif and .tiff" )*/ MMUtils.tifFilter );
		final List< String > listOfImageFilesnames = new ArrayList< String >( fileArray.length );
		for ( final File file : fileArray ) {
			if ( !file.isDirectory() ) {
				if ( isInDataRange( file.getAbsolutePath(), minTime, maxTime ) ) {
					listOfImageFilesnames.add( file.getAbsolutePath() );
				}
			}
		}

		if ( fileArray.length == 0 ) {
			throw new IllegalArgumentException( "Valid files found for processing: " + listOfImageFilesnames
					.size() + " of " + fileArray.length + " (filtered by " + minTime + " < t < " + maxTime + ")" );
		} else {
			System.out.println(
					"Valid files found for processing: " + listOfImageFilesnames
							.size() + " of " + fileArray.length + " (filtered by " + minTime + " < t < " + maxTime + ")" );
		}

		Collections.sort( listOfImageFilesnames );
		int i = 0;
		List< String > srcFilenames = new ArrayList< String >( MMPreprocess.NUM_CHANNELS );
		for ( final String filename : listOfImageFilesnames ) {
//			System.out.println( filename );

			// collect
			if ( i % MMPreprocess.NUM_CHANNELS == 0 ) {
				srcFilenames.add( filename );
			} else {
				srcFilenames.add( filename );
			}
			// add + restart collecting
			if ( ( i + 1 ) % MMPreprocess.NUM_CHANNELS == 0 ) {
				dataFrames.add( new MMDataFrame( srcFilenames, minChannelIdx, inputFolder.getName() ) );
				srcFilenames = new ArrayList< String >( MMPreprocess.NUM_CHANNELS );
			}

			i++;
		}
	}

	/**
	 * @return
	 */
	private boolean isInDataRange( final String fn, final int minT, final int maxT ) {
		//final int start = fn.indexOf( "_t" ) + 2;
		//final String strT = fn.substring( start, start + 4 );
		try {
			final int t = FloatTypeImgLoader.getTimeFromFilename(fn); //Integer.parseInt( strT );
			if ( t >= minT && t <= maxT ) { return true; }
		} catch ( final NumberFormatException e ) {
			throw new IllegalArgumentException( String.format(
					"ERROR\tFile list corrupt. Time could not be extracted for file %s.", fn ) );
		}
		return false;
	}

	/**
	 * @param inputFolder
	 */
	public void sanityChecks( final File inputFolder ) {
		if ( inputFolder == null ) {
			throw new IllegalArgumentException( "ERROR\tGiven input folder is null." );
		}
		if ( !inputFolder.canRead() ) {
			throw new IllegalArgumentException( String.format(
				"ERROR\tGiven input folder can not be read - check permissions (%s).",
				inputFolder.getAbsolutePath() ) );
		}
		if ( !inputFolder.isDirectory() ) {
			throw new IllegalArgumentException( String.format(
				"ERROR\tGiven input folder is invalid (%s).",
				inputFolder.getAbsolutePath() ) );
		}
	}

	public MMDataFrame getFrame( final int i ) {
		return dataFrames.get( i );
	}

	/**
	 * @return
	 */
	public int size() {
		return dataFrames.size();
	}

}
