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
package opt.multivariate.unconstrained.leastsquares;

import opt.OptimizerSolution;

/**
 * 
 *
 */
public class LeastSquaresOptimizerSolution extends OptimizerSolution<double[], double[]> {

	/**
	 * 
	 * @param sol
	 * @param fevals
	 * @param dfevals
	 * @param converged
	 */
	public LeastSquaresOptimizerSolution(final double[] sol, final int fevals, final int dfevals,
			final boolean converged) {
		super(sol, fevals, dfevals, converged);
	}

	@Override
	public String toString() {
		String result = "";
		result += "x*: " + compactToString(mySol) + "\n";
		result += "calls to f: " + myFEvals + "\n";
		result += "calls to df/dx: " + myDEvals + "\n";
		result += "converged: " + myConverged;
		return result;
	}

	private static final String compactToString(final double[] arr) {
		String result = "[";
		for (final double x : arr) {
			result += String.format("%.6f", x) + " ";
		}
		result = result.trim() + "]";
		return result;
	}
}
