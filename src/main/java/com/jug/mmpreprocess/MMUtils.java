/**
 *
 */
package com.jug.mmpreprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.jug.mmpreprocess.oldshit.FindLocalMaxima;
import com.jug.mmpreprocess.oldshit.GrowthLineFrame;
import com.jug.mmpreprocess.oldshit.Loops;


/**
 * @author jug
 */
public class MMUtils {

	/**
	 * @param dataFrame
	 * @return
	 */
	public static double computeTiltAngle(
			final MMDataFrame dataFrame,
			final double intensityThreshold ) {
		return computeTiltAngle( dataFrame.getChannel( 0 ), intensityThreshold );
	}

	private static double computeTiltAngle(
			final RandomAccessibleInterval< FloatType > img2d,
			final double intensityThreshold ) {

		final SimpleRegression regression = new SimpleRegression();

		for ( long x = img2d.min( 0 ); x < img2d.max( 0 ); x++ ) {
			final IntervalView< FloatType > column = Views.hyperSlice( img2d, 0, x );

			final Cursor< FloatType > colCursor = column.cursor();
			while ( colCursor.hasNext() ) {
				colCursor.fwd();
				try {
					if ( colCursor.get().get() > intensityThreshold ) {
						regression.addData( x, -colCursor.getIntPosition( 0 ) );
						break;
					}
				} catch ( final Exception e ) {
					System.out.println( colCursor.getIntPosition( 0 ) );
					e.printStackTrace();
					System.exit( MMPreprocess.EXIT_STATUS_COULDNOTLOAD_AS_FLOATTYPE );
				}
			}
		}

		final double dCorrectedSlope = regression.getSlope();
		final double radSlant = Math.atan( dCorrectedSlope );

		return radSlant / Math.PI * 180;
	}

