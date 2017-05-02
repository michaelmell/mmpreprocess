/**
 *
 */
package com.jug.mmpreprocess;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.jug.mmpreprocess.oldshit.GrowthLineFrame;
import com.jug.mmpreprocess.oldshit.Loops;
import com.jug.mmpreprocess.oldshit.VarOfRai;
import com.jug.mmpreprocess.util.FloatTypeImgLoader;

import ij.IJ;
import io.scif.img.ImgIOException;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class MMDataFrame {

	private final int numChannels;
	private final int minChannelIdx;

	private int t;
	private final String basisName;

	private List< String > channelSourceFilenames = new ArrayList< String >();
	private List< RandomAccessibleInterval< FloatType > > channelImages = new ArrayList<RandomAccessibleInterval< FloatType > >();
	private List< CropArea > glCropAreas = null;

	public MMDataFrame(
			final List< String > srcFilenames,
			final int numChannels,
			final int minChannelIdx,
			final String prefix ) {
		channelSourceFilenames = srcFilenames;
		this.numChannels = numChannels;
		this.minChannelIdx = minChannelIdx;

		sanityChecks();

		//final int start = channelSourceFilenames.get( 0 ).indexOf( "_t" ) + 2;
		//final String strT = channelSourceFilenames.get( 0 ).substring( start, start + 4 );
		try {
			this.t = FloatTypeImgLoader.getTimeFromFilename(channelSourceFilenames.get( 0 )); //Integer.parseInt( strT );
		} catch ( final NumberFormatException e ) {
			throw new IllegalArgumentException( String.format(
					"ERROR\tFile list corrupt. Time could not be extracted for file %s.",
					channelSourceFilenames.get( 0 ) ) );
		}

		basisName = prefix;
	}

	/**
	 *
	 */
	private void sanityChecks() {
		int c=0;
		for ( final String filename : channelSourceFilenames ) {
			final String channelPattern = String.format( "_c%04d", c+minChannelIdx );
			if ( !filename.contains( channelPattern )) {
				throw new IllegalArgumentException( String.format(
					"ERROR\tFile list corrupt. Attempt to load file %s as channel %d.",
					filename,
					c + minChannelIdx ) );
			}
			c++;
		}
	}

	/**
	 * @param i
	 * @return
	 */
	public RandomAccessibleInterval< FloatType > getChannel( final int i ) {
		readImageDataIfNeeded();
		return channelImages.get( i );
	}

	/**
	 *
	 */
	public boolean readImageDataIfNeeded() {
		if ( channelImages.size() < numChannels ) {
			loadChannelImages();
			return true;
		}
		return false;
	}

	public void dropImageData() {
		channelImages = new ArrayList<RandomAccessibleInterval< FloatType > >();
	}

	/**
	 *
	 */
	public void loadChannelImages() {
		channelImages = new ArrayList<RandomAccessibleInterval< FloatType > >();

		// normalize first channel
		boolean normalize = true;

		for ( final String filename : channelSourceFilenames ) {
			RandomAccessibleInterval< FloatType > img = null;
			try {
				img = FloatTypeImgLoader.loadTiffEnsureFloatType( new File( filename ) );

				if ( normalize ) {
					Normalize.normalize(
							Views.iterable( img ),
							new FloatType( 0.0f ),
							new FloatType( 1.0f ) );
					normalize = false;
				}

			} catch ( final ImgIOException e ) {
				e.printStackTrace();
				System.exit( MMPreprocess.EXIT_STATUS_COULDNOTLOAD );
			}
			channelImages.add( img );
		}
	}

	/**
	 * @param angle
	 */
	public void rotate( final double angle, final int bottomPadding ) {
		final AffineTransform2D affine = getTransformFromAngle( angle );

		final CropArea ca = determineRotatedArea( angle, bottomPadding );

		for ( int i = 0; i < channelImages.size(); i++ ) {
			final RandomAccessibleInterval< FloatType > img = channelImages.get( i );

			final RandomAccessible<FloatType> ra = Views.extendValue(img, new FloatType(0.0f));
	        final RealRandomAccessible<FloatType> rra = Views.interpolate(ra, new NLinearInterpolatorFactory<FloatType>());
	        final RandomAccessible<FloatType> raRotated = RealViews.affine(rra, affine);

			final RandomAccessibleInterval< FloatType > raiRotatedAndCropped =
					Views.zeroMin( Views.interval(
							raRotated,
							new long[] { ca.left, ca.top },
							new long[] { ca.right, ca.bottom } ) );

			channelImages.remove( i );
			channelImages.add( i, raiRotatedAndCropped );
		}
	}

	/**
	 * Angle must be given in degrees and be within [-45,45]!
	 *
	 * @param angle
	 * @param img2d
	 * @return
	 */
	private AffineTransform2D getTransformFromAngle( final double angle ) {

		final double dCorrectedSlope = angle / 45.0;
		final double radSlant = Math.atan( dCorrectedSlope );
		final double[] dCenter2d =
				new double[] { channelImages.get( 0 ).dimension( 0 ) * 0.5,
							  channelImages.get( 0 ).dimension( 1 ) * 0.5 };

		// ...and inversely rotate the whole stack in XY
		final AffineTransform2D affine = new AffineTransform2D();
		affine.translate( -dCenter2d[ 0 ], -dCenter2d[ 1 ] );
		affine.rotate( radSlant );
		affine.translate( dCenter2d[ 0 ], dCenter2d[ 1 ] );

		return affine;
	}

	/**
	 * @param angle
	 * @return
	 */
	private CropArea determineRotatedArea( final double angle, final int bottomPadding ) {
		// crop positions to be evaluated
		final long top = 0, bottom = channelImages.get( 0 ).dimension( 1 );
		long left = 0, right = channelImages.get( 0 ).dimension( 0 );

		final double dCorrectedSlope = angle / 45.0;

		left = Math.round( Math.floor( 0 - dCorrectedSlope * bottom ) );
		right =
				Math.round( Math.ceil( channelImages.get( 0 ).dimension( 0 ) + dCorrectedSlope * ( channelImages.get(
						0 ).dimension( 1 ) - top ) ) );

		return new CropArea( top, left, bottom + bottomPadding, right );
	}

	/**
	 * @return
	 */
	public CropArea computeTightCropArea(
			final double varianceThreshold,
			final int minLength,
			final int topPadding,
			final int bottomPadding,
			final boolean debug ) {
		final long left = 0, right = channelImages.get( 0 ).dimension( 0 );

		final List< FloatType > points =
				new Loops< FloatType, FloatType >().forEachHyperslice(
						channelImages.get( 0 ),
						1,
						new VarOfRai< FloatType >() );
		final double[] y = new double[ points.size() ];
		int i = 0;
		for ( final FloatType dtPoint : points ) {
			y[ i ] = dtPoint.get();
			i++;
		}

		if ( debug ) {
			System.out.println( "\nVariances per pixel row are:\n" );
			for ( final double d : y ) {
				System.out.print( String.format( "%7.5f, ", d ) );
			}
			System.out.println( "" );
		}

		// looking for first interval above threshold longer then given value
		final List< Pair< Integer, Integer >> intervals =
				new ArrayList< Pair< Integer, Integer >>();
		int curLen = 0;
		int candidateStartIndex = 0;
		boolean wasBelow = true;
		for ( int j = 0; j < y.length; j++ ) {
			// case: switch to above threshold
			if ( wasBelow && y[ j ] > varianceThreshold ) {
				candidateStartIndex = j;
				wasBelow = false;
				curLen = 1;
			}
			// case: still above
			if ( !wasBelow && y[ j ] > varianceThreshold ) {
				curLen++;
			}
			// case: switch to below threshold
			if ( !wasBelow && ( y[ j ] <= varianceThreshold || j == y.length - 1 ) ) {
				wasBelow = true;
				intervals.add( new ValuePair< Integer, Integer >( candidateStartIndex, j ) );
				if ( curLen > minLength ) { return new CropArea( Math.max(
						0,
						candidateStartIndex - topPadding ), left, Math.min( j + bottomPadding,
						channelImages.get( 0 ).dimension( 1 ) ), right ); }
				curLen = 0;
			}
		}
		System.out.println( "WARNING\tNo crop area of sufficient height was found. Entire frames will be used -- this is likely to fail later on!" );
		return new CropArea( 0, left, channelImages.get( 0 ).dimension( 1 ), right );
	}

	/**
	 * @param tightCropArea
	 */
	public void crop( final CropArea ca ) {
		for ( int i = 0; i < channelImages.size(); i++ ) {
			final RandomAccessibleInterval< FloatType > raiRotatedAndCropped =
					Views.zeroMin( Views.interval(
							channelImages.get( i ),
							new long[] { ca.left, ca.top },
							new long[] { ca.right, ca.bottom } ) );

			channelImages.remove( i );
			channelImages.add( i, raiRotatedAndCropped );
		}
	}

	/**
	 * @param cropWidth
	 * @param lateralOffset
	 * @return
	 */
	public List< CropArea > computeGrowthLaneCropAreas( final int lateralOffset, final int cropWidth, final double sigmaX, final double sigmaY ) {

		final List< CropArea > ret = new ArrayList< CropArea >();

		final List< GrowthLineFrame > growthLines =
				MMUtils.getGrowthLineFrames( channelImages.get( 0 ), lateralOffset, 30, 30, sigmaX, sigmaY );

		System.out.println( "Number of GLs found: " + growthLines.size() + "\n" );

		int i = 0;
		for ( final GrowthLineFrame gl : growthLines ) {
			final long left = gl.getMedianXpos() - ( cropWidth / 2 );
			final long right = gl.getMedianXpos() + ( cropWidth / 2 );
			final long top = channelImages.get( 0 ).min( 1 );
			final long bottom = channelImages.get( 0 ).max( 1 );

			final CropArea ca = new CropArea( top, left, bottom, right );
			ret.add( ca );

			i++;
		}

		return ret;
	}

	/**
	 * @param glCropAreas
	 */
	public void setGLCropAreas( final List< CropArea > glCropAreas ) {
		this.glCropAreas = glCropAreas;
	}

	/**
	 * @param outputFolder
	 */
	public void saveGLCropsTo( final File outputFolder ) {
		saveGLCropsTo( outputFolder, false );
	}

	/**
	 * @param outputFolder
	 * @param isTestSet
	 */
	public void saveGLCropsTo( final File outputFolder, final boolean isTestSet ) {
		if ( glCropAreas == null ) { throw new RuntimeException( "ERROR\tMethod saveGLCropsTo was called before GL crop areas have been set. Call setGLCropAreas first!" ); }
		if ( !outputFolder.exists() ) {
			if ( !outputFolder.mkdirs() ) { throw new RuntimeException( "ERROR\tUnable to create output directory " + outputFolder.getAbsolutePath() ); }
		}
		if ( !outputFolder.canWrite() ) { throw new RuntimeException( "ERROR\tOutput directory cannot be written to. --> " + outputFolder.getAbsolutePath() ); }
		if ( glCropAreas.size() == 0 ) {
			System.out.println( "WARNING\tRequest to save GL crops with 0 detected GLs." );
		}

		System.out.print( "\nWriting GL crops into " + outputFolder.getAbsolutePath() );

		final PrintStream original = System.out;
		System.setOut( new PrintStream( new OutputStream() {

			@Override
			public void write( final int b ) {
				//DO NOTHING
			}
		} ) );
		System.out.println( "this should go to /dev/null, but it doesn't because it's not supported on other platforms" );

		// ALARM!!!! ALARM!!!! ALARM!!!!   STDOUT IS TURNED OFF HERE!!!
		// ALARM!!!! ALARM!!!! ALARM!!!!   STDOUT IS TURNED OFF HERE!!!

		for ( int gl = 0; gl < glCropAreas.size(); gl++ ) {
			final CropArea ca = glCropAreas.get( gl );
			final long dims[] = new long[ 2 ];
			dims[ 0 ] = ca.right - ca.left;
			dims[ 1 ] = ca.bottom - ca.top;

			for ( int c = 0; c < numChannels; c++ ) {
				final IntervalView< FloatType > toSave = Views.interval(
						channelImages.get( c ),
						new long[] { ca.left, ca.top },
						new long[] { ca.right, ca.bottom } );

				String path = outputFolder.getAbsolutePath();
				if ( !isTestSet ) {
					path = String.format( "%s/%s_GL%02d",
									outputFolder.getAbsolutePath(),
							basisName, gl + 1 );
				}

//				System.err.println( path );
				new File( path ).mkdirs();

				String complete_fn;
				complete_fn =
						String.format( "%s/%s_GL%02d_t%04d_c%04d.tif",
								path, basisName, gl + 1, t, c + minChannelIdx );
				IJ.saveAsTiff(
						ImageJFunctions.wrap( toSave, "tmp" ), complete_fn );

			}
		}

		System.setOut( original );
		System.out.println( " ... done!" );
	}
}
