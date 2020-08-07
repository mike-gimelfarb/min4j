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
package opt.multivariate.unconstrained.order0.evol;

import java.util.Arrays;
import java.util.function.Function;

import opt.OptimizerSolution;
import opt.multivariate.GradientFreeOptimizer;
import utils.BlasMath;
import utils.Sequences;

/**
 * A particle swarm optimization algorithm with competition, using an optional
 * local ring topology.
 * 
 * 
 * REFERENCES:
 * 
 * [1] Cheng, Ran & Jin, Yaochu. (2014). A Competitive Swarm Optimizer for Large
 * Scale Optimization. IEEE transactions on cybernetics. 45.
 * 10.1109/TCYB.2014.2322602.
 * 
 * [2] Kennedy, James & Mendes, Rui. (2002). Population structure and particle
 * swarm performance. Proc IEEE Congr Evol Comput. 2. 1671 - 1676.
 * 10.1109/CEC.2002.1004493.
 */
public final class CsoAlgorithm extends GradientFreeOptimizer {

	private final class Particle {

		// local positional properties
		final double[] myPos;
		final double[] myVel;
		double[] myMean;
		double myFit;

		// references to neighbors in right topology
		Particle myLeft;
		Particle myRight;

		Particle(final double[] pos, final double[] vel) {
			myPos = pos;
			myVel = vel;
			myFit = myFunc.apply(myPos);
		}

		final void competeWith(final Particle other) {

			// find the loser
			final Particle loser;
			final Particle winner;
			if (myFit > other.myFit) {
				loser = this;
				winner = other;
			} else {
				loser = other;
				winner = this;
			}

			// update velocity and position of the loser: equations (6) and (7)
			for (int i = 0; i < myD; ++i) {

				// velocity update (6)
				final double r1 = RAND.nextDouble();
				final double r2 = RAND.nextDouble();
				final double r3 = RAND.nextDouble();
				loser.myVel[i] = r1 * loser.myVel[i] + r2 * (winner.myPos[i] - loser.myPos[i])
						+ myPhi * r3 * (loser.myMean[i] - loser.myPos[i]);

				// clip velocity
				final double range = myUpper[i] - myLower[i];
				final double maxv = 0.2 * range;
				loser.myVel[i] = Math.max(-maxv, Math.min(maxv, loser.myVel[i]));

				// position update: equation (7)
				loser.myPos[i] += loser.myVel[i];
			}

			// correct if out of box
			if (myCorrectInBox) {
				for (int i = 0; i < myD; ++i) {
					if (loser.myPos[i] < myLower[i]) {
						loser.myPos[i] = myLower[i];
					} else if (loser.myPos[i] > myUpper[i]) {
						loser.myPos[i] = myUpper[i];
					}
				}
			}

			// update the fitness of the loser
			loser.myFit = myFunc.apply(loser.myPos);
		}
	}

	// model parameters
	private final boolean myUseRingTopology, myCorrectInBox;
	private final double myPhi, mySigmaTol;
	private final int mySize, myMaxEvals;
	private final Particle[] mySwarm;

	// problem parameters
	private Function<? super double[], Double> myFunc;
	private int myD;
	private double[] myMean, myLower, myUpper;
	private Particle myBest, myWorst;
	private int myEvals = 0;

	/**
	 *
	 * @param swarmSize
	 * @param stdevTolerance
	 * @param tolerance
	 * @param phi
	 * @param maxEvaluations
	 * @param useRingTopology
	 * @param correctOutOfBounds
	 */
	public CsoAlgorithm(final double tolerance, final double stdevTolerance, final int swarmSize,
			final int maxEvaluations, final double phi, final boolean useRingTopology,
			final boolean correctOutOfBounds) {
		super(tolerance);
		mySigmaTol = stdevTolerance;
		mySize = ((swarmSize & 1) == 0) ? swarmSize : swarmSize + 1;
		mySwarm = new Particle[mySize];
		myPhi = phi;
		myMaxEvals = maxEvaluations;
		myUseRingTopology = useRingTopology;
		myCorrectInBox = correctOutOfBounds;
	}

	/**
	 *
	 * @param swarmSize
	 * @param stdevTolerance
	 * @param tolerance
	 * @param maxEvaluations
	 */
	public CsoAlgorithm(final double tolerance, final double stdevTolerance, final int swarmSize,
			final int maxEvaluations) {
		this(tolerance, stdevTolerance, swarmSize, maxEvaluations, getPhi(swarmSize), false, false);
	}

	private static double getPhi(final int m) {

		// this is equation (25)
		if (m <= 100) {
			return 0.0;
		}

		// this is equations (25) and (26): take midpoint between hi and lo
		final double phimin;
		final double phimax;
		if (m <= 200) {
			phimin = 0.0;
			phimax = 0.1;
		} else if (m <= 400) {
			phimin = 0.1;
			phimax = 0.2;
		} else if (m <= 600) {
			phimin = 0.1;
			phimax = 0.2;
		} else {
			phimin = 0.1;
			phimax = 0.3;
		}
		return 0.5 * (phimin + phimax);
	}

