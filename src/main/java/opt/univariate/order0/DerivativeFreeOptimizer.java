/*
Copyright (c) 2020 Mike Gimelfarb

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the > "Software"), to
deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, > subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package opt.univariate.order0;

import java.util.function.Function;

import opt.univariate.UnivariateOptimizer;
import utils.Constants;

/**
 *
 */
public abstract class DerivativeFreeOptimizer extends UnivariateOptimizer {

	// ==========================================================================
	// CONSTRUCTORS
	// ==========================================================================
	/**
	 *
	 * @param absoluteTolerance
	 * @param relativeTolerance
	 * @param maxEvaluations
	 */
	public DerivativeFreeOptimizer(final double absoluteTolerance, final double relativeTolerance,
			final int maxEvaluations) {
		super(absoluteTolerance, relativeTolerance, maxEvaluations);
	}

	// ==========================================================================
	// ABSTRACT METHODS
	// ==========================================================================
	public abstract double optimize(Function<? super Double, Double> f, double a, double b);

	// ==========================================================================
	// IMPLEMENTATIONS
	// ==========================================================================
	@Override
	public Double optimize(final Function<? super Double, Double> f, final Double guess) {

		// first use guess to compute a bracket [a, b] that contains a min
		final int[] fev = new int[1];
		final double[] brackt = bracket(f, guess, Constants.GOLDEN, myMaxEvals, fev);
		myEvals += fev[0];
		if (brackt == null) {
			return Double.NaN;
		}
		final double a = brackt[0];
		final double b = brackt[1];

		// perform optimization using the bracketed routine
		return optimize(f, a, b);
	}
}