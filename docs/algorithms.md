---
# Algorithms
---

The current version supports the optimization algorithms list below.

1. line search methods:
	- Backtracking
	- Constant Step Size
	- Fletcher
	- Hager-Zhang
	- More-Thuente
	- Strong Wolfe Conditions
2. univariate problems:
	- derivative-free methods:
		- Brent (local version)
		- Brent (global version)
		- Calvin
		- Davies-Swann-Campey
		- Fibonacci Search
		- Gaussian Estimation-of-Distribution (Gaussian-EDA)
		- Golden Section Search
		- Modified Piyavskii
	- first-order methods:
		- Cubic Interpolation
		- Modified Secant
3. multivariate problems:
	- unconstrained and box-constrained problems:
		- derivative-free methods:
			- quadratic approximation methods:
				- BOBYQA
				- NEWUOA
				- UOBYQA
			- CMA-ES methods and variants:
				- Vanilla (CMA-ES)
				- Active (aCMA-ES)
				- Cholesky (cholesky CMA-ES)
				- Limited Memory (LM-CMA-ES)
				- Separable (sep-CMA-ES)
				- Restarts with Increasing Pop. (IPOP, NIPOP...)
				- Restarts with Two Pop. (BIPOP, NBIPOP...)
			- direct search methods:
				- Controlled Random Search (CRS)
				- Dividing Rectangles (DIRECT)
				- Nelder-Mead Simplex
				- Praxis
				- Rosenbrock
			- evolutionary and swarm-based methods:
				- Adaptive Firefly
				- Adaptive PSO
				- Cooperatively Co-Evolving PSO
				- Competitive Swarm Optimization (CSO)
				- AMaLGaM IDEA
				- Differential Search
				- ESCH
				- PIKAIA
				- Self-Adaptive Differential Evolution with Neighborhood Search (SaNSDE)
		- first and second-order methods:
			- Conjugate Gradient (CG+)
			- Conjugate Variable Metric (PLIC)
			- Limited-Memory BFGS (LBFGS-B)
			- Truncated Newton
			- Trust-Region Newton
	- constrained problems:
		- derivative-free methods:
			- Box Complex
			- COBYLA
			- LINCOA
		- first and second-order methods:
			- Shor (SOLVOPT)
			- SQP Variable Metric (PSQP)
			- TOLMIN
	- other problems with specific structure:
		- linear programming problems:
			- Revised Simplex
		- least-squares problems:
			- Levenberg-Marquardt