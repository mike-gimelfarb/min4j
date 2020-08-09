/* Copyright (c) 2007-2014 Massachusetts Institute of Technology
*
* Permission is hereby granted, free of charge, to any person obtaining
* a copy of this software and associated documentation files (the
* "Software"), to deal in the Software without restriction, including
* without limitation the rights to use, copy, modify, merge, publish,
* distribute, sublicense, and/or sell copies of the Software, and to
* permit persons to whom the Software is furnished to do so, subject to
* the following conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
* LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
* OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
* WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
*/
package opt.multivariate.unconstrained.order0.direct;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.function.Function;

import opt.multivariate.GradientFreeOptimizer;
import opt.multivariate.MultivariateOptimizerSolution;
import utils.BlasMath;

/**
 * A translation of the controlled random search (CRS) algorithm for the
 * minimization of a general non-linear function without constraints.
 * 
 * 
 * REFERENCES:
 * 
 * [1] Steven G. Johnson, The NLopt nonlinear-optimization package,
 * http://github.com/stevengj/nlopt
 */
public final class CrsAlgorithm extends GradientFreeOptimizer {

	private static final class RbNode implements Comparable<RbNode> {

		int i_ps;
		double fx;
		double[] x;

		@Override
		public int compareTo(final RbNode o) {
			final double k1 = fx;
			final double k2 = o.fx;
			if (k1 < k2) {
				return -1;
			} else if (k1 > k2) {
				return 1;
			} else {

				// tie-breaker
				return (int) (k1 - k2);
			}
		}
	}

	private static final class CrsData {

		int n;
		double[] lb, ub;
		Function<? super double[], Double> f;
		int evals;

		int npts;
		double[] psf;
		double[][] psx;
		double[] px;
		double pf;
		TreeSet<RbNode> t;
	}

	// algorithm parameters
	private final double myTolF;
	private final int myMaxEvals, myPopSize, myMaxMutations;

	// problem parameters
	private Function<? super double[], Double> myFunc;
	private double[] myLower, myUpper;
	private int n;

	// algorithm memory and changing variables
	private CrsData data;
	private double minF;
	private double[] x;
	private boolean done;

	/**
	 * 
	 * @param toleranceX
	 * @param toleranceF
	 * @param maxEvaluations
	 * @param populationSize
	 * @param numberMutations
	 */
	public CrsAlgorithm(final double toleranceX, final double toleranceF, final int maxEvaluations,
			final int populationSize, final int numberMutations) {
		super(toleranceX);
		myTolF = toleranceF;
		myMaxEvals = maxEvaluations;
		myPopSize = populationSize;
		myMaxMutations = numberMutations;
	}

	/**
	 * 
	 * @param toleranceX
	 * @param toleranceF
	 * @param maxEvaluations
	 */
	public CrsAlgorithm(final double toleranceX, final double toleranceF, final int maxEvaluations) {
		this(toleranceX, toleranceF, maxEvaluations, 0, 1);
	}

	@Override
	public final void initialize(final Function<? super double[], Double> func, final double[] guess) {
		final double[] lo = new double[guess.length];
		final double[] hi = new double[guess.length];
		for (int i = 0; i < guess.length; ++i) {
			lo[i] = guess[i] - 4.0;
			hi[i] = guess[i] + 4.0;
		}
		initialize(func, lo, hi, guess);
	}

	@Override
	public MultivariateOptimizerSolution optimize(final Function<? super double[], Double> func, final double[] guess) {
		final double[] lo = new double[guess.length];
		final double[] hi = new double[guess.length];
		for (int i = 0; i < guess.length; ++i) {
			lo[i] = guess[i] - 4.0;
			hi[i] = guess[i] + 4.0;
		}
		return optimize(func, lo, hi, guess);
	}

	@Override
	public final void iterate() {
		crs_trial(data, myMaxEvals, myMaxMutations);
		final RbNode best = data.t.first();
		if (best.fx < minF) {
			if (Math.abs(best.fx - minF) <= myTolF) {
				done = true;
			}
			double dx = 0.0;
			for (int i = 0; i < n; ++i) {
				final double dxi = best.x[i] - x[i];
				dx += dxi * dxi;
			}
			if (dx <= myTol) {
				done = true;
			}
			minF = best.fx;
			System.arraycopy(best.x, 0, x, 0, n);
		}
		if (data.evals >= myMaxEvals) {
			done = true;
		}
	}

	/**
	 * 
	 * @param func
	 * @param lb
	 * @param ub
	 * @param guess
	 */
	public void initialize(final Function<? super double[], Double> func, final double[] lb, final double[] ub,
			final double[] guess) {
		myFunc = func;
		myLower = lb;
		myUpper = ub;
		n = guess.length;

		// initialize
		done = false;
		x = Arrays.copyOf(guess, n);
		minF = Double.POSITIVE_INFINITY;
		data = new CrsData();
		crs_init(data, n, x, myLower, myUpper, myFunc, myPopSize);

		// set best element to current guess
		final RbNode best = data.t.first();
		minF = best.fx;
		System.arraycopy(best.x, 0, x, 0, n);
	}

	/**
	 * 
	 * @param func
	 * @param lb
	 * @param ub
	 * @param guess
	 */
	public MultivariateOptimizerSolution optimize(final Function<? super double[], Double> func, final double[] lb,
			final double[] ub, final double[] guess) {
		initialize(func, lb, ub, guess);
		while (!done) {
			iterate();
		}
		// TODO: check convergence
		return new MultivariateOptimizerSolution(Arrays.copyOf(x, n), data.evals, 0, false);
	}

