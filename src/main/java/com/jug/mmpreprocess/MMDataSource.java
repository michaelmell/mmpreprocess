/**
 *
 */
package com.jug.mmpreprocess;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import weka.gui.ExtensionFileFilter;


/**
 * @author jug
 */
public class MMDataSource {

	private final String[] extensions = { "tif", "tiff" };

	private final List< MMDataFrame > dataFrames = new ArrayList< MMDataFrame >();

	private final int numChannels;

	/**
	 * @param inputFolder
	 */
	public MMDataSource( final File inputFolder, final int numChannels, final int minChannelIdx ) {
		sanityChecks( inputFolder );

		this.numChannels = numChannels;

		final File[] fileArray =
				inputFolder.listFiles( new ExtensionFileFilter( extensions, ".tif and .tiff" ) );
		final List< String > listOfImageFilesnames = new ArrayList< String >( fileArray.length );
		for ( final File file : fileArray ) {
			if ( !file.isDirectory() ) {
				listOfImageFilesnames.add( file.getAbsolutePath() );
			}
		}

		System.out.println( "Valid files found for processing: " + listOfImageFilesnames.size() );

		Collections.sort( listOfImageFilesnames );
		int i = 0;
		List< String > srcFilenames = new ArrayList< String >( numChannels );
		for ( final String filename : listOfImageFilesnames ) {
//			System.out.println( filename );

			if ( i % numChannels == 0 ) {
				if ( i > 0 )
					dataFrames.add( new MMDataFrame( srcFilenames, numChannels, minChannelIdx, inputFolder.getName() ) );
				srcFilenames = new ArrayList< String >( numChannels );
				srcFilenames.add( filename );
			} else {
				srcFilenames.add( filename );
			}
			i++;
		}
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
