import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class JumpingWindow {

    private int windowSizew;
    private double epsilon;
    public LinkedList<HashMap<Integer, Integer>> window;
    private int subWindowSize;
    private int lastelement;                 // keeps a counter for the whole W 
    private boolean leaving = false;

    public JumpingWindow(int windowSizeW, double epsilon) {
        this.windowSizew = windowSizeW;
        this.epsilon = epsilon;
        this.subWindowSize = (int) (2 * epsilon * windowSizew);
        window = new LinkedList<>();
    }

    public void insertEvent(int srcIP) {
        if (this.lastelement % subWindowSize == 0) // new subwindow
        {
            HashMap<Integer, Integer> hm = new HashMap<>();
            hm.put(srcIP, 1);
            window.offer(hm);
        } else {
            Integer k;
            window.getLast().put(srcIP, ((k = window.getLast().get(srcIP)) == null ? 1 : k + 1));     // else increment for this IP
        }

        // if windowSize is full then I keep the oldest subwindow until the newest one is complete.
        if (this.lastelement > windowSizew) {
            leaving = true;
            int s = 0;
            Iterator it = window.getLast().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry temp = (Map.Entry) it.next();
                s += (Integer) temp.getValue();
            }
            if (s == subWindowSize) {
                window.removeFirst();
                this.lastelement -= subWindowSize;
            }
        }
    }

    int getFreqEstimation(int srcIP, int queryWindowSizeW1) {
        int result = 0;
        if (queryWindowSizeW1 >= windowSizew) // || queryWindowSizeW1 >= this.lastelement)
        {
            return this.getFreqEstimation(srcIP);
        } else {
            int first = ((this.lastelement - queryWindowSizeW1) / subWindowSize);   // find first 
            //take half from the beginning
            Integer temp = window.get(first).get(srcIP);
            if (temp != null) {
                result += Math.ceil(temp/2);
            }
            for (int i = first + 1; i < window.size(); i++) {
                temp = window.get(i).get(srcIP);
                if (temp != null) {
                    result += temp;
                }
            }
        }
        return result;
    }

    int getFreqEstimation(int srcIP) {
        if (leaving) {
            return computeRunningSum(srcIP);
        } 
        else      // if W is full
        {
            int result = 0;
            // result = window.stream().map((m) -> m.get(srcIP)).filter((temp) -> (temp != null)).map((temp) -> temp).reduce(result, Integer::sum); 
            for (HashMap<Integer, Integer> h1 : window) {
                Integer temp = h1.get(srcIP);
                if (temp != null) {
                    result += temp;
                }
            }
            return result;
        }
    }

    public int computeRunningSum(int srcIP) {
        int sum = 0;
        //Add half of the subwindow leaving
        Integer k = this.window.get(0).get(srcIP);   // add the oldest subwindow that until the latest is complete
        if (k != null) {
            sum += Math.ceil(k / 2);
        }
        for (int i = 1; i < window.size(); i++) {
            Integer t = window.get(i).get(srcIP);
            if (t != null) {
                sum += t;
            }
        }
        return sum;
    }

    public boolean isLeaving() {
        return leaving;
    }

    public void setLeaving(boolean leaving) {
        this.leaving = leaving;
    }

    public int getLastelement() {
        return lastelement;
    }

    public void incrementLastelement() {
        this.lastelement++;
    }

    public int getSubWindowSize() {
        return subWindowSize;
    }

    public void setSubWindowSize(int subWindowSize) {
        this.subWindowSize = subWindowSize;
    }

    public int getWindowSizew() {
        return windowSizew;
    }

    public void setWindowSizew(int windowSizew) {
        this.windowSizew = windowSizew;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public int getSubWindow() {
        return subWindowSize;
    }

    public void setSubWindow(int subWindow) {
        this.subWindowSize = subWindow;
    }

    public int calc_hash() {
        return window.size();
    }

}
