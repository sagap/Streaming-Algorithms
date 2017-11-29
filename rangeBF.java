import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

public class rangeBF {
    
    private final long PRIME_NUM = (1L << 31) - 1;
    private double pr;
    private int k;
    private int[] a;     // for hashing
    private int[] b;      // for hashing
    private int m;
    private int n;
    private int domainSize;                      // how many sketches
    private ArrayList<sketch> bloomFiltersArray;    

    public class sketch{
        private Integer levels;
        BitSet bloomF;                  // For each level
        private int length;           // how many elements each sketch contains e.g. for length 4 can contain elements:[4-7]
        private long range;      // describes total range
        
        public sketch(int size, int l){
            this.levels = l;                
            this.length = (int)Math.pow(2, l); // length of dyadic intervals on this level
            this.range = (long)Math.pow(2, 32-l);
            bloomF = new BitSet((int) (size / (length)));
        }
        
        public Integer getStart() {
            return levels;
        }

        public void setStart(Integer levels) {
            this.levels = levels;
        }

        public BitSet getBloomF() {
            return bloomF;
        }

        public void setBloomF(BitSet bloomF) {
            this.bloomF = bloomF;
        }

        public Integer getLevels() {
            return levels;
        }

        public void setLevels(Integer levels) {
            this.levels = levels;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public long getRange() {
            return range;
        }

        public void setRange(long range) {
            this.range = range;
        }
    }

    public rangeBF(double pr) {
        this.pr = pr;
    }

    public void createSketch(int size, int m)
    {
        this.domainSize = size;
        bloomFiltersArray = new ArrayList<>(size);
        for(int i=0;i<size;i++)
        {
            bloomFiltersArray.add(new sketch(m,i));       
        }
    }    

    public void insertValue(int key) 
    {
        long ip = (key & 0x00000000ffffffffL);
        long p;
        long temp_key = 0;
        for(int i= 0;i<domainSize;i++)            // for each level compute the hash and set true to the corresponding positions
        {
            p = this.bloomFiltersArray.get(i).range;
            for(int j =0; j<k; j++)
            {
                temp_key =  computeHash(p, a[j], b[j], ip);
                this.bloomFiltersArray.get(i).getBloomF().set((int) temp_key, true);
            }
            ip >>= 1;
        }
    }
    
    public boolean existsInRange(int l, int r)
    {
        long minA = (l & 0x00000000ffffffffL);
        long maxA = (r & 0x00000000ffffffffL);
        long start = minA;
        long end = maxA;
        if(minA > maxA)
            return false;
        boolean flag = false;                  // return true only if all k functions return true
        int to_return = 0;
        boolean start_alone = false;
        boolean end_closes = false;
        long posl;
        long posr;
        for(int i =0;i<this.domainSize;i++)   // start from the bottom
        {
            if(start >= end)
                return false;
            if(i==0)
            {    
                for(int j=0;j<this.k;j++)
                {
                    long p = this.bloomFiltersArray.get(i).range;
                    posl =  computeHash(p, a[j], b[j], minA);
                    int b = findIfExists(i,(int)posl);
                    if(b ==1)
                        to_return++;
                }
                if(to_return == k)
                    return true;
            }
            else
            {
                int b = this.findIfExists(start, end, i);
                if(b == 2)
                    return false;
                else if(b == 1)
                    return true;
            }
            if(i == 0)
            { 
                if(start % 2 == 1)
                    start_alone = true;            // can only be taken alone from 2nd level
                if(end % 2 != 1)
                    end_closes = true;          // can close an interval
            }
            to_return=0;
            
            if(start_alone)
                start++;
            if(!end_closes)
                end--;
            start >>= 1;
            end >>= 1;
        }
            return false;
    }
    
    
    // find hash of each IP and return if exists
    private int findIfExists(int level, int key)
    {
        if(this.bloomFiltersArray.get(level).bloomF.get(key))
            return 1;
        return 0;
    }
    
 /*   private int findIfExists(long start, long end, int level)    // this function reached only level 2 because of the constraint
    {
        int query_count = 0;
        int pos;
        int to_return=0;
        System.out.println("Search: "+level);
        for(long j = start;j<end;j++)
        {
            for(int i =0;i<k;i++)
            {
                pos =  computeHash(this.bloomFiltersArray.get(level).range, a[i], b[i], j);
                if(this.bloomFiltersArray.get(level).bloomF.get(pos))
                    to_return++;
                query_count++;
                if(query_count >= 64)
                    return 2;
            }
            if(to_return == k)
                return 1;
            query_count++;
            if(query_count >= 64)
                return 2;
        }
        return 0;
    }*/
    
    private int findIfExists(long start, long end, int level)    // this function is used to query continuously from start
    {                                                            // to each upper level
        int query_count = 0;
        int pos;
        int to_return=0;
        long j = start;
        for(query_count=3;query_count<64;query_count+=3)
        {
            for(int i =0;i<k;i++)                   // hash functions
            {
                pos =  computeHash(this.bloomFiltersArray.get(level).range, a[i], b[i], j);
                if(this.bloomFiltersArray.get(level).bloomF.get(pos))
                    to_return++;
            }
            query_count+=3;
            if(query_count >= 64)
                return 2;
            if(to_return == k)
                return 1;
            if((j /2) < (end /2 ))
            {
                j >>= 1;
                end >>= 1;
                level++;
            }
            else
                j++;
        }
        return 2;
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
    
    public void setHashCoeff()
    {
        Random r = new Random();
        r.setSeed(1000000);
        a = new int[k];
        b = new int[k];
        for(int i=0;i<k;i++)
        {   
            a[i] = r.nextInt((Integer.MAX_VALUE - 0));
            b[i] = r.nextInt((Integer.MAX_VALUE - 0));
        }
    }

    public int computeHash(long p, int a, int b, long x1)
    {
        long hash = a*x1+b;
        hash += hash >> 32;
        hash &= PRIME_NUM;
        hash = hash % p;
        if(hash > Integer.MAX_VALUE)
            hash = Integer.MAX_VALUE-a;
        return (int)hash;
    }

    public int getDomainSize() {
        return domainSize;
    }

    public void setDomainSize(int domainSize) {
        this.domainSize = domainSize;
    }

    public ArrayList<sketch> getBloomFiltersArray() {
        return bloomFiltersArray;
    }

    public void setBloomFiltersArray(ArrayList<sketch> bloomFiltersArray) {
        this.bloomFiltersArray = bloomFiltersArray;
    }
    
    public int getM() {
        return m;
    }

    public void setM(int m) {
        this.m = m;
    }
    
    public double getPr() {
        return pr;
    }

    public void setPr(double pr) {
        this.pr = pr;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int[] getA() {
        return a;
    }

    public void setA(int[] a) {
        this.a = a;
    }

    public int[] getB() {
        return b;
    }

    public void setB(int[] b) {
        this.b = b;
    }
    
    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public ArrayList<sketch> getDatastr() {
        return bloomFiltersArray;
    }
    
    @Override
    public String toString() {
        return "";
    }
    
    public int findK(int n, int m) {
          return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }   
    
    public int optimalNumOfBits(long n, double p) {
        return (int) ((n*Math.log(p)) / Math.log(0.6185));
    }
}
