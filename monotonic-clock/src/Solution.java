import org.jetbrains.annotations.NotNull;

/**
 * Monotonic clock task
 * Java version
 * @author Chebotareva Olesya (@phoenix-1202)
 */
public class Solution implements MonotonicClock {
    private final RegularInt c1 = new RegularInt(0);
    private final RegularInt c2 = new RegularInt(0);
    private final RegularInt c3 = new RegularInt(0);

    private final RegularInt t1 = new RegularInt(0);
    private final RegularInt t2 = new RegularInt(0);
    private final RegularInt t3 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        t1.setValue(time.getD1());
        t2.setValue(time.getD2());
        t3.setValue(time.getD3());
        c3.setValue(t3.getValue());
        c2.setValue(t2.getValue());
        c1.setValue(t1.getValue());
    }

    @NotNull
    @Override
    public Time read() {
        int resC1 = c1.getValue();
        int resC2 = c2.getValue();
        int resC3 = c3.getValue();
        int resT3 = t3.getValue();
        int resT2 = t2.getValue();
        int resT1 = t1.getValue();
        if (resC1 == resT1 && resC2 == resT2 && resC3 <= resT3)
            return new Time(resT1, resT2, resT3);
        if (resC1 >= resT1) {
            if (resC2 <= resT2)
                return new Time(resT1, resT2, 0);
            return new Time(resT1, resT2, resT3);
        }
        else
            return new Time(resT1, 0, 0);
    }
}