	@Override
	public final void initialize(final Function<? super double[], Double> func, final double[] guess) {
		final double[] lo = new double[guess.length];
		final double[] hi = new double[guess.length];
		for (int i = 0; i < guess.length; ++i) {
			lo[i] = guess[i] - 4.0;
			hi[i] = guess[i] + 4.0;
		}
		initialize(func, lo, hi);
	}

	@Override
	public final OptimizerSolution<double[], Double> optimize(final Function<? super double[], Double> func,
			final double[] guess) {
		final double[] lo = new double[guess.length];
		final double[] hi = new double[guess.length];
		for (int i = 0; i < guess.length; ++i) {
			lo[i] = guess[i] - 4.0;
			hi[i] = guess[i] + 4.0;
		}
		return optimize(func, lo, hi);
	}

	@Override
	public final void iterate() {

		// split m particles in the swarm into pairs:
		// shuffle the swarm and assign element i to m/2 + i
		Sequences.shuffle(RAND, 0, mySwarm.length - 1, mySwarm);

		// now go through each pairing and perform fitness selection
		final int halfm = mySize >>> 1;
		for (int i = 0; i < halfm; ++i) {
			final int j = i + halfm;
			mySwarm[i].competeWith(mySwarm[j]);
		}
		myEvals += halfm;

		// update means based on neighbors topology
		if (myUseRingTopology) {
			for (final Particle p : mySwarm) {
				for (int i = 0; i < myD; ++i) {
					p.myMean[i] = (p.myLeft.myPos[i] + p.myPos[i] + p.myRight.myPos[i]) / 3.0;
				}
			}
		} else {
			Arrays.fill(myMean, 0.0);
			for (final Particle p : mySwarm) {
				for (int i = 0; i < myD; ++i) {
					myMean[i] += p.myPos[i] / mySize;
				}
			}
		}

		// find the best and worst points
		myBest = mySwarm[0];
		myWorst = mySwarm[0];
		for (final Particle p : mySwarm) {
			if (p.myFit <= myBest.myFit) {
				myBest = p;
			}
			if (p.myFit >= myWorst.myFit) {
				myWorst = p;
			}
		}
	}

	/**
	 *
	 * @param func
	 * @param lb
	 * @param ub
	 */
	public final void initialize(final Function<? super double[], Double> func, final double[] lb, final double[] ub) {

		// initialize function
		myFunc = func;
		myD = lb.length;
		myLower = lb;
		myUpper = ub;
		myEvals = 0;

		// initialize swarm
		for (int i = 0; i < mySize; ++i) {
			final double[] x = new double[myD];
			final double[] v = new double[myD];
			for (int j = 0; j < myD; ++j) {

				// randomly initialize position within the search space
				final double r = RAND.nextDouble();
				x[j] = (myUpper[j] - myLower[j]) * r + myLower[j];

				// set velocity initially to zero to reduce the chance the
				// particle leaves the boundary in subsequent iterations
				v[j] = 0.0;
			}
			mySwarm[i] = new Particle(x, v);
		}
		myEvals += mySize;

		// initialize topology to ring or dense topology
		if (myUseRingTopology) {
			for (int i = 0; i < mySize; ++i) {
				final int il = i == 0 ? mySize - 1 : i - 1;
				final int ir = i == mySize - 1 ? 0 : i + 1;
				mySwarm[i].myLeft = mySwarm[il];
				mySwarm[i].myRight = mySwarm[ir];
				mySwarm[i].myMean = new double[myD];
				for (int j = 0; j < myD; ++j) {
					mySwarm[i].myMean[j] = (mySwarm[i].myLeft.myPos[j] + mySwarm[i].myPos[j]
							+ mySwarm[i].myRight.myPos[j]) / 3.0;
				}
			}
		} else {
			myMean = new double[myD];
			for (final Particle p : mySwarm) {
				for (int i = 0; i < myD; ++i) {
					myMean[i] += p.myPos[i] / mySize;
				}
				p.myMean = myMean;
			}
		}
	}

	/**
	 *
	 * @param func
	 * @param lb
	 * @param ub
	 * @return
	 */
	public final OptimizerSolution<double[], Double> optimize(final Function<? super double[], Double> func,
			final double[] lb, final double[] ub) {

		// initialize parameters
		initialize(func, lb, ub);

		// main iteration loop over generations
		boolean converged = false;
		while (myEvals < myMaxEvals) {

			// perform a single generation
			iterate();

			// converge when distance in fitness between best and worst points
			// is below the given tolerance
			final double distY = Math.abs(myBest.myFit - myWorst.myFit);
			final double avgY = 0.5 * (myBest.myFit + myWorst.myFit);
			if (distY <= myTol + RELEPS * Math.abs(avgY)) {

				// compute standard deviation of swarm radiuses
				final int D = lb.length;
				int count = 0;
				double mean = 0.0;
				double m2 = 0.0;
				for (final Particle pt : mySwarm) {
					final double x = BlasMath.denorm(D, pt.myPos);
					++count;
					final double delta = x - mean;
					mean += delta / count;
					final double delta2 = x - mean;
					m2 += delta * delta2;
				}

				// test convergence in standard deviation
				if (m2 <= (mySize - 1) * mySigmaTol * mySigmaTol) {
					converged = true;
					break;
				}
			}
		}
		return new OptimizerSolution<>(myBest == null ? null : myBest.myPos, myEvals, 0, myBest != null && converged);
	}
}
