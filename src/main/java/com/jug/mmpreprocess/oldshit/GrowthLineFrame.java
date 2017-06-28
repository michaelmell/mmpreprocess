/**
 *
 */
package com.jug.mmpreprocess.oldshit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 *         Represents one growth line (well) in which Bacteria can grow, at one
 *         instance in time.
 *         This corresponds to one growth line micrograph. The class
 *         representing an entire time
 *         series (2d+t) representation of an growth line is
 *         <code>GrowthLine</code>.
 */
public class GrowthLineFrame {

	// -------------------------------------------------------------------------------------
	// private fields
	// -------------------------------------------------------------------------------------
	/**
	 * Points at all the detected GrowthLine centers associated with this
	 * GrowthLine.
	 */
	private List< Point > imgLocations;

	// -------------------------------------------------------------------------------------
	// setters and getters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the location
	 */
	public List< Point > getImgLocations() {
		return imgLocations;
	}

	/**
	 * @param locations
	 *            the location to set
	 */
	public void setImgLocations( final List< Point > locations ) {
		this.imgLocations = locations;
	}

	/**
	 * @return the x-offset of the GrowthLineFrame given the original micrograph
	 */
	public long getOffsetX() {
		return getAvgXpos();
		//		return getPoint( 0 ).getLongPosition( 0 );
	}

	/**
	 * @return the y-offset of the GrowthLineFrame given the original micrograph
	 */
	public long getOffsetY() {
		return 0;
	}

	// -------------------------------------------------------------------------------------
	// constructors
	// -------------------------------------------------------------------------------------
	public GrowthLineFrame() {
		imgLocations = new ArrayList<>();
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * @return the number of points (the length) of this GrowthLine
	 */
	public int size() {
		return imgLocations.size();
	}

	/**
	 * Adds a detected center point to a GrowthsLineFrame.
	 *
	 * @param point
	 */
	public void addPoint( final Point point ) {
		imgLocations.add( point );
	}

	/**
	 *
	 */
	public void sortPoints() {
		Collections.sort( imgLocations, new Comparator< Point >() {

			@Override
			public int compare( final Point o1, final Point o2 ) {
				return new Integer( o1.getIntPosition( 1 ) ).compareTo( new Integer( o2.getIntPosition( 1 ) ) );
			}
		} );
	}

	/**
	 * Gets a detected center point of a GrowthsLine.
	 *
	 * @param idx
	 *            - index of the Point to be returned.
	 */
	public Point getPoint( final int idx ) {
		return ( imgLocations.get( idx ) );
	}

	/**
	 * Gets the first detected center point of a GrowthsLine.
	 */
	public Point getFirstPoint() {
		return ( imgLocations.get( 0 ) );
	}

	/**
	 * Gets the last detected center point of a GrowthsLine.
	 */
	public Point getLastPoint() {
		return ( imgLocations.get( imgLocations.size() - 1 ) );
	}

	/**
	 * @param img
	 * @return
	 */
	public double[] getCenterLineValues( final Img< DoubleType > img ) {
		final RandomAccess< DoubleType > raImg = img.randomAccess();

		final double[] dIntensity = new double[ imgLocations.size() ];
		for ( int i = 0; i < imgLocations.size(); i++ ) {
			raImg.setPosition( imgLocations.get( i ) );
			dIntensity[ i ] = raImg.get().get();
		}
		return dIntensity;
	}

	/**
	 * @return the average X coordinate of the center line of this
	 *         <code>GrowthLine</code>
	 */
	public int getAvgXpos() {
		int avg = 0;
		for ( final Point p : imgLocations ) {
			avg += p.getIntPosition( 0 );
		}
		if ( imgLocations.size() == 0 ) { return -1; }
		return avg / imgLocations.size();
	}

	/**
	 * @return the average X coordinate of the center line of this
	 *         <code>GrowthLine</code>
	 */
	public int getMedianXpos() {
		final Comparator<Point> comp = new Comparator< Point >(){

			@Override
			public int compare( final Point o1, final Point o2 ) {
				return o1.getIntPosition( 0 ) - o2.getIntPosition( 0 );
			}};
		Collections.sort( imgLocations, comp );
		return imgLocations.get( imgLocations.size() / 2 ).getIntPosition( 0 );
	}
}
