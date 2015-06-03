package com.jug.mmpreprocess.oldshit;

import net.imglib2.ops.operation.UnaryOperation;

/**
 * Class should only be used if the output can't be known by the user in
 * advance. For example if the operation produces an double[] of unknown size,
 * this algorithm should know what to do.
 * <p>
 * If the output can be known, e.g. {@link net.imglib2.img.Img} to
 * {@link net.imglib2.img.Img} of same dimensionality {@link UnaryOperation}
 * should be used.
 * </p>
 * 
 * @author Christian Dietz (University of Konstanz)
 */
public interface OldUnaryOutputOperation< INPUT_TYPE, OUTPUT_TYPE > extends UnaryOperation< INPUT_TYPE, OUTPUT_TYPE >
{
	/**
	 * Creates an empty output for the given input.
	 * 
	 * @param in
	 *            Input to be processed
	 * @return Output object, which can be used to store the result
	 */
	OUTPUT_TYPE createEmptyOutput( INPUT_TYPE in );

	/**
	 * 
	 * @param in
	 *            Input to be processed
	 * @return Output object storing the result
	 * 
	 */
	OUTPUT_TYPE compute( INPUT_TYPE in );

	/**
	 * {@inheritDoc}
	 */
	@Override
	OldUnaryOutputOperation< INPUT_TYPE, OUTPUT_TYPE > copy();
}
