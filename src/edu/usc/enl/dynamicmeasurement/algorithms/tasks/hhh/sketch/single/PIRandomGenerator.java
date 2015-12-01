package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 3/3/13
 * Time: 10:00 AM
 */
public class PIRandomGenerator {
    public static final int MOD = 2147483647;
    public static final int MOD32 = 0xFFFFFFFF;
    public static final int HL = 31;
    public static final int NTAB = 32;
    public static final int KK = 17;
    public static final int JJ = 10;
    public static final int R1 = 5;
    public static final int R2 = 3;

    int usenric; // which prng to use
    long floatidum;
    long intidum; // needed to keep track of where we are in the
    // nric random number generators
    long iy;
    /* global variables */
    long[] randbuffer = new long[KK];  /* history buffer */
    int r_p1, r_p2;          /* indexes into history buffer */
    int iset;

    PIRandomGenerator(long seed, int nric) {

        // Initialise the random number generators.  nric determines
        // which algorithm to use, 1 for Numerical Recipes in C,
        // 0 for the other one.

        iy = 0;
        usenric = nric;
        floatidum = -1;
        intidum = -1;
        iset = 0;
        // set a global variable to record which algorithm to use
        switch (nric) {
            case 2:
                randomRotationAInit(seed);
                break;
            case 1:
                if (seed > 0) {
                    // to initialise the NRiC PRNGs, call it with a negative value
                    // so make sure it gets a negative value!
                    floatidum = -(seed);
                    intidum = -(seed);
                } else {
                    floatidum = seed;
                    intidum = seed;
                }
                break;
            case 3:
//                srand48(seed);
                break;
        }

//        prng_float(result);
        next();
        // call the routines to actually initialise them
    }

    public static int hash31(long a, long b, long x) {

        long result;

        // return a hash of x using a and b mod (2^31 - 1)
// may need to do another mod afterwards, or dream high bits
// depending on d, number of bad guys
// 2^31 - 1 = 2147483647

        //  result = ((int64_t) a)*((int64_t) x)+((int64_t) b);
        result = (a * x) + b;
        result = ((result >> HL) + result) & MOD;

        return (int) result;
    }

    public static int hash32(long a, long b, long x) {

        long result;

        //  result = ((int64_t) a)*((int64_t) x)+((int64_t) b);
        result = (a * x) + b;
        result = ((result >> HL) + result) & MOD32;

        return (int) result;
    }

    public long next() {

        // returns a pseudo-random long integer.  Initialise the generator
        // before use!

        long response = 0;

        switch (usenric) {
            case 1:
//                response = (ran2());
                break;
            case 2:
                response = (ran3());
                break;
            case 3:
//                response = (lrand48());
                break;
        }
        return response;
    }

    void randomRotationAInit(long seed) {

        int i;

  /* put semi-random numbers into the buffer */
        for (i = 0; i < KK; i++) {
            randbuffer[i] = seed;
            seed = rotl(seed, 5) + 97;
        }

  /* initialize pointers to circular buffer */
        r_p1 = 0;
        r_p2 = JJ;

  /* randomize */
        for (i = 0; i < 300; i++) {
            ran3();
        }
//        scale = Math.pow(2, -Long.SIZE);
    }

    private long ran3() {

        long x;

  /* generate next random number */

        x = randbuffer[r_p1] = rotl(randbuffer[r_p2], R1)
                + rotl(randbuffer[r_p1], R2);
  /* rotate list pointers */
        if (--r_p1 < 0) r_p1 = KK - 1;
        if (--r_p2 < 0) r_p2 = KK - 1;
        return x;
    }

    long rotl(long x, long r) {
        return (x << r) | (x >> (Long.SIZE - r));
    }
}