	private static void random_trial(final CrsData d, final RbNode best) {
		final int n = d.n;

		// initialize x to x_0 = best point
		System.arraycopy(best.x, 0, d.px, 0, n);
		final int i0 = best.i_ps;

		// which of remaining n points is "x_n", i.e. which to reflect through ...
		// this is necessary since we generate the remaining points in order, so
		// just picking the last point would not be very random
		int jn = RAND.nextInt(n);

		/*
		 * use "method A" from
		 * 
		 * Jeffrey Scott Vitter, "An efficient algorithm for sequential random
		 * sampling," ACM Trans. Math. Soft. 13 (1), 58--67 (1987). to randomly pick n
		 * distinct points out of the remaining N-1 (not including i0!). (The same as
		 * "method S" in Knuth vol. 2.) This method requires O(N) time, which is fine in
		 * our case (there are better methods if n << N).
		 */
		{
			int nptsleft = d.npts - 1;
			int nleft = n;
			int nptsfree = nptsleft - nleft;
			int i = 0;
			if (i == i0) {
				++i;
			}
			while (nleft > 1) {
				double q = ((double) nptsfree) / nptsleft;
				final double v = RAND.nextDouble();
				while (q > v) {
					++i;
					if (i == i0) {
						++i;
					}
					--nptsfree;
					--nptsleft;
					q = (q * nptsfree) / nptsleft;
				}
				if (jn-- == 0) {

					// point to reflect through
					BlasMath.daxpym(n, -0.5 * n, d.psx[i], 1, d.px, 1);
				} else {

					// point to include in centroid
					BlasMath.dxpym(n, d.psx[i], 1, d.px, 1);
				}
				++i;
				if (i == i0) {
					++i;
				}
				--nptsleft;
				--nleft;
			}
			i += RAND.nextInt(nptsleft);
			if (i == i0) {
				++i;
			}
			if (jn-- == 0) {

				// point to reflect through
				BlasMath.daxpym(n, -0.5 * n, d.psx[i], 1, d.px, 1);
			} else {

				// point to include in centroid
				BlasMath.dxpym(n, d.psx[i], 1, d.px, 1);
			}
		}

		// re-normalize
		for (int k = 0; k < n; ++k) {
			d.px[k] *= 2.0 / n;
			if (d.px[k] > d.ub[k]) {
				d.px[k] = d.ub[k];
			} else if (d.px[k] < d.lb[k]) {
				d.px[k] = d.lb[k];
			}
		}
	}

	private static void crs_trial(final CrsData d, final int maxevls, final int numMutations) {
		final RbNode best = d.t.first();
		final RbNode worst = d.t.last();
		int mutation = numMutations;
		int n = d.n;
		random_trial(d, best);
		do {
			d.pf = d.f.apply(d.px);
			++d.evals;
			if (d.pf < worst.fx) {
				break;
			}
			if (d.evals >= maxevls) {
				return;
			}
			if (mutation != 0) {
				for (int i = 0; i < n; ++i) {
					final double w = RAND.nextDouble();
					d.px[i] = best.x[i] * (1.0 + w) - w * d.px[i];
					if (d.px[i] > d.ub[i]) {
						d.px[i] = d.ub[i];
					} else if (d.px[i] < d.lb[i]) {
						d.px[i] = d.lb[i];
					}
				}
				--mutation;
			} else {
				random_trial(d, best);
				mutation = numMutations;
			}
		} while (true);
		d.t.remove(worst);
		worst.fx = d.pf;
		System.arraycopy(d.px, 0, worst.x, 0, n);
		d.t.add(worst);
	}

	private static void crs_init(final CrsData d, final int n, final double[] x, final double[] lb, final double[] ub,
			final Function<? super double[], Double> f, final int pop) {
		if (pop == 0) {

			/*
			 * TODO: how should we set the default population size? the Kaelo and Ali paper
			 * suggests 10*(n+1), but should we add more random points if maxeval is large,
			 * or... ?
			 */
			// heuristic initial population size
			d.npts = 10 * (n + 1);
		} else {
			d.npts = pop;
		}

		// population must be big enough for a simplex
		if (d.npts < n + 1) {
			throw new IllegalArgumentException("Not enough points in population.");
		}

		d.n = n;
		d.f = f;
		d.evals = 0;
		d.ub = ub;
		d.lb = lb;
		d.psx = new double[d.npts][n];
		d.psf = new double[d.npts];
		d.px = new double[n];
		d.pf = 0.0;
		d.t = new TreeSet<>();

		// generate initial points randomly, plus starting guess
		System.arraycopy(x, 0, d.psx[0], 0, n);
		d.psf[0] = f.apply(x);
		++d.evals;
		final RbNode node = new RbNode();
		node.x = d.psx[0];
		node.fx = d.psf[0];
		node.i_ps = 0;
		d.t.add(node);
		for (int i = 1; i < d.npts; ++i) {
			final double[] k = d.psx[i];
			for (int j = 0; j < n; ++j) {
				k[j] = lb[j] + (ub[j] - lb[j]) * RAND.nextDouble();
			}
			d.psf[i] = f.apply(k);
			++d.evals;
			final RbNode node_i = new RbNode();
			node_i.x = k;
			node_i.fx = d.psf[i];
			node_i.i_ps = i;
			d.t.add(node_i);
		}
	}
}