	/**
	 *
	 * @param in_channel_1
	 * @return
	 */
	public static List< GrowthLineFrame > getGrowthLineFrames(
			final RandomAccessibleInterval< FloatType > img,
			final int offsetLateral,
			final int offsetTop,
			final int offsetBottom ) {

		// ------ GAUSS -----------------------------

		final double[] sigmas = new double[ 2 ];
		sigmas[ 0 ] = 20;
		sigmas[ 1 ] = 0;

		final long dims[] = new long[ img.numDimensions() ];
		img.dimensions( dims );
		final RandomAccessibleInterval< FloatType > smooth =
				new ArrayImgFactory< FloatType >().create( dims, new FloatType() );

		try {
			Gauss3.gauss( sigmas, Views.extendZero( img ), smooth );
		} catch ( final IncompatibleTypeException e ) {
			e.printStackTrace();
		}

//		ImageJFunctions.show( smooth, "DEBUG-getGrowthLineFrames" );

		// ------ FIND AND FILTER MAXIMA -------------

		// Find maxima per image row (per frame)
		final List< List< Point >> frameWellCenters =
				new Loops< FloatType, List< Point >>().forEachHyperslice(
						smooth,
						1,
						new FindLocalMaxima< FloatType >() );

		// Delete detected points that are too lateral
		for ( int y = 0; y < frameWellCenters.size(); y++ ) {
			final List< Point > lstPoints = frameWellCenters.get( y );
			for ( int x = lstPoints.size() - 1; x >= 0; x-- ) {
				if ( lstPoints.get( x ).getIntPosition( 0 ) < offsetLateral || lstPoints.get( x ).getIntPosition(
						0 ) > img.dimension( 0 ) - offsetLateral ) {
					lstPoints.remove( x );
				}
			}
			frameWellCenters.set( y, lstPoints );
		}

		// Delete detected points that are too high or too low
		// (and use this sweep to compute 'maxWellCenterIdx' and
		// 'maxWellCenters')
		for ( int y = 0; y < frameWellCenters.size(); y++ ) {
			if ( y < offsetTop || y >= smooth.dimension( 1 ) - offsetBottom ) {
				frameWellCenters.get( y ).clear();
			}
		}

		// Median stuff
		final int[] numWellCenters = new int[ frameWellCenters.size() ];
		final int[] sortedNumWellCenters = new int[ frameWellCenters.size() ];
		for ( int y = 0; y < frameWellCenters.size(); y++ ) {
			numWellCenters[ y ] = frameWellCenters.get( y ).size();
			sortedNumWellCenters[ y ] = frameWellCenters.get( y ).size();
		}
		Arrays.sort( sortedNumWellCenters );
		final int medianWellCenters = sortedNumWellCenters[ sortedNumWellCenters.length / 2 ];

		// remove rows with faulty detections (all > median#)
		int helper_numRowsOfMedianLength = 0;
		for ( int y = 0; y < frameWellCenters.size(); y++ ) {
			if ( frameWellCenters.get( y ).size() == medianWellCenters ) {
				helper_numRowsOfMedianLength++;
			}
			if ( frameWellCenters.get( y ).size() > medianWellCenters ) {
				frameWellCenters.get( y ).clear();
			}
		}

		int medianWellCentersIdx = -1;
		helper_numRowsOfMedianLength /= 2;
		for ( int y = 0; y < frameWellCenters.size(); y++ ) {
			if ( numWellCenters[ y ] == medianWellCenters ) {
				helper_numRowsOfMedianLength--;
				if ( helper_numRowsOfMedianLength == 0 ) {
					medianWellCentersIdx = y;
					break;
				}
			}
		}
		if ( medianWellCentersIdx == -1 ) { throw new RuntimeException( "ERROR\tCritical error occured while looking for GLs. Call for help!" ); }

//		int maxWellCenters = 0;
//		int maxWellCentersIdx = 0;
//		for ( int y = 0; y < frameWellCenters.size(); y++ ) {
//			if ( y < offsetTop || y > frameWellCenters.size() - 1 - offsetBottom ) {
//				frameWellCenters.get( y ).clear();
//			} else {
//				if ( maxWellCenters < frameWellCenters.get( y ).size() ) {
//					maxWellCenters = frameWellCenters.get( y ).size();
//					maxWellCentersIdx = y;
//				}
//			}
//			// System.out.println("Max well center number (" + maxWellCenters + ") found at row " + y);
//		}

		// ------ DISTRIBUTE POINTS TO CORRESPONDING GROWTH LINES -------

		final List< GrowthLineFrame > growthLines = new ArrayList< GrowthLineFrame >();

		final Point pOrig = new Point( 3 );
		pOrig.setPosition( 0, 2 ); // 1st channel

		// start at the row containing the maximum number of well centers
		// (see above for the code that found maxWellCenter*)
		pOrig.setPosition( medianWellCentersIdx, 1 );
		for ( int x = 0; x < medianWellCenters; x++ ) {
			growthLines.add( new GrowthLineFrame() ); // add one GLF for each
			// found column
			final Point p = frameWellCenters.get( medianWellCentersIdx ).get( x );
			pOrig.setPosition( p.getLongPosition( 0 ), 0 );
			growthLines.get( x ).addPoint( new Point( pOrig ) );
		}
		// now go backwards from 'maxWellCenterIdx' and find the right
		// assignment in case
		// a different number of wells was found (going forwards comes
		// below!)
		for ( int y = medianWellCentersIdx - 1; y >= 0; y-- ) {
			pOrig.setPosition( y, 1 ); // location in orig. Img (2nd of 3
			// steps)

			final List< Point > maximaPerImgRow = frameWellCenters.get( y );
			if ( maximaPerImgRow.size() == 0 ) {
				break;
			}
			// move points into detected wells
			for ( int x = 0; x < maximaPerImgRow.size(); x++ ) {
				final Point p = maximaPerImgRow.get( x );

				// find best matching well for this point
				final int posX = frameWellCenters.get( y ).get( x ).getIntPosition( 0 );
				int mindist = ( int ) img.dimension( 0 );
				int offset = 0;
				for ( int xx = 0; xx < medianWellCenters; xx++ ) {
					final int wellPosX = growthLines.get( xx ).getFirstPoint().getIntPosition( 0 );
					if ( mindist > Math.abs( wellPosX - posX ) ) {
						mindist = Math.abs( wellPosX - posX );
						offset = xx;
					}
				}
				pOrig.setPosition( p.getLongPosition( 0 ), 0 );
				growthLines.get( offset ).addPoint( new Point( pOrig ) );
			}
		}
		// now go forward from 'maxWellCenterIdx' and find the right
		// assignment in case
		// a different number of wells was found
		for ( int y = medianWellCentersIdx + 1; y < frameWellCenters.size(); y++ ) {
			pOrig.setPosition( y, 1 ); // location in original Img (2nd of 3
			// steps)

			final List< Point > maximaPerImgRow = frameWellCenters.get( y );
			if ( maximaPerImgRow.size() == 0 ) {
				break;
			}
			// move points into detected wells
			for ( int x = 0; x < maximaPerImgRow.size(); x++ ) {
				final Point p = maximaPerImgRow.get( x );

				// find best matching well for this point
				final int posX = frameWellCenters.get( y ).get( x ).getIntPosition( 0 );
				int mindist = ( int ) img.dimension( 0 );
				int offset = 0;
				for ( int xx = 0; xx < medianWellCenters; xx++ ) {
					final int wellPosX = growthLines.get( xx ).getFirstPoint().getIntPosition( 0 );
					if ( mindist > Math.abs( wellPosX - posX ) ) {
						mindist = Math.abs( wellPosX - posX );
						offset = xx;
					}
				}
				pOrig.setPosition( p.getLongPosition( 0 ), 0 );
				growthLines.get( offset ).addPoint( new Point( pOrig ) );
			}
		}

		// sort points
		for ( final GrowthLineFrame glf : growthLines ) {
			glf.sortPoints();
		}

		return growthLines;
	}
}
