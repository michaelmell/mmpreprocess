package com.jug.mmpreprocess.oldshit;


import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.AbstractOp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
public class RaiMeanSubtractor<T extends NumericType<T> & NativeType<T> > extends AbstractOp {
	
	@Parameter
	private RandomAccessibleInterval<T> input;

	@Parameter(type = ItemIO.OUTPUT)
	private RandomAccessibleInterval<T> output;

    public RandomAccessibleInterval<T> compute(
                    RandomAccessibleInterval<T> input,
                    RandomAccessibleInterval<T> output) {
            T mean = new MeanOfRai<T>().compute(input);
            DataMover.copy(input, output);

            for (T pixel : Views.iterable(output)) {
                    pixel.sub(mean);
            }
            return output;
    }

    public RandomAccessibleInterval<T> createEmptyOutput(
                    RandomAccessibleInterval<T> in) {
            return DataMover.createEmptyArrayImgLike(in, in.randomAccess().get()); 
    }

    @Override
    public void run() {
        output = compute( input, createEmptyOutput(input) );
    }


}
