import java.util.BitSet;
import java.util.Random;

public class betterFrequencyEstimator {
    
    private final long PRIME_NUM = (1L << 31) - 1;
    private int availableSpace;
    private double pr1;
    private double epsilon;
    private double pr2;
    private int w;
    private int d;
    private int delta;
    private int m;
    private int k;
    private BitSet bloomFilter;
    private int[][] cmSketch;
    private int[] a;
    private int[] b;
    private int[] c1;
    private int[] c2;
    
    
    public betterFrequencyEstimator(int availableSpace, double pr1, double epsilon, double pr2) throws InsufficientMemoryException
    {
        this.epsilon = epsilon;
        this.pr1 = pr1;
        this.pr2 = pr2;
        this.availableSpace = availableSpace;
        this.delta = (int) Math.ceil(Math.log(1/(1-this.pr2)));
        this.d = (int)Math.ceil(Math.log(1 / pr1));
        this.w = (int)Math.ceil(2.718 /epsilon);
        m = this.optimalNumOfBits(400000, pr1);
        k = this.findK(400000, m);
        this.setHashCoeff(k, d);
        bloomFilter  = new BitSet(m);
        cmSketch = new int[d][w];
        if((32+4*w*d + bloomFilter.size()/8 + 4*d*2  + 4*k*2 +c1.length*8*2 + a.length*2) > availableSpace)
        {
            throw new InsufficientMemoryException("Not enough memory!");
        }
    }
    public void addArrival(int key)
    {
        long ip = (key & 0x00000000ffffffffL);
        this.insertBloomFilter(ip);
        this.insertToCMSketch(ip);
    }
    public int getFreqEstimation(int key)
    {   
        long ip = (key & 0x00000000ffffffffL);
        int freq =0;
        boolean f;
        if(f = this.checkBloomFilter(ip))
        {
            freq = this.findFrequency(ip);
        }
        return freq;
    }
    
    private int findFrequency(long key)
    {
        int res =  Integer.MAX_VALUE;
        for(int i=0; i<d ;i++)
        {
            int num = this.computeHash(w, c1[i], c2[i], key);
            res = Math.min(res,cmSketch[i][num]);
        }
        return res;
    }
    
    private boolean checkBloomFilter(long key)
    {
        for(int i=0; i<k;i++)
        {
            int num = this.computeHash(m, a[i], b[i], key);
            if(!bloomFilter.get(num))
                return false;
        }
        return true;
    }
    
    private void insertBloomFilter(long ip)
    {
        int toInsert;
        for(int i =0;i<this.k;i++)
        {
            toInsert = this.computeHash(m, a[i], b[i], ip);
            bloomFilter.set(toInsert,true);
        }        
    }

    private void insertToCMSketch(long ip) 
    {
        int toInsert;
        for(int j=0;j<this.d;j++)
        {
            toInsert = this.computeHash(w, c1[j], c2[j], ip);  
            cmSketch[j][toInsert]++;
        }
    }
    
    public long transformIPsToInts(String address)
    {
        long result =0;
         String[] addrArray = address.split("\\.");
         for (int i=0;i<addrArray.length;i++) {
            int power = 3-i;
            result += ((Integer.parseInt(addrArray[i])%256 * Math.pow(256,power)));
         }
        return result; 
    }

    public void setHashCoeff(int k, int d)
    {
        Random r = new Random();
        r.setSeed(1000000);
        a = new int[k];
        b = new int[k];
        c1 = new int[d];
        c2 = new int[d];
        for(int i=0;i<k;i++)
        {   
            a[i] = r.nextInt((Integer.MAX_VALUE - 0));
            b[i] = r.nextInt((Integer.MAX_VALUE - 0));
        }
        for(int i=0;i<k;i++)
        {   
            c1[i] = r.nextInt((Integer.MAX_VALUE - 0));
            c2[i] = r.nextInt((Integer.MAX_VALUE - 0));
        }        
    }
    
    public int computeHash(int p, int a, int b, long x1)
    {
        long hash = a*x1+b;
        hash += hash >> 32;
        hash &= PRIME_NUM;
        hash = hash % p;
        return (int)hash;
        
    }
    
    public int getAvailableSpace() {
        return availableSpace;
    }

    public void setAvailableSpace(int availableSpace) {
        this.availableSpace = availableSpace;
    }

    public double getPr1() {
        return pr1;
    }

    public void setPr1(double pr1) {
        this.pr1 = pr1;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public double getPr2() {
        return pr2;
    }

    public void setPr2(double pr2) {
        this.pr2 = pr2;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public BitSet getBloomFilter() {
        return bloomFilter;
    }

    public void setBloomFilter(BitSet bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    public int[][] getCmSketch() {
        return cmSketch;
    }

    public void setCmSketch(int[][] cmSketch) {
        this.cmSketch = cmSketch;
    }
    
    public class InsufficientMemoryException extends Exception {
        public InsufficientMemoryException(String message) {
            super(message);
        }
    }
        
    private int findK(int n, int m) {
          return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }   
    
    private int optimalNumOfBits(long n, double p) {
        return (int) ((n*Math.log(p)) / Math.log(0.6185));
    }
}
